#!/usr/bin/env bash
# EcoVault 生产部署脚本：停止旧服务、备份旧版本、部署新版本、启动并执行健康检查。

set -euo pipefail

APP_NAME="ecovault"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_JAR="${BASE_DIR}/target/${APP_NAME}.jar"
# 根据自己情况修改部署目录，确保 Jenkins 有权限写入。
DEPLOY_DIR="/usr/local/runtime/ecovault"
BACKUP_DIR="${DEPLOY_DIR}/backup"
LOG_DIR="${DEPLOY_DIR}/logs"
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"
APP_JAR="${DEPLOY_DIR}/${APP_NAME}.jar"
APP_LOG="${LOG_DIR}/${APP_NAME}.log"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8100/actuator/health}"
DEFAULT_JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS:-${DEFAULT_JAVA_OPTS}}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
HEALTH_RETRY="${HEALTH_RETRY:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

ensure_dirs() {
  mkdir -p "${DEPLOY_DIR}" "${BACKUP_DIR}" "${LOG_DIR}"
}

is_running() {
  local pid="$1"
  [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

stop_service() {
  if [[ ! -f "${PID_FILE}" ]]; then
    log "未发现 PID 文件，跳过停止步骤。"
    return 0
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  if ! is_running "${pid}"; then
    log "PID ${pid} 未运行，清理过期 PID 文件。"
    rm -f "${PID_FILE}"
    return 0
  fi

  log "正在停止 ${APP_NAME}，PID=${pid}。"
  kill "${pid}"

  for _ in $(seq 1 30); do
    if ! is_running "${pid}"; then
      rm -f "${PID_FILE}"
      log "服务已正常停止。"
      return 0
    fi
    sleep 1
  done

  log "服务未在限定时间内停止，执行强制终止。"
  kill -9 "${pid}"
  rm -f "${PID_FILE}"
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
  nohup java ${JAVA_OPTS} -jar "${APP_JAR}" --spring.profiles.active="${SPRING_PROFILE}" >> "${APP_LOG}" 2>&1 &
  local pid=$!
  echo "${pid}" > "${PID_FILE}"
  log "服务启动命令已执行，PID=${pid}，日志=${APP_LOG}。"
}

health_check() {
  log "开始健康检查：${HEALTH_URL}。"
  for _ in $(seq 1 "${HEALTH_RETRY}"); do
    if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
      log "健康检查通过。"
      return 0
    fi
    sleep "${HEALTH_INTERVAL}"
  done

  log "健康检查失败，请查看日志：${APP_LOG}。"
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if is_running "${pid}"; then
      log "部署失败，停止新启动的服务，PID=${pid}。"
      kill "${pid}" || true
    fi
  fi
  exit 1
}

main() {
  ensure_dirs
  stop_service
  backup_old_version
  deploy_new_version
  start_service
  health_check
  log "${APP_NAME} 部署完成。"
}

main "$@"
