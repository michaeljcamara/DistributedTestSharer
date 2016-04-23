@echo off

REM uncomment statements to override variable values, comments show the default

REM The port the TLB server would listen to
REM set TLB_PORT=7019 

REM Store is the directory under which the test information(running-time, results etc) are stored
REM set tlb_store=tlb_store #in current working directory

java -jar tlb-server*.jar