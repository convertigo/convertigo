#!/bin/sh

if [ "$1" = "convertigo" ]; then
    
    WEB_INF=$CATALINA_HOME/webapps/convertigo/WEB-INF
    
    ## function used to cipher passwords
    
    toHash() {
        echo "System.out.println(org.apache.commons.codec.digest.DigestUtils.sha512Hex(\"$1\"))" | jshell --class-path $CATALINA_HOME/webapps/convertigo/WEB-INF/lib/dependencies-*.jar -
    }
        
    ## if needed, force the admin and testplatform accounts
    
    if [ "$CONVERTIGO_ADMIN_USER" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.admin.username=$CONVERTIGO_ADMIN_USER $JAVA_OPTS"
        unset CONVERTIGO_ADMIN_USER
    fi
    
    if [ "$CONVERTIGO_ADMIN_PASSWORD" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.admin.password=$(toHash $CONVERTIGO_ADMIN_PASSWORD) $JAVA_OPTS"
        unset CONVERTIGO_ADMIN_PASSWORD
    fi
    
    if [ "$CONVERTIGO_TESTPLATFORM_USER" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.testplatform.username=$CONVERTIGO_TESTPLATFORM_USER $JAVA_OPTS"
        unset CONVERTIGO_TESTPLATFORM_USER
    fi
    
    if [ "$CONVERTIGO_TESTPLATFORM_PASSWORD" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.testplatform.password=$(toHash $CONVERTIGO_TESTPLATFORM_PASSWORD) $JAVA_OPTS"
        unset CONVERTIGO_TESTPLATFORM_PASSWORD
    fi
    
    
    ## add the linked couchdb container as the fullsync couchdb
    
    if [ "$(getent hosts couchdb)" != "" ]; then
        export JAVA_OPTS="-Dconvertigo.engine.fullsync.couch.url=http://couchdb:5984 $JAVA_OPTS"
    fi
    
    
    ## add custom jar or class to the convertigo server
    
    cp -r /workspace/lib/* $WEB_INF/lib/ 2>/dev/null
    cp -r /workspace/classes/* $WEB_INF/classes/ 2>/dev/null
    
    
    ## check and adapt the Java Xmx for limited devices
    
    if [ "$JXMX" != "" ]; then
        export JAVA_OPTS="$JAVA_OPTS -Xms128m -Xmx${JXMX}m"
        unset JXMX
    else
        export JAVA_OPTS="$JAVA_OPTS -XX:MaxRAMPercentage=80"
    fi
    
    ## default common JAVA_OPTS, can be extended with "docker run -e JAVA_OPTS=-custom" 
    
    export JAVA_OPTS="$JAVA_OPTS \
        -XX:+UseG1GC \
        -XX:+UseStringDeduplication \
        -XX:MinHeapFreeRatio=10 \
        -XX:MaxHeapFreeRatio=15 \
        -Xdebug \
        -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n \
        -Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true \
        -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true \
        -Dconvertigo.cems.user_workspace_path=/workspace"
    
    ## the web-connector version can use an existing DISPLAY or declare one
    ## the mbaas version need to be headless and remove the DISPLAY variable
    
    if [ -d $WEB_INF/xvnc ]; then
        export DISPLAY=${DISPLAY:-:0}
    else
        unset DISPLAY
    fi
    
    if [ "$COOKIE_PATH" != "" ]; then
        sed -i.bak -e "s,sessionCookiePath=\"[^\"]*\",sessionCookiePath=\"$COOKIE_PATH\"," $CATALINA_HOME/conf/context.xml
        unset COOKIE_PATH
    fi
    
    if [ "$COOKIE_SECURE" = "true" ]; then
        sed -i.bak -e "s,<secure>false</secure>,<secure>true</secure>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml
    else
    	sed -i.bak -e "s,<secure>true</secure>,<secure>false</secure>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml
    fi
    unset COOKIE_SECURE
    
    if [ "$COOKIE_SAMESITE" != "" ]; then
        sed -i.bak -e "s,sameSiteCookies=\"[^\"]*\",sameSiteCookies=\"$COOKIE_SAMESITE\"," $CATALINA_HOME/conf/context.xml
        unset COOKIE_SAMESITE
    fi
    
    if [ "$SESSION_TIMEOUT" != "" ]; then
        sed -i.bak -e "s,<.*session-timeout.*,<session-timeout>$SESSION_TIMEOUT</session-timeout>," $CATALINA_HOME/webapps/convertigo/WEB-INF/web.xml
    fi
    
    if [ "$DISABLE_SUDO" = "true" ]; then
        rm /etc/sudoers.d/convertigo
    fi
    
    
    
    exec gosu convertigo $CATALINA_HOME/bin/catalina.sh run
fi

exec "$@"