@echo off

echo Administrative permissions required. Detecting permissions...

net session >nul 2>&1
if %errorLevel% == 0 (
  echo Success: Administrative permissions confirmed.
  sc stop ConvertigoServer
) else (
  echo Failure: Current permissions inadequate.
  pause >nul
)