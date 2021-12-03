@echo off
set JAVA_HOME=%~dp0%jdk
set CATALINA_HOME=%~dp0%tomcat

echo Administrative permissions required. Detecting permissions...

net session >nul 2>&1
if %errorLevel% == 0 (
  echo Success: Administrative permissions confirmed.
  %CATALINA_HOME%\bin\service.bat uninstall ConvertigoServer --rename
) else (
  echo Failure: Current permissions inadequate.
)
pause >nul