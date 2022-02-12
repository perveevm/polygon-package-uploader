#!/bin/bash

if [ ! -f ~/.polygon-package-uploader/uploader_path ]
then
  echo "Please, use install.sh first"
  exit 0
fi

if [ ! -f ~/.polygon-package-uploader/uploader_version ]
then
  echo "Please, use install.sh first"
  exit 0
fi

UPLOADER_PATH=$(cat ~/.polygon-package-uploader/uploader_path)
UPLOADER_VERSION=$(ls ${UPLOADER_PATH}/target | grep -e polygon-package-uploader | sort | tail -1)

if [ "$1" = "init" ]
then
  while true; do
    read -p "Do you want to use advanced functionality (creating problem by name, committing and building packages?) [y/n] " ADVANCED_CHOISE
    case ${ADVANCED_CHOISE} in
      [Yy]* ) java -jar ${UPLOADER_PATH}/target/${UPLOADER_VERSION} init -k -s -l -p; echo "ADVANCED" > ~/.polygon-package-uploader/uploader_mode; break;;
      [Nn]* ) java -jar ${UPLOADER_PATH}/target/${UPLOADER_VERSION} init -k -s; echo "API" > ~/.polygon-package-uploader/uploader_mode; break;;
      * ) echo "Please, choose y or n";;
    esac
  done

  echo "Your credentials are saved!"
elif [ "$1" = "upload" ]
then
  if [ ! -f ~/.polygon-package-uploader/uploader_mode ]
  then
    echo "Please, use polygon-uploader init to save credentials first"
    exit 0
  fi

  java -jar ${UPLOADER_PATH}/target/${UPLOADER_VERSION} $@
else
  echo "Unsupported command ${1}. Only init and upload commands are supported"
fi
