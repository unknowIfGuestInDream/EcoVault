#!/usr/bin/env bash
# EcoVault 生产部署脚本：停止旧服务、备份旧版本、部署新版本、启动并执行健康检查。

set -euo pipefail

APP_NAME="ecovault"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_JAR="${BASE_DIR}/target/${APP_NAME}.jar"
TARGET_NATIVE="${BASE_DIR}/target/${APP_NAME}"
DEPLOY_DIR="${BASE_DIR}"
BACKUP_DIR="${DEPLOY_DIR}/backup"
LOG_DIR="${DEPLOY_DIR}/logs"
APP_JAR="${DEPLOY_DIR}/${APP_NAME}.jar"
APP_NATIVE="${DEPLOY_DIR}/${APP_NAME}"
APP_LOG="${LOG_DIR}/${APP_NAME}.log"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8100/actuator/health}"
# auto: 优先部署 Native，可回退到 Jar；native/jar: 强制指定部署类型
ECOVAULT_ARTIFACT_TYPE="${ECOVAULT_ARTIFACT_TYPE:-auto}"
# 默认堆内存限制，生产环境可通过 JAVA_OPTS 环境变量覆盖
DEFAULT_JAVA_OPTS="-Xms128m -Xmx512m --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS:-${DEFAULT_JAVA_OPTS}}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
HEALTH_RETRY="${HEALTH_RETRY:-60}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"
DEPLOY_MODE=""
TARGET_ARTIFACT=""
APP_ARTIFACT=""
LAST_RUNNING_MODE=""

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

ensure_dirs() {
  mkdir -p "${DEPLOY_DIR}" "${BACKUP_DIR}" "${LOG_DIR}" "$(dirname "${TARGET_JAR}")"
}

require_target_artifact() {
  local artifact_path="$1"
  local artifact_mode="$2"
  if [[ ! -f "${artifact_path}" ]]; then
    log "未找到 ${artifact_mode} 模式所需产物：${artifact_path}。请先执行对应构建。"
    exit 1
  fi
}

resolve_artifact_mode() {
  case "${ECOVAULT_ARTIFACT_TYPE}" in
    auto)
      if [[ -f "${TARGET_NATIVE}" ]]; then
        DEPLOY_MODE="native"
        TARGET_ARTIFACT="${TARGET_NATIVE}"
        APP_ARTIFACT="${APP_NATIVE}"
      elif [[ -f "${TARGET_JAR}" ]]; then
        DEPLOY_MODE="jar"
        TARGET_ARTIFACT="${TARGET_JAR}"
        APP_ARTIFACT="${APP_JAR}"
      else
        log "未找到可部署产物：${TARGET_NATIVE} 或 ${TARGET_JAR}。请先执行对应构建。"
        exit 1
      fi
      ;;
    native)
      DEPLOY_MODE="native"
      TARGET_ARTIFACT="${TARGET_NATIVE}"
      APP_ARTIFACT="${APP_NATIVE}"
      require_target_artifact "${TARGET_ARTIFACT}" "${DEPLOY_MODE}"
      ;;
    jar)
      DEPLOY_MODE="jar"
      TARGET_ARTIFACT="${TARGET_JAR}"
      APP_ARTIFACT="${APP_JAR}"
      require_target_artifact "${TARGET_ARTIFACT}" "${DEPLOY_MODE}"
      ;;
    *)
      log "不支持的部署类型：${ECOVAULT_ARTIFACT_TYPE}。可选值为 auto、native、jar。"
      exit 1
      ;;
  esac
}

is_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && [[ "${pid}" =~ ^[0-9]+$ ]] && kill -0 "${pid}" 2>/dev/null
}

