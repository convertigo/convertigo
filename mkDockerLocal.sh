#!/bin/sh

dir=$( cd -P $(dirname $0); pwd)

if ! command -v docker >/dev/null 2>&1 || ! docker ps >/dev/null 2>&1 ; then
    echo "Need a shell environment with a working 'docker' command"
    exit 5
fi

case "$1" in
"build")   REBUILD=false;;
"rebuild") REBUILD=true;;
*)         echo "Missing arguments:\nbuild [i386|alpine]\nAlso rebuild base: rebuild [i386|alpine]"
           exit 1;;
esac

case "$2" in
"i386")   export TYPE=i386   WAR="linux32-";;
"alpine") export TYPE=alpine WAR="";;
"")       export TYPE=amd64  WAR="";;
*)        echo "Bad argument: |i386|alpine"
          exit 2;;
esac

export CONVERTIGO_VERSION_NUMBER=$(sed -n "s/def convertigoVersion = '\(.*\)'/\\1/p" $dir/build.gradle)
export CONVERTIGO_VERSION_TAG=$(sed -n "s/def convertigoTag = '\(.*\)'/\\1/p" $dir/build.gradle)
if [ "$CONVERTIGO_VERSION_TAG" = "" ] ; then
    export CONVERTIGO_VERSION=$CONVERTIGO_VERSION_NUMBER
else
    export CONVERTIGO_VERSION=$CONVERTIGO_VERSION_NUMBER-$CONVERTIGO_VERSION_TAG
fi

WAR_FILE=$dir/engine/build/libs/convertigo-${WAR}${CONVERTIGO_VERSION}.war

if [ ! -f $WAR_FILE ]; then
    echo "Missing the WAR (not built ?): $WAR_FILE"
    exit 3
fi

echo "Using the WAR: $WAR_FILE"

rm -rf $dir/workspace/docker
cp -r $dir/docker $dir/workspace

(
cd $dir/workspace/docker

export FROM=$(eval echo $(sed -n "s/FROM \(.*\)/\1/p" Dockerfile))

echo "Searching for the base: $FROM"

if [ "$REBUILD" = true ] || ! docker pull $FROM ; then
    echo "Building: $FROM"
    if [ $TYPE = "i386" ]; then
        sed -i "s,tomcat:,i386/tomcat:," Dockerfile_base
    fi
    if [ $TYPE = "alpine" ]; then
        docker build --rm=false -f alpine/Dockerfile_base -t $FROM alpine
    else
        docker build --rm=false -f Dockerfile_base -t $FROM .
    fi
fi

sed -i -e "s,RUN curl.*,COPY convertigo.war /tmp/convertigo.war," -e "s,    \\(&& mkdir\\),RUN chmod a+x /*.sh \\1," Dockerfile

ln -s $WAR_FILE convertigo.war
docker build --rm=false -t c8o:$TYPE --build-arg TYPE=$TYPE .
docker tag c8o:$TYPE c8o:latest
echo "All is done, the image is accessible from c8o:$TYPE or c8o:latest"
)