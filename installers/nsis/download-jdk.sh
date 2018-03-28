#!/bin/sh

JDK_URL=https://github.com/ojdkbuild/ojdkbuild/releases/download/1.8.0.161-1/java-1.8.0-openjdk-1.8.0.161-1.b14.ojdkbuild.windows.${JDK_ARCH}.zip
JDK_NAME=$( echo $JDK_URL | sed "s,.*/\(.*\)\\.zip,\\1," )

cd /tmp
curl -fSL -o ${JDK_NAME}.zip ${JDK_URL}
unzip -q ${JDK_NAME}.zip ${JDK_NAME}/jre/**
mkdir -p /tmp/c8o_jre
mv ${JDK_NAME}/jre /tmp/c8o_jre/${JDK_ARCH}
rm -rf ${JDK_NAME}*