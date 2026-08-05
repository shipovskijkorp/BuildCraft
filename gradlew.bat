@echo off
setlocal EnableExtensions

set "GRADLE_VERSION=9.0.0"
set "APP_HOME=%~dp0"
set "BOOTSTRAP_DIR=%APP_HOME%.gradle-bootstrap"
set "GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "WINDOWS_POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "WINDOWS_CURL=%SystemRoot%\System32\curl.exe"
set "WINDOWS_TAR=%SystemRoot%\System32\tar.exe"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run

if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
if errorlevel 1 (
  echo Failed to create "%BOOTSTRAP_DIR%".
  exit /b 1
)

if not exist "%ZIP_FILE%" call :download
if errorlevel 1 exit /b %errorlevel%

call :extract
if errorlevel 1 exit /b %errorlevel%

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle %GRADLE_VERSION% was downloaded, but "%GRADLE_HOME%\bin\gradle.bat" was not created.
  echo Delete "%BOOTSTRAP_DIR%" and run gradlew.bat again.
  exit /b 1
)

goto run

:download
echo Downloading Gradle %GRADLE_VERSION%...
if exist "%WINDOWS_CURL%" (
  "%WINDOWS_CURL%" -fL --retry 3 --connect-timeout 30 -o "%ZIP_FILE%" "%URL%"
  exit /b %errorlevel%
)

where curl.exe >nul 2>nul
if not errorlevel 1 (
  curl.exe -fL --retry 3 --connect-timeout 30 -o "%ZIP_FILE%" "%URL%"
  exit /b %errorlevel%
)

if exist "%WINDOWS_POWERSHELL%" (
  "%WINDOWS_POWERSHELL%" -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%URL%' -OutFile '%ZIP_FILE%'"
  exit /b %errorlevel%
)

where pwsh.exe >nul 2>nul
if not errorlevel 1 (
  pwsh.exe -NoLogo -NoProfile -NonInteractive -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP_FILE%'"
  exit /b %errorlevel%
)

echo Could not download Gradle: curl.exe and PowerShell were not found.
echo Download %URL% manually and save it as "%ZIP_FILE%".
exit /b 1

:extract
echo Extracting Gradle %GRADLE_VERSION%...
if exist "%WINDOWS_TAR%" (
  "%WINDOWS_TAR%" -xf "%ZIP_FILE%" -C "%BOOTSTRAP_DIR%"
  exit /b %errorlevel%
)

where tar.exe >nul 2>nul
if not errorlevel 1 (
  tar.exe -xf "%ZIP_FILE%" -C "%BOOTSTRAP_DIR%"
  exit /b %errorlevel%
)

if exist "%WINDOWS_POWERSHELL%" (
  "%WINDOWS_POWERSHELL%" -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "Expand-Archive -Force -LiteralPath '%ZIP_FILE%' -DestinationPath '%BOOTSTRAP_DIR%'"
  exit /b %errorlevel%
)

where pwsh.exe >nul 2>nul
if not errorlevel 1 (
  pwsh.exe -NoLogo -NoProfile -NonInteractive -Command "Expand-Archive -Force -LiteralPath '%ZIP_FILE%' -DestinationPath '%BOOTSTRAP_DIR%'"
  exit /b %errorlevel%
)

echo Could not extract Gradle: tar.exe and PowerShell were not found.
echo Extract "%ZIP_FILE%" into "%BOOTSTRAP_DIR%" manually.
exit /b 1

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %errorlevel%
