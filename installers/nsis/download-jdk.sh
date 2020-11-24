#!/bin/sh
# see https://adoptopenjdk.net
JDK_URL=https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1%2B9/OpenJDK15U-jdk_x64_windows_hotspot_15.0.1_9.zip
JDK_NAME=$( echo $JDK_URL | sed "s,.*/\(.*\)\\.zip,\\1," )x64

mkdir -p /tmp/${JDK_NAME} /tmp/c8o_jre
cd /tmp/${JDK_NAME}
curl -sfSL -o jdk.zip ${JDK_URL}
unzip -q jdk.zip
rm jdk.zip
mv jdk* /tmp/c8o_jre/x64
cd /tmp/c8o_jre/x64
rm -rf demo jmods lib/src.zip