#!/bin/bash


#uncomment statements to override variable values, comments show the default

#The port the TLB server would listen to
#TLB_SERVER_PORT=7019 

#Store is the directory under which the test information(running-time, results etc) are stored
#TLB_DATA_DIR=tlb_store #in current working directory


pid_file=.server.pid
server_out=server.out
server_err=server.err

function start_server {
    load_status
    check_if_already_running

    if [ $status = does-not-exist ]; then
        tlb_jar=`ls -t | grep '^tlb-server.*\.jar$' | head -1` 
        java -jar $tlb_jar 1>$server_out 2>$server_err &
        pid=$!
        disown

        echo $pid > $pid_file
        echo "Server started(and demonized), PID: $pid"
    fi 
}

function stop_server {
    load_status
    if [ $status = 'running' ]; then
        kill $pid
        remove_pid_file
    else
        echo "Doesn't look like tlb server is running"
        exit 1
    fi
}

function load_status {
    if [ -e $pid_file ]; then
        pid=`cat $pid_file`
        ps u -p $pid | grep tlb-server | grep -q java
        if [ $? -eq 0 ]; then
            status='running'
        else
            status='unknown'
        fi
    else
        status='does-not-exist'
    fi
}

function check_if_already_running {
    if [ $status != does-not-exist ]; then
        echo "PID file: $pid_file already exists, it seems a tlb server is already running off this directory."
        echo "The process id of this process, according to PID file, should be $pid"
        echo "Please stop this process(user 'server.sh stop' or call 'server.sh cleanup' to have this pid file removed)"
        exit 1
    fi
}

function remove_pid_file {
    if [ $status != 'does-not-exist' ]; then
        rm $pid_file
    else
        echo "No PID file($pid_file) found."
        exit 1
    fi
}

function display_status {
    load_status
    if [ $status = running ]; then
        echo "The server is running (PID: $pid)"
    elif [ $status = unknown ]; then
        echo "There should be a server running with PID: $pid(according to pid file content), but this script failed to find its status"
    elif [ $status = does-not-exist ]; then
        echo "The server is NOT running"
    else
        echo "Unknown status"
    fi
}


arg=${1:-unknown}

if [ $arg = start ]; then
    start_server
elif [ $arg = stop ]; then
    stop_server
elif [ $arg = cleanup ]; then
    load_status
    remove_pid_file
elif [ $arg = status ]; then
    display_status
else
    echo "Usage: <path>/server.sh <action>"
    echo "    Valid values for <action> are 'start', 'stop', 'status' and 'cleanup' (without quotes)"
    echo "        start:   starts the server(off current working dir)"
    echo "        stop:    stops the server(running off current working dir)"
    echo "        status:  status of the server process(running off current working dir)"
    echo "        cleanup: forgets about the existing server process, removes the pid file(advised only when the server process has died on its own or has been killed externally)"
fi
