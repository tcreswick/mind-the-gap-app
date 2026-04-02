@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MVN_VERSION=3.9.9"
set "MVN_DIR=%SCRIPT_DIR%.mvn\apache-maven-%MVN_VERSION%"
set "MVN_BIN=%MVN_DIR%\bin\mvn.cmd"
set "MVN_ZIP=%SCRIPT_DIR%.mvn\apache-maven-%MVN_VERSION%-bin.zip"
set "MVN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip"

if not exist "%SCRIPT_DIR%.mvn" mkdir "%SCRIPT_DIR%.mvn"

if not exist "%MVN_BIN%" (
  echo Bootstrapping Maven %MVN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%MVN_URL%' -OutFile '%MVN_ZIP%'" || goto :download_failed
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MVN_ZIP%' -DestinationPath '%SCRIPT_DIR%.mvn' -Force" || goto :extract_failed
)

call "%MVN_BIN%" %*
exit /b %errorlevel%

:download_failed
echo Failed to download Maven from %MVN_URL%
exit /b 1

:extract_failed
echo Failed to extract Maven archive %MVN_ZIP%
exit /b 1
