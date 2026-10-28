#!/bin/sh
# Source file for docker/*/docker-entrypoint.sh, regenerated with ./gradlew generateDockerfile.

## train the AOT cache of the image when it is built (AOT_CACHE_IMAGE), used by default by its
## containers: start the server once on an empty workspace with a small sample project, call it,
## stop it (the JVM writes the cache when it exits), then remove everything else this run wrote.

if [ "$1" = "aot-train" ]; then
    touch /tmp/aot-train.start
    AOT_DIR=$CATALINA_HOME/aot
    mkdir -p $AOT_DIR /workspace/projects/AotTraining
    ## bean declarations end with ': ' in the project YAML format
    sed 's/\]:$/]: /' > /workspace/projects/AotTraining/c8oProject.yaml <<'EOF'
↑convertigo: 8.4.0.m006
↓AotTraining [core.Project]:
  ↓void [connectors.SqlConnector]:
    isDefault: true
    ↓Default [transactions.SqlTransaction]:
      isDefault: true
  ↓Hello [sequences.GenericSequence]:
    ↓Compute [steps.SimpleStep-1700002470001]:
      expression: |
        var words = [];
        for (var i = 0; i < 50; i++) {
          words.push("w" + (i * 7 % 13));
        }
        words.sort();
        var summary = words.join(",");
    ↓summary [steps.ElementStep-1700002470002]:
      expression: summary
      nodeName: summary
EOF
    chown -R convertigo:convertigo $AOT_DIR /workspace
    AOT_CACHE=record ENABLE_JDWP_DEBUG=false JXMX=1024 "$0" convertigo &
    AOT_PID=$!
    AOT_URL=http://127.0.0.1:28080/convertigo
    i=0
    until curl -fs -o /dev/null --max-time 30 $AOT_URL/admin/services/engine.CheckAuthentication; do
        i=$((i + 1))
        if [ $i -gt 600 ] || ! kill -0 $AOT_PID 2>/dev/null; then
            echo "AOT training: the server did not start"
            break
        fi
        sleep 1
    done
    ## a single session (cookie jar): the server has no license key here
    AOT_CURL="curl -s -b /tmp/aot-train.cookies -c /tmp/aot-train.cookies"
    AOT_CALLS=0
    for i in $(seq 1 20); do
        $AOT_CURL "$AOT_URL/projects/AotTraining/.json?__sequence=Hello" | grep -q summary && AOT_CALLS=$((AOT_CALLS + 1))
        $AOT_CURL "$AOT_URL/projects/AotTraining/.xml?__sequence=Hello" | grep -q summary && AOT_CALLS=$((AOT_CALLS + 1))
    done
    echo "AOT training: $AOT_CALLS/40 calls of the sample project"
    $AOT_CURL -o /dev/null $AOT_URL/admin/services/engine.Supervision
    $AOT_CURL -o /dev/null "$AOT_URL/projects/AotTrainingMissing/.json?__sequence=Missing"
    $AOT_CURL -o /dev/null $AOT_URL/
    kill -TERM $AOT_PID 2>/dev/null
    wait $AOT_PID
    find /workspace $CATALINA_HOME/logs $CATALINA_HOME/temp $CATALINA_HOME/work /home/convertigo /tmp \
        -mindepth 1 -maxdepth 1 -newer /tmp/aot-train.start -exec rm -rf {} +
    rm -rf /tmp/aot-train.start $CATALINA_HOME/conf/Catalina
    if [ -s $AOT_DIR/convertigo.aot ]; then
        chown -R root:root $AOT_DIR
        chmod -R a+rX,go-w $AOT_DIR
        echo "AOT training: $(du -h $AOT_DIR/convertigo.aot | cut -f1) cache in $AOT_DIR"
    else
        rm -rf $AOT_DIR
        echo "AOT training: no cache created, the containers will start without AOT cache"
    fi
    exit 0
fi

