#!/usr/bin/env bash
set -euo pipefail

APP_NAME="mind-the-gap"
REPO_URL="https://github.com/tcreswick/mind-the-gap-app.git"
BRANCH="main"
SRC_DIR=""

usage() {
  cat <<'EOF'
Bootstrap a Debian server for this Spring Boot app.

Usage:
  bootstrap-server.sh --repo-url <git-url> [--branch main] [--app-name name] [--src-dir /srv/name]

Example:
  curl -fsSL https://raw.githubusercontent.com/tcreswick/mind-the-gap-app/main/deploy/bootstrap-server.sh \
    | bash -s -- --repo-url https://github.com/tcreswick/mind-the-gap-app.git --branch main
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

if ! command -v git >/dev/null 2>&1; then
  echo "ERROR: git is required but not installed."
  echo "Install with: sudo apt-get update && sudo apt-get install -y git"
  exit 1
fi

echo "==> Preparing source directory at ${SRC_DIR}"
sudo mkdir -p "$(dirname "${SRC_DIR}")"

if [[ ! -d "${SRC_DIR}/.git" ]]; then
  echo "==> Cloning ${REPO_URL} (${BRANCH})"
  sudo git clone --branch "${BRANCH}" "${REPO_URL}" "${SRC_DIR}"
else
  echo "==> Updating existing repository checkout"
  sudo git -C "${SRC_DIR}" fetch origin
  sudo git -C "${SRC_DIR}" checkout "${BRANCH}"
  sudo git -C "${SRC_DIR}" reset --hard "origin/${BRANCH}"
fi

echo "==> Installing service and deploy tooling"
sudo bash "${SRC_DIR}/deploy/install-server.sh" \
  --repo-url "${REPO_URL}" \
  --branch "${BRANCH}" \
  --app-name "${APP_NAME}" \
  --src-dir "${SRC_DIR}"

echo "==> Running first deployment"
sudo "/usr/local/bin/deploy-${APP_NAME}.sh"

echo "==> Bootstrap complete"
echo "Service status:"
sudo systemctl --no-pager status "${APP_NAME}.service" || true
