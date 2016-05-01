#!/bin/bash

if [[ $1 = no-verbosity ]]; then
    verbose=false
else 
    verbose=true
fi

dev_lib_dir=../../tlb/target
dist_lib_dir=../../server

if [ -e $dev_lib_dir ]; then
    export TLB_JAR=`ls $dev_lib_dir/tlb-server*.jar`
else
    export TLB_JAR=`ls $dist_lib_dir/tlb-server*.jar`
fi

export TLB_OUT_FILE=/tmp/tlb_balancer.out 
export TLB_ERR_FILE=/tmp/tlb_balancer.err
export TLB_BASE_URL='http://192.168.1.139:7019'
export TLB_JOB_VERSION=`date | sed -e 's# #-#g'`

    $TEST_TASK
