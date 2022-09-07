#!/bin/sh
# see https://adoptopenjdk.net

mkdir -p /tmp/c8o_jre
cd /tmp/c8o_jre

JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.4.1_1.zip
curl -sfSL -o jdk.zip ${JDK_URL}
unzip -q jdk.zip
rm jdk.zip
mv jdk* win
rm -rf win/jmods win/lib/src.zip

JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4.1%2B1/OpenJDK17U-jdk_x64_mac_hotspot_17.0.4.1_1.tar.gz
curl -sfSL -o jdk.tar.gz ${JDK_URL}
tar -xf jdk.tar.gz
mv jdk*/Contents/Home mac
rm -rf jdk* mac/lib/src.zip mac/man mac/jmods

JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.4.1_1.tar.gz
curl -sfSL -o jdk.tar.gz ${JDK_URL}
tar -xf jdk.tar.gz
rm jdk.tar.gz
mv jdk* lin
rm -rf lin/lib/src.zip lin/man lin/jmods