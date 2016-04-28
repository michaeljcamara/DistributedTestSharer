@echo off

set pid_file=.server.pid

if "%1%"=="start" (
  call :start_server
) else if "%1%"=="stop" (
  call :stop_server
) else if "%1%"=="status" (
  call :status
) else if "%1" == "cleanup" (
  if not exist %pid_file% (
    echo "No PID file (%pid_file%) found"
    exit /B 1
  ) else (
    call :delete_pid_file
  )
) else (
  echo '%1%' is not a supported option. Options have to be one of 'start', 'stop' or 'status'
)
goto :EOF

:start_server
  echo Starting TLB server
  if exist %pid_file% (
    echo PID file: %pid_file% already exists. It seems a TLB server is already running off this directory.
    for /f "usebackq delims==" %%a in (`type %pid_file%`) do set pid_from_file=%%a
    echo The process ID of this process, according to the PID file is: %pid_from_file%
    echo Please stop this process using 'server.bat stop' or cleanup using 'server.bat cleanup' to have this PID file removed
    exit /B 1
  )
  START "Test Load Balancer Server in %cd%" /MIN server.cmd
  echo %PID% > %pid_file%
  call :populate_pid
  echo Server started. The PID is: %PID%
goto :EOF

:status
  call :is_server_running
  if "%server_running%"=="true" (
    echo TLB server is running with process ID: %PID%
  ) else (
    echo TLB server is not running
  )
goto :EOF

:stop_server
  call :is_server_running
  if "%server_running%"=="true" (
    call :kill_server
  ) else (
    echo Does not look like TLB server is running
  )
goto :EOF

:kill_server
  call :populate_pid
  TASKKILL /F /T /PID %PID%
  call :delete_pid_file
  echo Stopped TLB server
goto :EOF

:populate_pid
  set PID=
  FOR /F "tokens=2" %%I in ('TASKLIST /NH /FI "WINDOWTITLE eq Test Load Balancer Server in %cd% - server.cmd"' ) DO set PID=%%I
goto :EOF

:delete_pid_file
  del /Q /F %pid_file%
goto :EOF

:is_server_running
  call :populate_pid
  if %PID% neq "NO" set "running=true"
  if %PID% neq "No" set "running=true"
  if "%running%"=="true" (
    set "server_running=true"
  ) else  (
    set "server_running=false"
  )
goto :EOF