if [ "$1" = "convertigo" ]; then
    
    WEB_INF=$CATALINA_HOME/webapps/convertigo/WEB-INF
    
    ## function used to cipher passwords
    
    toHash() {
        echo "System.out.println(org.apache.commons.codec.digest.DigestUtils.sha512Hex(\"$1\"))" | jshell --class-path $CATALINA_HOME/webapps/convertigo/WEB-INF/lib/dependencies-*.jar -
    }
        
    ## if needed, force admin account and dashboard anonymous mode
    
    if [ "$CONVERTIGO_ADMIN_USER" != "" ]; then
        if ! grep -q '^admin\.username=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.admin.username=$CONVERTIGO_ADMIN_USER $JAVA_OPTS"
        else
            echo 'Ignore $CONVERTIGO_ADMIN_USER because /workspace/configuration/engine.properties defines admin.username' 
        fi
        unset CONVERTIGO_ADMIN_USER
    fi
    
    if [ "$CONVERTIGO_ADMIN_PASSWORD" != "" ]; then
        if ! grep -q '^admin\.password=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.admin.password=$(toHash $CONVERTIGO_ADMIN_PASSWORD) $JAVA_OPTS"
        else
            echo 'Ignore $CONVERTIGO_ADMIN_PASSWORD because /workspace/configuration/engine.properties defines admin.password' 
        fi
        unset CONVERTIGO_ADMIN_PASSWORD
    fi
    
    if [ "$CONVERTIGO_ANONYMOUS_DASHBOARD" != "" ]; then
        if ! grep -q '^anonymous\.dashboard=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.anonymous.dashboard=$CONVERTIGO_ANONYMOUS_DASHBOARD $JAVA_OPTS"
        else
            echo 'Ignore $CONVERTIGO_ANONYMOUS_DASHBOARD because /workspace/configuration/engine.properties defines anonymous.dashboard' 
        fi
        unset CONVERTIGO_ANONYMOUS_DASHBOARD
    fi
    
    if [ "$PUBLIC_DOMAINS" != "" ]; then
        if ! grep -q '^cors\.policy=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.cors.policy=$PUBLIC_DOMAINS $JAVA_OPTS"
            echo "Configure cors.policy from PUBLIC_DOMAINS"
        else
            echo 'Ignore $PUBLIC_DOMAINS because /workspace/configuration/engine.properties defines cors.policy'
        fi
        unset PUBLIC_DOMAINS
    fi

    ## enable log to stdout or file
    
    if [ "$LOG_STDOUT" = "true" ]; then
    	export JAVA_OPTS="-Dconvertigo.engine.log.stdout.enable=true $JAVA_OPTS"
        unset LOG_STDOUT
    fi
    
    if [ "$LOG_FILE" = "false" ]; then
    	export JAVA_OPTS="-Dconvertigo.engine.log.file.enable=false $JAVA_OPTS"
        unset LOG_FILE
    fi
    
    ## enable JVM debug with JDWP
    if [ "$ENABLE_JDWP_DEBUG" = "true" ]; then
    	export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n $JAVA_OPTS"
    	unset ENABLE_JDWP_DEBUG
    fi
    
    ## add the linked couchdb container as the fullsync couchdb
    
    if [ "$(getent hosts couchdb)" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.fullsync.couch.url=http://couchdb:5984 $JAVA_OPTS"
    fi
    
    
    ## add custom jar or class to the convertigo server
    
    if [ -d /workspace/lib/ ]; then
        cp -r /workspace/lib/* $WEB_INF/lib/ 2>/dev/null
    fi

    ## native libraries dropped in /workspace/lib (copied above) must be found by the JVM

    export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}$WEB_INF/lib"

    if [ -d /workspace/classes/ ]; then
        cp -r /workspace/classes/* $WEB_INF/classes/ 2>/dev/null
    fi

    ## check and adapt the Java Xmx for limited devices
    
    if [ "$JXMX" != "" ]; then
        export JAVA_OPTS="$JAVA_OPTS -Xms128m -Xmx${JXMX}m"
        echo "Use JXMX to set -Xmx${JXMX}m"
        unset JXMX
    else
        export JAVA_OPTS="$JAVA_OPTS -XX:MaxRAMPercentage=80"
        echo "No JXMX, set -XX:MaxRAMPercentage=80"
    fi
    
    ## default common JAVA_OPTS, can be extended with "docker run -e JAVA_OPTS=-custom" 
    
    export JAVA_OPTS="$JAVA_OPTS \
        --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
        --add-opens=java.base/java.lang=ALL-UNNAMED \
        --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
        --add-opens=java.base/java.io=ALL-UNNAMED \
        --add-opens=java.base/java.net=ALL-UNNAMED \
        --add-opens=java.base/java.util=ALL-UNNAMED \
        --add-opens=java.base/sun.security.util=ALL-UNNAMED \
        --add-opens=java.base/sun.security.x509=ALL-UNNAMED \
        --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED \
        --enable-native-access=ALL-UNNAMED \
        -XX:+UseG1GC \
        -XX:+UseStringDeduplication \
        -Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true \
        -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true \
        -Dconvertigo.cems.user_workspace_path=/workspace"
    
    if [ "$COOKIE_PATH" != "" ]; then
        (TMPSED=`sed -e "s,sessionCookiePath=\"[^\"]*\",sessionCookiePath=\"$COOKIE_PATH\"," $CATALINA_HOME/conf/context.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/conf/context.xml)
        echo "Configure sessionCookiePath to $COOKIE_PATH"
        unset COOKIE_PATH
    fi
    
    if [ "$COOKIE_SECURE" = "true" ]; then
        (TMPSED=`sed -e "s,<secure>false</secure>,<secure>true</secure>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml)
        echo "Configure Cookie secure to 'true'"
    else
    	(TMPSED=`sed -e "s,<secure>true</secure>,<secure>false</secure>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml)
    	echo "Configure Cookie secure to 'false'"
    fi
    unset COOKIE_SECURE
    
    if [ "$COOKIE_SAMESITE" != "" ]; then
        (TMPSED=`sed -e "s,sameSiteCookies=\"[^\"]*\",sameSiteCookies=\"$COOKIE_SAMESITE\"," $CATALINA_HOME/conf/context.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/conf/context.xml)
        echo "Configure sameSiteCookies to $COOKIE_SAMESITE"
        unset COOKIE_SAMESITE
    fi
    
    if [ "$SESSION_TIMEOUT" != "" ]; then
        (TMPSED=`sed -e "s,<.*session-timeout.*,<session-timeout>$SESSION_TIMEOUT</session-timeout>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml)
        echo "Configure session-timeout to $SESSION_TIMEOUT"
    fi
    
    if [ $(id -u) = "0" ] && [ "$DISABLE_SUDO" = "true" ]; then
        rm /etc/sudoers.d/convertigo
        echo "Disable 'sudo'"
    fi
    
    if [ -d "/ssl/" ]; then
        rm -f /certs/*
        cp /ssl/* /certs/ 2>/dev/null
        echo "Copy SSL files from /ssl"
    fi
    
    if [ ! -f "/certs/key.pem" ] && [ "$SSL_SELFSIGNED" != "" ]; then
        echo "Generate a self-signed certificate for $SSL_SELFSIGNED"
        openssl req -x509 -newkey rsa:4096 -keyout /certs/key.pem -out /certs/cert.pem -sha256 -days 365 -nodes -subj "/CN=$SSL_SELFSIGNED"
        if [ -d "/ssl/" ] && [ ! -f "/ssl/key.pem" ] && [ ! -f "/ssl/cert.pem" ] ; then
            cp /certs/key.pem /ssl/
            cp /certs/cert.pem /ssl/
            echo "Copy the generated self-signed certificate to /ssl"
        fi
    fi
    unset SSL_SELFSIGNED
    
    if [ ! -f "/certs/key.pem" ] && [ "$SSL_KEY_B64" != "" ]; then
        echo "$SSL_KEY_B64" | base64 -d > /certs/key.pem
        echo "Configure SSL private key from SSL_KEY_B64"
    fi
    unset SSL_KEY_B64
    
    if [ ! -f "/certs/cert.pem" ] && [ "$SSL_CERT_B64" != "" ]; then
        echo "$SSL_CERT_B64" | base64 -d > /certs/cert.pem
        echo "Configure SSL certificate from SSL_CERT_B64"
    fi
    unset SSL_CERT_B64
    
    if [ ! -f "/certs/chain.pem" ] && [ "$SSL_CHAIN_B64" != "" ]; then
        echo "$SSL_CHAIN_B64" | base64 -d > /certs/chain.pem
        echo "Configure SSL chain from SSL_CHAIN_B64"
    fi
    unset SSL_CHAIN_B64
    
    if [ -f "/certs/cert.pem" ] && [ ! -f "/certs/chain.pem" ] && [ ! -f "/certs/full.pem" ]; then
        cp /certs/cert.pem /certs/full.pem
    fi
    
    if [ -f "/certs/full.pem" ]; then
        echo "Split SSL certificate and chain files"
        grep -B 1000 -m 1 -F -e "-----END CERTIFICATE-----" /certs/full.pem > /certs/cert.pem
        tail -n +2 /certs/full.pem | grep -A 1000 -m 1 -F -e "-----BEGIN CERTIFICATE-----" > /certs/chain.pem
    fi
    
    if [ -f "/certs/key.pem" ] && [ -f "/certs/cert.pem" ] && [ -f "/certs/chain.pem" ]; then
        echo "Enable SSL configuration for Tomcat"
        chmod a+r /certs/*
        (TMPSED=`sed -e 's,--SSL<,--SSL--><,' -e 's,>SSL--,><!--SSL--,' $CATALINA_HOME/conf/server.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/conf/server.xml)
    else
        echo "Disable SSL configuration for Tomcat"
        (TMPSED=`sed -e 's,--SSL--><,--SSL<,' -e 's,><!--SSL--,>SSL--,' $CATALINA_HOME/conf/server.xml` && \
            echo "$TMPSED" > $CATALINA_HOME/conf/server.xml)
    fi
    
    %ON_LAUNCH%
    
    ## AOT cache of the JVM (JDK 25): the classes and method profiles recorded when the image was
    ## built are used by default when the image has a cache and nothing prevents it (JDWP, AOT or CDS
    ## options in JAVA_OPTS); AOT_CACHE=off starts without it. A cache that does not match the JDK,
    ## the JVM options or the classpath is ignored by the JVM, which starts without it.

    if [ -z "$AOT_CACHE" ] && [ -f $CATALINA_HOME/aot/convertigo.aot ]; then
        AOT_CACHE=image
        AOT_DEFAULT=true
    fi
    case "$AOT_CACHE:$JAVA_OPTS" in
    :*|off:*|false:*)
        ;;
    *jdwp*|*-XX:AOT*|*-XX:SharedArchiveFile*|*-Xshare*)
        if [ "$AOT_DEFAULT" != "true" ]; then
            echo "Ignore AOT_CACHE=$AOT_CACHE: not compatible with JDWP or the AOT/CDS options of JAVA_OPTS"
        fi
        ;;
    *)
        ## adapters are machine code that depends on the CPU features: keep them out of the cache. Any
        ## -Dcom.sun.management.* option (JMX remote agent) makes the JVM add jdk.management.agent to the
        ## modules, and a cache trained with other modules is refused: always add it with the cache
        AOT_OPTS="-XX:+UnlockDiagnosticVMOptions -XX:-AOTAdapterCaching --add-modules=jdk.management.agent"
        case "$AOT_CACHE" in
        image)
            if [ -f $CATALINA_HOME/aot/convertigo.aot ]; then
                export JAVA_OPTS="$JAVA_OPTS $AOT_OPTS -XX:AOTCache=$CATALINA_HOME/aot/convertigo.aot"
                echo "Use the AOT cache of the image"
            else
                echo "Ignore AOT_CACHE=image: this image has no AOT cache"
            fi
            ;;
        record)
            export JAVA_OPTS="$JAVA_OPTS $AOT_OPTS -Xlog:aot=error -XX:AOTCacheOutput=$CATALINA_HOME/aot/convertigo.aot"
            ;;
        *)
            echo "Ignore AOT_CACHE=$AOT_CACHE: use off, or image (the default)"
            ;;
        esac
        ;;
    esac
    unset AOT_CACHE AOT_DEFAULT

    ## custom certificate authorities: delegate to the Eclipse Temurin entrypoint shipped in the
    ## base image (opt-in with USE_SYSTEM_CA_CERTS, certificates mounted in /certificates/*.crt)
    
    CACERT_ENTRYPOINT=""
    if [ -n "$USE_SYSTEM_CA_CERTS" ]; then
        if [ -x /__cacert_entrypoint.sh ]; then
            CACERT_ENTRYPOINT=/__cacert_entrypoint.sh
        else
            echo "USE_SYSTEM_CA_CERTS is set but the base image does not provide /__cacert_entrypoint.sh"
            exit 1
        fi
    fi
    
    if [ $(id -u) = "0" ]; then
        chown -Lf convertigo:convertigo /workspace
        export HOME=/home/convertigo
        exec setpriv --reuid=convertigo --regid=convertigo --init-groups $CACERT_ENTRYPOINT $CATALINA_HOME/bin/catalina.sh run
    else
        exec $CACERT_ENTRYPOINT $CATALINA_HOME/bin/catalina.sh run
    fi
fi

exec "$@"
