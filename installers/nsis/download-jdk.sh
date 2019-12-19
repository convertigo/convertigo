#!/bin/sh
# see https://adoptopenjdk.net
JDK_URL=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.5%2B10/OpenJDK11U-jdk_${JDK_ARCH}_windows_hotspot_11.0.5_10.zip
JDK_NAME=$( echo $JDK_URL | sed "s,.*/\(.*\)\\.zip,\\1," )${JDK_ARCH}

mkdir -p /tmp/${JDK_NAME} /tmp/c8o_jre
cd /tmp/${JDK_NAME}
curl -sfSL -o jdk.zip ${JDK_URL}
unzip -q jdk.zip
rm jdk.zip
mv jdk* /tmp/c8o_jre/${JDK_ARCH}
cd /tmp/c8o_jre/${JDK_ARCH}
rm -rf demo jmods lib/src.zip