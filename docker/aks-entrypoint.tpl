
    if [ "$TUNNEL_PORT" != "" ]; then
        if [ "$TUNNEL_KEY" != "" ]; then
            TUNNEL_KEY="--key $TUNNEL_KEY"
        fi
        if [ "$TUNNEL_AUTH" != "" ]; then
            TUNNEL_AUTH="--auth $TUNNEL_AUTH"
        fi
        if [ "$TUNNEL_PORT" = "28080" ]; then
            /usr/local/bin/chisel server --port 28080 $TUNNEL_KEY $TUNNEL_AUTH --reverse --socks5 --proxy http://localhost:28081 2>&1 >/var/log/chisel &
            (TMPSED=`sed -e "s/"28080"/"28081"/" $CATALINA_HOME/conf/server.xml` && \
                echo "$TMPSED" > $CATALINA_HOME/conf/server.xml)
        else
            /usr/local/bin/chisel server --port $TUNNEL_PORT $TUNNEL_KEY $TUNNEL_AUTH --reverse --socks5 2>&1 >/var/log/chisel &
        fi
    fi
    for i in $(set | grep "_SERVICE_\|_PORT" | cut -f1 -d=); do unset $i; done
    
    ## AOT cache of the JVM (JDK 25), aks image only: the classes and method profiles recorded when
    ## the image was built (/aot-train.sh, which starts the server with AOT_CACHE=record) are used by
    ## default when the image has a cache and nothing prevents it (JDWP, AOT or CDS options in
    ## JAVA_OPTS); AOT_CACHE=off starts without it. A cache that does not match the JDK, the JVM
    ## options or the classpath is ignored by the JVM, which starts without it.

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
    