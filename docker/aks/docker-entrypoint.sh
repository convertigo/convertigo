#!/bin/sh

if [ "$1" = "convertigo" ]; then
    
    WEB_INF=$CATALINA_HOME/webapps/convertigo/WEB-INF
    
    ## function used to cipher passwords
    
    toHash() {
        echo "System.out.println(\"$1\".hashCode())" | jshell -
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
    
    if [ "$JXMX" = "" ]; then
        JXMX=2048
    fi
    
    java -Xmx${JXMX}m -version >/dev/null
    while [ $? != 0 ] && [ $JXMX -gt 200 ]; do
       JXMX=`expr $JXMX / 2 + $JXMX / 4`
       java -Xmx${JXMX}m -version >/dev/null
    done
    
    
    ## default common JAVA_OPTS, can be extended with "docker run -e JAVA_OPTS=-custom" 
    
    export JAVA_OPTS="\
        -Xms128m \
        -Xmx${JXMX}m \
        -Xdebug \
        -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n \
        -Dconvertigo.cems.user_workspace_path=/workspace \
        $JAVA_OPTS"
    
    unset JXMX
    
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
    
    if [ "$TUNNEL_PORT" != "" ]; then
        if [ "$TUNNEL_PORT" = "28080" ]; then
            /usr/local/bin/chisel server --port 28080 --reverse --socks5 --proxy http://localhost:28081 2>&1 >/var/log/chisel &
            sed -i.bak2 -e 's/"28080"/"28081"/' $CATALINA_HOME/conf/server.xml
        else
            /usr/local/bin/chisel server --port $TUNNEL_PORT --reverse --socks5 2>&1 >/var/log/chisel &
        fi
    fi
    for i in $(set | grep "_SERVICE_\|_PORT" | cut -f1 -d=); do unset $i; done
    
    exec gosu convertigo $CATALINA_HOME/bin/catalina.sh run
fi

exec "$@"