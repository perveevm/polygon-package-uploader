#!/bin/bash

UPLOADER_PATH=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)
UPLOADER_VERSION=$(ls ${UPLOADER_PATH}/target | grep -e polygon-package-uploader | sort | tail -1)

mvn package -f ${UPLOADER_PATH}/pom.xml
sudo ln -s ${UPLOADER_PATH}/upload.sh /usr/local/bin/polygon-uploader

mkdir ~/.polygon-package-uploader
echo ${UPLOADER_PATH} > ~/.polygon-package-uploader/uploader_path
echo ${UPLOADER_VERSION} > ~/.polygon-package-uploader/uploader_version
