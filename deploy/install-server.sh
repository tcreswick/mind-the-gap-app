#!/usr/bin/env bash
set -euo pipefail

APP_NAME="mind-the-gap-app"
APP_USER="springapp"
APP_GROUP="springapp"
REPO_URL="https://github.com/tcreswick/mind-the-gap-app.git"
BRANCH="main"
SRC_DIR=""
APP_BASE=""
RELEASES_DIR=""
CURRENT_LINK=""

usage() {
  cat <<'EOF'
Install deploy tooling and systemd configuration for this app.

Usage:
  install-server.sh --repo-url <git-url> [--branch main] [--app-name name] [--src-dir /srv/name]
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-url)
      REPO_URL="${2:-}"
      shift 2
      ;;
    --branch)
      BRANCH="${2:-}"
      shift 2
      ;;
    --app-name)
      APP_NAME="${2:-}"
      shift 2
      ;;
    --app-user)
      APP_USER="${2:-}"
      shift 2
      ;;
    --app-group)
      APP_GROUP="${2:-}"
      shift 2
      ;;
    --src-dir)
      SRC_DIR="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${SRC_DIR}" ]]; then
  SRC_DIR="/srv/${APP_NAME}"
fi

APP_BASE="/opt/${APP_NAME}"
RELEASES_DIR="${APP_BASE}/releases"
CURRENT_LINK="${APP_BASE}/current"
CONFIG_DIR="/etc/${APP_NAME}"
STATE_DIR="/var/lib/${APP_NAME}"
DEPLOY_SCRIPT_PATH="/usr/local/bin/deploy-${APP_NAME}.sh"
SERVICE_PATH="/etc/systemd/system/${APP_NAME}.service"
ENV_PATH="/etc/default/${APP_NAME}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="${SCRIPT_DIR}/templates"

if [[ ! -d "${TEMPLATES_DIR}" ]]; then
  echo "ERROR: templates directory not found at ${TEMPLATES_DIR}"
  exit 1
fi

if [[ "$(id -u)" -ne 0 ]]; then
  echo "ERROR: install-server.sh must run as root (use sudo)."
  exit 1
fi

echo "==> Ensuring runtime user exists"
if ! getent group "${APP_GROUP}" >/dev/null; then
  groupadd --system "${APP_GROUP}"
fi
if ! id "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --gid "${APP_GROUP}" --home /nonexistent --shell /usr/sbin/nologin "${APP_USER}"
fi

echo "==> Creating app directories"
mkdir -p "${SRC_DIR}" "${RELEASES_DIR}" "${CONFIG_DIR}" "${STATE_DIR}"
chown -R "${APP_USER}:${APP_GROUP}" "${SRC_DIR}" "${APP_BASE}" "${STATE_DIR}"

echo "==> Installing deploy script"
sed \
  -e "s|__APP_NAME__|${APP_NAME}|g" \
  -e "s|__APP_USER__|${APP_USER}|g" \
  -e "s|__APP_GROUP__|${APP_GROUP}|g" \
  -e "s|__REPO_URL__|${REPO_URL}|g" \
  -e "s|__BRANCH__|${BRANCH}|g" \
  -e "s|__SRC_DIR__|${SRC_DIR}|g" \
  -e "s|__APP_BASE__|${APP_BASE}|g" \
  -e "s|__RELEASES_DIR__|${RELEASES_DIR}|g" \
  -e "s|__CURRENT_LINK__|${CURRENT_LINK}|g" \
  -e "s|__STATE_DIR__|${STATE_DIR}|g" \
  "${TEMPLATES_DIR}/deploy.sh.template" > "${DEPLOY_SCRIPT_PATH}"
chmod 0755 "${DEPLOY_SCRIPT_PATH}"

echo "==> Installing systemd unit"
sed \
  -e "s|__APP_NAME__|${APP_NAME}|g" \
  -e "s|__APP_USER__|${APP_USER}|g" \
  -e "s|__APP_GROUP__|${APP_GROUP}|g" \
  "${TEMPLATES_DIR}/app.service.template" > "${SERVICE_PATH}"

echo "==> Ensuring env file exists"
if [[ ! -f "${ENV_PATH}" ]]; then
  sed -e "s|__APP_NAME__|${APP_NAME}|g" "${TEMPLATES_DIR}/default.env.template" > "${ENV_PATH}"
  chmod 0644 "${ENV_PATH}"
fi

echo "==> Reloading systemd"
systemctl daemon-reload
systemctl enable "${APP_NAME}.service"

echo "==> Install complete"
echo "Deploy command: sudo ${DEPLOY_SCRIPT_PATH}"
