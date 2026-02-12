#!/bin/sh
# see https://adoptium.net

set -eu

# Keep this single value up to date with the bundled Studio JDK release.
JDK_RELEASE=25.0.2+10
JDK_VERSION=${JDK_RELEASE%%+*}
JDK_BUILD=${JDK_RELEASE#*+}
JDK_MAJOR=${JDK_VERSION%%.*}
JDK_FILE_VERSION=${JDK_VERSION}_${JDK_BUILD}
JDK_BASE_URL=https://github.com/adoptium/temurin${JDK_MAJOR}-binaries/releases/download/jdk-${JDK_RELEASE}
JDK_FILE_PREFIX=OpenJDK${JDK_MAJOR}U-jdk

JDK_URL_WIN=${JDK_BASE_URL}/${JDK_FILE_PREFIX}_x64_windows_hotspot_${JDK_FILE_VERSION}.zip
JDK_URL_MAC=${JDK_BASE_URL}/${JDK_FILE_PREFIX}_x64_mac_hotspot_${JDK_FILE_VERSION}.tar.gz
JDK_URL_MAC_ARM=${JDK_BASE_URL}/${JDK_FILE_PREFIX}_aarch64_mac_hotspot_${JDK_FILE_VERSION}.tar.gz
JDK_URL_LINUX=${JDK_BASE_URL}/${JDK_FILE_PREFIX}_x64_linux_hotspot_${JDK_FILE_VERSION}.tar.gz

mkdir -p /tmp/c8o_jre
cd /tmp/c8o_jre

curl -sfSL -o jdk.zip "${JDK_URL_WIN}"
unzip -q jdk.zip
rm jdk.zip
mv jdk* win
rm -rf win/jmods win/lib/src.zip

curl -sfSL -o jdk.tar.gz "${JDK_URL_MAC}"
tar -xf jdk.tar.gz
mv jdk*/Contents/Home mac
rm -rf jdk* mac/lib/src.zip mac/man mac/jmods

curl -sfSL -o jdk.tar.gz "${JDK_URL_MAC_ARM}"
tar -xf jdk.tar.gz
mv jdk*/Contents/Home mac-arm
rm -rf jdk* mac-arm/lib/src.zip mac-arm/man mac-arm/jmods

curl -sfSL -o jdk.tar.gz "${JDK_URL_LINUX}"
tar -xf jdk.tar.gz
rm jdk.tar.gz
mv jdk* lin
rm -rf lin/lib/src.zip lin/man lin/jmods
