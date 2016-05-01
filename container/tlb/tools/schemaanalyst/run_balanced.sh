#!/bin/bash

export TLB_JOB_NAME='schemaanalyst'
export TLB_BASE_URL='http://192.168.1.139:7019'
export TLB_TOTAL_PARTITIONS=3
export TLB_PARTITION_NUMBER=1
export TEST_TASK='ant test.balanced'
export ALL_PARTITIONS_RAN_VERIFICATION_TASK='ant assert.all.partitions.executed'

source ../recipe.sh
