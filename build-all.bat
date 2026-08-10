@echo off
setlocal
set "REPOSITORY_ROOT=%~dp0"
set "GRADLE_ARGS=%*"
if "%~1"=="" set "GRADLE_ARGS=buildAndCollect"

echo ==^> Running legacy BuildCraft build: %GRADLE_ARGS%
pushd "%REPOSITORY_ROOT%builds\legacy"
call gradlew.bat %GRADLE_ARGS%
set "STATUS=%ERRORLEVEL%"
popd
if not "%STATUS%"=="0" exit /b %STATUS%

echo ==^> Running modern BuildCraft build: %GRADLE_ARGS%
pushd "%REPOSITORY_ROOT%builds\modern"
call gradlew.bat %GRADLE_ARGS%
set "STATUS=%ERRORLEVEL%"
popd
exit /b %STATUS%
