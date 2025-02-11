#!/bin/sh

if [ "$1" = "convertigo" ]; then
    
    WEB_INF=$CATALINA_HOME/webapps/convertigo/WEB-INF
    
    ## function used to cipher passwords
    
    toHash() {
        echo "System.out.println(org.apache.commons.codec.digest.DigestUtils.sha512Hex(\"$1\"))" | jshell --class-path $CATALINA_HOME/webapps/convertigo/WEB-INF/lib/dependencies-*.jar -
    }
        
    ## if needed, force the admin and testplatform accounts
    
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
    
    if [ "$CONVERTIGO_TESTPLATFORM_USER" != "" ]; then
        if ! grep -q '^testplatform\.username=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.testplatform.username=$ChtONVERTIGO_TESTPLATFORM_USER $JAVA_OPTS"
        else
            echo 'Ignore $CONVERTIGO_TESTPLATFORM_USER because /workspace/configuration/engine.properties defines testplatform.username' 
        fi
        unset CONVERTIGO_TESTPLATFORM_USER
    fi
    
    if [ "$CONVERTIGO_TESTPLATFORM_PASSWORD" != "" ]; then
        if ! grep -q '^testplatform\.password=' /workspace/configuration/engine.properties 2>/dev/null; then
            export JAVA_OPTS="-Dconvertigo.engine.testplatform.password=$(toHash $CONVERTIGO_TESTPLATFORM_PASSWORD) $JAVA_OPTS"
        else
            echo 'Ignore $CONVERTIGO_TESTPLATFORM_PASSWORD because /workspace/configuration/engine.properties define testplatform.password' 
        fi
        unset CONVERTIGO_TESTPLATFORM_PASSWORD
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
    
    if [ -d /workspace/classes/ ]; then
        cp -r /workspace/classes/* $WEB_INF/classes/ 2>/dev/null
    fi
    
    ## check and adapt the Java Xmx for limited devices
    
    if [ "$JXMX" != "" ]; then
        export JAVA_OPTS="$JAVA_OPTS -Xms128m -Xmx${JXMX}m"
        echo "Use JXMX to set -Xmx$[JXMX}m"
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
        --add-opens java.base/java.net=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        --add-opens java.base/sun.security.util=ALL-UNNAMED \
        --add-opens java.base/sun.security.x509=ALL-UNNAMED \
        --add-opens java.desktop/sun.awt.image=ALL-UNNAMED \
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
    
    if [ $(id -u) = "0" ]; then
        chown -Lf convertigo:convertigo /workspace
        exec sudo -n -E -u convertigo $CATALINA_HOME/bin/catalina.sh run
    else
        exec $CATALINA_HOME/bin/catalina.sh run
    fi
fi

exec "$@"