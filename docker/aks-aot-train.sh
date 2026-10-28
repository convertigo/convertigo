#!/bin/sh
# Source file for docker/aks/aot-train.sh, regenerated with ./gradlew generateDockerfile.

## train the AOT cache of the aks image when it is built (AOT_CACHE_IMAGE), used by default by its
## containers: start the server once on an empty workspace with a small sample project, call it,
## stop it (the JVM writes the cache when it exits), then remove everything else this run wrote.
## The AOT_CACHE handling of the entrypoint comes from docker/aks-entrypoint.tpl.

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
AOT_CACHE=record ENABLE_JDWP_DEBUG=false JXMX=1024 /docker-entrypoint.sh convertigo &
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
