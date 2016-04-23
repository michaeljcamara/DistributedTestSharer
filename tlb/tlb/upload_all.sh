#!/bin/bash
release=$1
export TLB_USER_NAME=$2
password=$3
export TLB_PROJECT_NAME='tlb'

function sub_space() {
    echo $*
}

function upload() {
    file_name=`sub_space $1`
    summary=`sub_space $2`
    labels=`sub_space $3`
    desc=`sub_space $4`
    file=`sub_space target/dist/$file_name`
    
    ./upload.sh "$password" "$file" "$file_name" "$summary" "$labels"
    
    ret=$?
    if [[ $ret != 0 ]]; then
        echo "upload failed!!!"
        exit $ret
    fi

    echo "Put Desc:"
    echo '-----------------'
    echo $desc
    echo '-----------------'
}

upload "tlb-complete-$release.tar.gz" "Complete $release release archive" "Featured, Type-Archive, OpSys-All, $release, Complete" "This archive version $release of tlb-server, tlb-alien, tlb-java, setup-examples and tlb-source."
upload "tlb-complete-$release.tar.gz.asc" "Signature: Complete $release release archive" "Featured, OpSys-All, $release, Complete, Sign" "Sign"

upload "tlb-server-$release.tar.gz" "Server $release release archive" "Featured, Type-Archive, OpSys-All, $release, Server" "Contains server jar(standalone, all dependencies bundled), and process management utility scripts"
upload "tlb-server-$release.tar.gz.asc" "Signature: Server $release release archive" "Featured, OpSys-All, $release, Server, Sign" "Sign"

upload "tlb-java-$release.tar.gz" "Java support $release release archive" "Featured, Type-Archive, OpSys-All, $release, Java" "Contains java support jar and its dependencies."
upload "tlb-java-$release.tar.gz.asc" "Signature: Java support $release release archive" "Featured, OpSys-All, $release, Java, Sign" "Sign"

upload "setup-examples-$release.tar.gz" "Setup examples for $release release archive" "Featured, Type-Archive, OpSys-All, $release, Examples" "Contains multiple tiny example projects written in different languages and using different frameworks TLB supports."
upload "setup-examples-$release.tar.gz.asc" "Signature: Server $release release archive" "Featured, OpSys-All, $release, Examples, Sign" "Sign"

upload "README-$release" "README for $release release" "Featured, Type-Docs, OpSys-All, $release, Readme" "This is also bundled in all release archive"
upload "README-$release.asc" "Signature: README for $release release" "Featured, OpSys-All, $release, Readme, Sign" "Sign"
