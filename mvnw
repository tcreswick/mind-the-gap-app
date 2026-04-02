#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_VERSION="3.9.9"
MVN_DIR="${SCRIPT_DIR}/.mvn/apache-maven-${MVN_VERSION}"
MVN_BIN="${MVN_DIR}/bin/mvn"
MVN_ZIP="${SCRIPT_DIR}/.mvn/apache-maven-${MVN_VERSION}-bin.zip"
MVN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.zip"

mkdir -p "${SCRIPT_DIR}/.mvn"

if [ ! -x "${MVN_BIN}" ]; then
  echo "Bootstrapping Maven ${MVN_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${MVN_URL}" -o "${MVN_ZIP}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${MVN_URL}" -O "${MVN_ZIP}"
  else
    echo "Neither curl nor wget is available to download Maven." >&2
    exit 1
  fi
  unzip -qo "${MVN_ZIP}" -d "${SCRIPT_DIR}/.mvn"
fi

exec "${MVN_BIN}" "$@"
