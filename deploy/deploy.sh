#!/usr/bin/env bash
# EcoVault 生产部署脚本：停止旧服务、备份旧版本、部署新版本、启动并执行健康检查。

set -euo pipefail

APP_NAME="ecovault"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_NATIVE="${BASE_DIR}/target/${APP_NAME}"
DEPLOY_DIR="${BASE_DIR}"
BACKUP_DIR="${DEPLOY_DIR}/backup"
LOG_DIR="${DEPLOY_DIR}/logs"
APP_NATIVE="${DEPLOY_DIR}/${APP_NAME}"
APP_JAR="${DEPLOY_DIR}/${APP_NAME}.jar"
APP_LOG="${LOG_DIR}/${APP_NAME}.log"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8100/actuator/health}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
HEALTH_RETRY="${HEALTH_RETRY:-60}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

ensure_dirs() {
  mkdir -p "${DEPLOY_DIR}" "${BACKUP_DIR}" "${LOG_DIR}" "$(dirname "${TARGET_NATIVE}")"
}

require_native_artifact() {
  if [[ ! -f "${TARGET_NATIVE}" ]]; then
    log "未找到 Native 可执行文件：${TARGET_NATIVE}。请先执行 GraalVM Native Image 构建。"
    exit 1
  fi
}

is_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

# 通过 ps 查询当前 APP_NAME 对应的运行进程信息（不依赖 PID 文件）。
# 兼容识别 Native 可执行文件与历史 JAR 进程，避免迁移到 Native 时漏停旧进程。
# 若存在多个匹配进程，取最近启动的一个（即列表末尾），并输出警告。
find_app_process() {
  local processes count
  processes="$(ps -eo pid,args | awk -v jar="${APP_JAR}" -v native="${APP_NATIVE}" '
    NR == 1 { next }
    function matches_native_path(command, candidate, suffix) {
      if (candidate == "") {
        return 0
      }
      if (index(command, candidate) != 1) {
        return 0
      }
      suffix = substr(command, length(candidate) + 1, 1)
      return (suffix == "" || suffix ~ /[[:space:]]/)
    }
    {
      pid = $1
      args = ""
      for (i = 2; i <= NF; i++) {
        args = args (i == 2 ? "" : " ") $i
      }
    }
    index(args, jar) > 0 { print pid "\t" jar; next }
    matches_native_path(args, native) {
      print pid "\t" native
    }
  ' || true)"

  if [[ -z "${processes}" ]]; then
    return 0
  fi

  count="$(printf '%s\n' "${processes}" | awk 'NF { count++ } END { print count + 0 }')"
  if (( count > 1 )); then
    printf '[%s] 警告：检测到 %d 个 %s 运行进程，将操作最近启动的进程。\n' \
      "$(date '+%Y-%m-%d %H:%M:%S')" "${count}" "${APP_NAME}" >&2
  fi

  printf '%s\n' "${processes}" | awk -F '\t' 'NF { pid = $1; artifact = $2 } END { if (pid != "") print pid "\t" artifact }'
}

stop_service() {
  local process_info="${1:-}" pid artifact
  if [[ -z "${process_info}" ]]; then
    process_info="$(find_app_process)"
  fi
  IFS=$'\t' read -r pid artifact <<< "${process_info}"

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
  local preferred_source="${1:-}" source_file backup_ext
  if [[ -n "${preferred_source}" ]] && [[ -f "${preferred_source}" ]]; then
    source_file="${preferred_source}"
    if [[ "${preferred_source}" == "${APP_JAR}" ]]; then
      backup_ext=".jar"
    else
      backup_ext=""
    fi
  elif [[ -f "${APP_NATIVE}" ]]; then
    source_file="${APP_NATIVE}"
    backup_ext=""
  elif [[ -f "${APP_JAR}" ]]; then
    source_file="${APP_JAR}"
    backup_ext=".jar"
  else
    log "未发现可备份的旧版本产物，跳过备份。"
    return 0
  fi

  local timestamp backup_file
  timestamp="$(date '+%Y%m%d%H%M%S')"
  backup_file="${BACKUP_DIR}/${APP_NAME}-${timestamp}${backup_ext}"
  cp "${source_file}" "${backup_file}"
  log "旧版本已备份到 ${backup_file}。"
}

deploy_new_version() {
  cp "${TARGET_NATIVE}" "${APP_NATIVE}"
  chmod +x "${APP_NATIVE}"
  log "新版本 Native 可执行文件已部署到 ${APP_NATIVE}。"
}

start_service() {
  log "正在以 Native 模式启动 ${APP_NAME}，配置环境为 ${SPRING_PROFILE}。"

  BUILD_ID=dontKillMe nohup "${APP_NATIVE}" \
    --spring.profiles.active="${SPRING_PROFILE}" >> "${APP_LOG}" 2>&1 &

  sleep 2

  local process_info pid artifact
  process_info="$(find_app_process)"
  IFS=$'\t' read -r pid artifact <<< "${process_info}"

  if [[ -z "${pid}" ]]; then
    log "未找到 ${APP_NAME} 的运行进程，启动失败，请查看日志：${APP_LOG}。"
    exit 1
  fi

  log "服务启动命令已执行，PID=${pid}，日志=${APP_LOG}。"
}

health_check() {
  log "开始健康检查：${HEALTH_URL}。"

  local process_info pid artifact
  process_info="$(find_app_process)"
  IFS=$'\t' read -r pid artifact <<< "${process_info}"

  if [[ -z "${pid}" ]]; then
    log "未找到 ${APP_NAME} 的运行进程，无法执行健康检查。请查看日志：${APP_LOG}。"
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
  local process_info previous_pid previous_artifact
  ensure_dirs
  require_native_artifact
  process_info="$(find_app_process)"
  IFS=$'\t' read -r previous_pid previous_artifact <<< "${process_info}"
  stop_service "${process_info}"
  backup_old_version "${previous_artifact}"
  deploy_new_version
  start_service
  health_check
  log "${APP_NAME} 部署完成。"
}

main "$@"
