#!/bin/bash

export TLB_BASE_URL='http://141.195.23.248:7019/'
export TLB_BALANCER_PORT=12345
export TLB_JOB_NAME='ant-ivy'
export TLB_TOTAL_PARTITIONS=2
export TEST_TASK='ant test.balanced'
export ALL_PARTITIONS_RAN_VERIFICATION_TASK='ant assert.all.partitions.executed'

source ../recipe.sh
