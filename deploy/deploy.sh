#!/usr/bin/env bash
# EcoVault 生产部署脚本：停止旧服务、备份旧版本、部署新版本、启动并执行健康检查。

set -euo pipefail

APP_NAME="ecovault"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_JAR="${BASE_DIR}/target/${APP_NAME}.jar"
DEPLOY_DIR="${BASE_DIR}"
BACKUP_DIR="${DEPLOY_DIR}/backup"
LOG_DIR="${DEPLOY_DIR}/logs"
APP_JAR="${DEPLOY_DIR}/${APP_NAME}.jar"
APP_LOG="${LOG_DIR}/${APP_NAME}.log"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8100/actuator/health}"
# 默认堆内存限制，生产环境可通过 JAVA_OPTS 环境变量覆盖
DEFAULT_JAVA_OPTS="-Xms128m -Xmx512m --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS:-${DEFAULT_JAVA_OPTS}}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
HEALTH_RETRY="${HEALTH_RETRY:-60}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

ensure_dirs() {
  mkdir -p "${DEPLOY_DIR}" "${BACKUP_DIR}" "${LOG_DIR}" "$(dirname "${TARGET_JAR}")"
}

is_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

# 通过 ps -ef 查询 APP_JAR 对应的 Java 进程 PID（不依赖 PID 文件）。
# 使用固定字符串匹配完整 JAR 路径，避免正则误匹配其他 Java 进程。
# 若存在多个匹配进程，取最近启动的一个（即列表末尾），并输出警告。
find_app_pid() {
  local pids count
  pids="$(ps -ef | grep -F -- "${APP_JAR}" | grep -v grep | awk '{print $2}' || true)"

  if [[ -z "${pids}" ]]; then
    return 0
  fi

  count="$(printf '%s\n' "${pids}" | awk 'NF { count++ } END { print count + 0 }')"
  if (( count > 1 )); then
    printf '[%s] 警告：检测到 %d 个 %s Java 进程，将操作最近启动的进程。\n' \
      "$(date '+%Y-%m-%d %H:%M:%S')" "${count}" "${APP_NAME}" >&2
  fi

  printf '%s\n' "${pids}" | awk 'NF { pid = $1 } END { if (pid != "") print pid }'
}

stop_service() {
  local pid
  pid="$(find_app_pid)"

  if [[ -z "${pid}" ]]; then
    log "未发现正在运行的 ${APP_NAME} 进程，跳过停止步骤。"
    return 0
  fi

  log "正在停止 ${APP_NAME}，PID=${pid}。"
  kill "${pid}"

  for _ in $(seq 1 30); do
    if ! is_running "${pid}"; then
      log "服务已正常停止。"
      return 0
    fi
    sleep 1
  done

  log "服务未在限定时间内停止，执行强制终止。"
  kill -9 "${pid}" || true
}

backup_old_version() {
  if [[ ! -f "${APP_JAR}" ]]; then
    log "未发现旧版本 Jar，跳过备份。"
    return 0
  fi

  local timestamp backup_file
  timestamp="$(date '+%Y%m%d%H%M%S')"
  backup_file="${BACKUP_DIR}/${APP_NAME}-${timestamp}.jar"
  cp "${APP_JAR}" "${backup_file}"
  log "旧版本已备份到 ${backup_file}。"
}

deploy_new_version() {
  if [[ ! -f "${TARGET_JAR}" ]]; then
    log "未找到新版本 Jar：${TARGET_JAR}。请先执行 mvn clean package。"
    exit 1
  fi

  cp "${TARGET_JAR}" "${APP_JAR}"
  log "新版本已部署到 ${APP_JAR}。"
}

start_service() {
  log "正在启动 ${APP_NAME}，配置环境为 ${SPRING_PROFILE}。"

  BUILD_ID=dontKillMe nohup java ${JAVA_OPTS} -jar "${APP_JAR}" \
    --spring.profiles.active="${SPRING_PROFILE}" >> "${APP_LOG}" 2>&1 &

  sleep 2

  local pid
  pid="$(find_app_pid)"

  if [[ -z "${pid}" ]]; then
    log "未找到 ${APP_NAME} 的 Java 进程，启动失败，请查看日志：${APP_LOG}。"
    exit 1
  fi

  log "服务启动命令已执行，PID=${pid}，日志=${APP_LOG}。"
}

health_check() {
  log "开始健康检查：${HEALTH_URL}。"

  local pid
  pid="$(find_app_pid)"

  if [[ -z "${pid}" ]]; then
    log "未找到 ${APP_NAME} 的 Java 进程，无法执行健康检查。请查看日志：${APP_LOG}。"
    exit 1
  fi

  for _ in $(seq 1 "${HEALTH_RETRY}"); do
    if ! is_running "${pid}"; then
      log "检测到服务进程已退出，PID=${pid}。请查看日志：${APP_LOG}。"
      exit 1
    fi

    if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
      log "健康检查通过。"
      return 0
    fi

    sleep "${HEALTH_INTERVAL}"
  done

  log "健康检查失败，请查看日志：${APP_LOG}。"
  if is_running "${pid}"; then
    log "部署失败，停止新启动的服务，PID=${pid}。"
    kill "${pid}" || true
  fi
  exit 1
}

main() {
  ensure_dirs
  stop_service
  # backup_old_version
  deploy_new_version
  start_service
  health_check
  log "${APP_NAME} 部署完成。"
}

main "$@"
