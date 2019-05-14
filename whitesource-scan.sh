#!/bin/bash

set -x
pwd

productName=COAB
# targetFolderName is where the jar file/s will be created.
# This is usually target or bin
targetFolderName=target

cd ~/cf-ops-automation-broker

#Extracted from  https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss_agent.sh
if [ ! -f wss-unified-agent.jar ]; then
    curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.jar
fi
if [ ! -f wss-unified-agent.config ]; then
    curl -LJO https://github.com/whitesource/unified-agent-distribution/raw/master/standAlone/wss-unified-agent.config
fi

# https://whitesource.atlassian.net/wiki/spaces/WD/pages/651919363/EUA+Support+for+Multi-Module+Analysis
#First discover submodules and save it into a properties file
java -jar wss-unified-agent.jar -d $(pwd) -analyzeMultiModule whitesource-multi-module.properties

echo "whitesource-multi-module.properties content:"
cat whitesource-multi-module.properties

curl -LJ https://s3.amazonaws.com/file-system-agent/xModuleAnalyzer/xModuleAnalyzer-19.2.2.jar -o xModuleAnalyzer.jar

#Then invoke analysis on all modules
java -jar xModuleAnalyzer.jar -xModulePath whitesource-multi-module.properties -fsaJarPath wss-unified-agent.jar -c whitesource_config.properties -statusDisplay dynamic 

echo "Whitesource-Logs:"
LOG_FILES=$(find ./Whitesource-Logs/ -type f)
for f in $LOG_FILES; do echo "content of :$f"; cat $f; done


for currentFolderName in $(ls -d1 cf-ops-automation*/)
do
  projectName=${currentFolderName%?}
  #Finds the 1st jar file
  jarCount=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1 | wc -l`
  if [ $jarCount -gt 0 ]; then
    jarFile=`find ${currentFolderName}/${targetFolderName} -type f -name "*.jar" | head -1`
    java -jar wss-unified-agent.jar -appPath ${jarFile} "${@:1}"
  fi
done

