#!/bin/bash

if [[ $# != 5 ]]; then
  echo "Usage: upload.sh <access_code> <source_file_path> <destination_file_name> <summary> <labels>"
  exit 1
fi

TLB_UPLOAD_PASSWORD=$1 TLB_UPLOAD_FILE_PATH=$2 TLB_UPLOAD_TARGET_FILE_NAME=$3 TLB_UPLOAD_SUMMARY="$4" TLB_UPLOAD_LABELS="$5" ant -Doffline=true upload-files >> /tmp/upload_log 2>&1

tmp_file=/tmp/$3

rm $tmp_file
curl "http://${TLB_PROJECT_NAME}.googlecode.com/files/$3" > $tmp_file

actual_md5=`md5sum ${tmp_file} | awk '{ print $1 }'`
expected_md5=`md5sum $2 | awk '{print $1 }'`

if [[ $actual_md5 != $expected_md5 ]]; then
  echo "Looks like the file upload is wrong. Expected MD5: $expected_md5, but got: $actual_md5"
  exit 1
fi

echo "Uploaded file successfully"