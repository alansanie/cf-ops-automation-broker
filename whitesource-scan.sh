#!/bin/bash

productName=COAB
# targetFolderName is where the jar file/s will be created.
# This is usually target or bin
targetFolderName=target

cd ~/

#Extracted from  https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss_agent.sh
curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.jar
curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.config


for currentFolderName in $(ls -d1 */)
do
  projectName=${currentFolderName%?}
  #Finds the 1st jar file
  jarCount=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1 | wc -l`
  if [ $jarCount -gt 0 ]; then
    jarFile=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1`
    java -jar wss-unified-agent.jar -c wss-unified-agent.config -appPath ${jarFile} "${@:2}"
  fi
done
