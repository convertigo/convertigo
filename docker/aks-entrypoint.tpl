
    if [ "$TUNNEL_PORT" != "" ]; then
        if [ "$TUNNEL_PORT" = "28080" ]; then
            /usr/local/bin/chisel server --port 28080 --reverse --socks5 --proxy http://localhost:28081 2>&1 >/var/log/chisel &
            sed -i.bak2 -e 's/"28080"/"28081"/' $CATALINA_HOME/conf/server.xml
        else
            /usr/local/bin/chisel server --port $TUNNEL_PORT --reverse --socks5 2>&1 >/var/log/chisel &
        fi
    fi
    for i in $(set | grep "_SERVICE_\|_PORT" | cut -f1 -d=); do unset $i; done
    