# 通过 ps 查询当前 APP_NAME 对应的运行进程 PID（不依赖 PID 文件）。
# 同时识别 Native 可执行文件与完整 JAR 路径，避免误匹配其他进程。
# 若存在多个匹配进程，取最近启动的一个（即列表末尾），并输出警告。
find_app_process() {
  local processes count
  processes="$(ps -eo pid=,args= | awk -v jar="${APP_JAR}" -v native="${APP_NATIVE}" '
    {
      pid = $1
      args = ""
      for (i = 2; i <= NF; i++) {
        args = args (i == 2 ? "" : " ") $i
      }
    }
    index(args, jar) > 0 { print pid "\tjar"; next }
    index(args, native) == 1 {
      suffix = substr(args, length(native) + 1, 1)
      if (suffix == "" || suffix ~ /[[:space:]]/) {
        print pid "\tnative"
      }
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

  printf '%s\n' "${processes}" | awk 'NF { pid = $1; mode = $2 } END { if (pid != "") print pid "\t" mode }'
}

stop_service() {
  local process_info pid mode
  process_info="$(find_app_process)"
  pid="$(printf '%s' "${process_info}" | awk 'NF { print $1 }')"
  mode="$(printf '%s' "${process_info}" | awk 'NF { print $2 }')"
  LAST_RUNNING_MODE="${mode}"

  if [[ -z "${pid}" ]]; then
    log "未发现正在运行的 ${APP_NAME} 进程，跳过停止步骤。"
    return 0
  fi

  log "正在停止 ${APP_NAME}（模式：${mode}），PID=${pid}。"
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
  local source_file backup_ext backup_mode
  backup_mode="${LAST_RUNNING_MODE:-${DEPLOY_MODE}}"
  if [[ "${backup_mode}" == "native" ]] && [[ -f "${APP_NATIVE}" ]]; then
    source_file="${APP_NATIVE}"
    backup_ext=""
  elif [[ "${backup_mode}" == "jar" ]] && [[ -f "${APP_JAR}" ]]; then
    source_file="${APP_JAR}"
    backup_ext=".jar"
  elif [[ -f "${APP_ARTIFACT}" ]]; then
    source_file="${APP_ARTIFACT}"
    if [[ "${DEPLOY_MODE}" == "jar" ]]; then
      backup_ext=".jar"
    else
      backup_ext=""
    fi
  else
    log "未发现当前部署模式对应的旧版本产物，跳过备份。"
    return 0
  fi

  local timestamp backup_file
  timestamp="$(date '+%Y%m%d%H%M%S')"
  backup_file="${BACKUP_DIR}/${APP_NAME}-${timestamp}${backup_ext}"
  cp "${source_file}" "${backup_file}"
  log "旧版本已备份到 ${backup_file}。"
}

deploy_new_version() {
  if [[ ! -f "${TARGET_ARTIFACT}" ]]; then
    log "未找到新版本产物：${TARGET_ARTIFACT}。请先执行对应构建。"
    exit 1
  fi

  cp "${TARGET_ARTIFACT}" "${APP_ARTIFACT}"
  if [[ "${DEPLOY_MODE}" == "native" ]]; then
    chmod +x "${APP_ARTIFACT}"
  fi
  log "新版本已部署到 ${APP_ARTIFACT}（模式：${DEPLOY_MODE}）。"
}

start_service() {
  log "正在启动 ${APP_NAME}（模式：${DEPLOY_MODE}），配置环境为 ${SPRING_PROFILE}。"

  if [[ "${DEPLOY_MODE}" == "native" ]]; then
    BUILD_ID=dontKillMe nohup "${APP_NATIVE}" \
      --spring.profiles.active="${SPRING_PROFILE}" >> "${APP_LOG}" 2>&1 &
  else
    BUILD_ID=dontKillMe nohup java ${JAVA_OPTS} -jar "${APP_JAR}" \
      --spring.profiles.active="${SPRING_PROFILE}" >> "${APP_LOG}" 2>&1 &
  fi

  sleep 2

  local process_info pid mode
  process_info="$(find_app_process)"
  pid="$(printf '%s' "${process_info}" | awk 'NF { print $1 }')"
  mode="$(printf '%s' "${process_info}" | awk 'NF { print $2 }')"

  if [[ -z "${pid}" ]]; then
    log "未找到 ${APP_NAME} 的运行进程，启动失败，请查看日志：${APP_LOG}。"
    exit 1
  fi

  log "服务启动命令已执行，PID=${pid}，模式=${mode}，日志=${APP_LOG}。"
}

health_check() {
  log "开始健康检查：${HEALTH_URL}。"

  local process_info pid
  process_info="$(find_app_process)"
  pid="$(printf '%s' "${process_info}" | awk 'NF { print $1 }')"

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
  ensure_dirs
  resolve_artifact_mode
  stop_service
  backup_old_version
  deploy_new_version
  start_service
  health_check
  log "${APP_NAME} 部署完成。"
}

main "$@"
