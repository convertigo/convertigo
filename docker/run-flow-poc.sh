#!/bin/sh

set -eu

ROOT=$(cd -P "$(dirname "$0")/.." && pwd)
IMAGE=${C8O_FLOW_POC_IMAGE:-convertigo/convertigo-ci:develop}
NAME=${C8O_FLOW_POC_NAME:-c8o-flow-poc}
PORT=${C8O_FLOW_POC_PORT:-19080}
WORKSPACE=${C8O_FLOW_POC_WORKSPACE:-"$ROOT/workspace/flow-poc-docker"}

AAA_PROJECT=${C8O_FLOW_POC_AAA_PROJECT:-/Users/nicolas/dev/convertigo/runtime-ConvertigoStudio/AAAProject}
FLOW_ENGINE_PROJECT=${C8O_FLOW_POC_ENGINE_PROJECT:-/Users/nicolas/git/lib_flow_engine}
MCP_PROJECT=${C8O_FLOW_POC_MCP_PROJECT:-/Users/nicolas/git/c8oprj-c8o-mcp}

sync_dir() {
	src=$1
	dst=$2
	if [ ! -d "$src" ]; then
		echo "Skip missing project: $src"
		return
	fi
	mkdir -p "$dst"
	if command -v rsync >/dev/null 2>&1; then
		rsync -a --delete --exclude .git --exclude _private "$src/" "$dst/"
	else
		rm -rf "$dst"
		mkdir -p "$dst"
		(cd "$src" && tar --exclude .git --exclude _private -cf - .) | (cd "$dst" && tar -xf -)
	fi
}

sync_classes() {
	"$ROOT/gradlew" -p "$ROOT" :engine:compileJava :engine:processResources
	rm -rf "$WORKSPACE/classes"
	mkdir -p "$WORKSPACE/classes"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/beans"
	cp "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/beans/BeansDefaultValues.class" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/"
	cp "$ROOT/engine/build/resources/main/com/twinsoft/convertigo/beans/database_objects.xml" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/"
	cp "$ROOT/engine/build/resources/main/com/twinsoft/convertigo/beans/database_objects_default.xml" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/beans/core"
	cp "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/beans/core/Project.class" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/core/"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/engine/enums"
	cp "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/engine/enums/DatabaseObjectTypes.class" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/engine/enums/"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/engine/helpers"
	cp "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/engine/helpers/WalkHelper.class" \
		"$WORKSPACE/classes/com/twinsoft/convertigo/engine/helpers/"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/beans/flow"
	cp -R "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/beans/flow/." \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/flow/"
	cp -R "$ROOT/engine/build/resources/main/com/twinsoft/convertigo/beans/flow/." \
		"$WORKSPACE/classes/com/twinsoft/convertigo/beans/flow/"

	mkdir -p "$WORKSPACE/classes/com/twinsoft/convertigo/engine/flow"
	cp -R "$ROOT/engine/build/classes/java/main/com/twinsoft/convertigo/engine/flow/." \
		"$WORKSPACE/classes/com/twinsoft/convertigo/engine/flow/"
}

sync_projects() {
	mkdir -p "$WORKSPACE/projects"
	sync_dir "$AAA_PROJECT" "$WORKSPACE/projects/AAAProject"
	sync_dir "$FLOW_ENGINE_PROJECT" "$WORKSPACE/projects/lib_flow_engine"
	sync_dir "$MCP_PROJECT" "$WORKSPACE/projects/ConvertigoMCP"
}

wait_http() {
	url="http://localhost:$PORT/convertigo/"
	i=0
	while [ "$i" -lt 90 ]; do
		code=$(curl -sS -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || true)
		if [ "$code" != "000" ]; then
			return 0
		fi
		i=$((i + 1))
		sleep 1
	done
	echo "Timed out waiting for $url"
	return 1
}

wait_ready() {
	url="http://localhost:$PORT/convertigo/api/mcp"
	body='{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"project-list","arguments":{}}}'
	i=0
	while [ "$i" -lt 120 ]; do
		response=$(curl -sS -H "Content-Type: application/json" -H "MCP-Protocol-Version: 2025-06-18" \
			--data "$body" "$url" 2>/dev/null || true)
		if echo "$response" | grep -q '"AAAProject"' && echo "$response" | grep -q '"lib_flow_engine"'; then
			return 0
		fi
		i=$((i + 1))
		sleep 1
	done
	echo "Timed out waiting for Convertigo projects to be ready"
	return 1
}

start() {
	if [ "${C8O_FLOW_POC_PULL:-false}" = "true" ]; then
		docker pull "$IMAGE"
	fi
	sync_classes
	sync_projects
	docker rm -f "$NAME" >/dev/null 2>&1 || true
	docker run -d \
		--name "$NAME" \
		-p "$PORT:28080" \
		-e LOG_STDOUT=true \
		-e LOG_FILE=false \
		-v "$WORKSPACE:/workspace" \
		"$IMAGE" >/dev/null
	wait_http
	wait_ready
	echo "Convertigo Flow POC: http://localhost:$PORT/convertigo"
}

stop() {
	docker rm -f "$NAME" >/dev/null 2>&1 || true
}

logs() {
	docker logs -f "$NAME"
}

test_weather() {
	internal_base=${C8O_FLOW_POC_INTERNAL_BASE:-http://127.0.0.1:28080}
	curl -sS --get \
		--data-urlencode "__sequence=WeatherAlert" \
		--data-urlencode "weatherUrl=$internal_base/convertigo/projects/lib_flow_engine/fixtures/weather-alert.json" \
		--data-urlencode "apiKey=demo-key" \
		--data-urlencode "threshold=35" \
		"http://localhost:$PORT/convertigo/projects/AAAProject/.json"
	echo
}

case "${1:-start}" in
	pull)
		docker pull "$IMAGE"
		;;
	sync)
		sync_classes
		sync_projects
		;;
	start)
		start
		;;
	restart)
		stop
		start
		;;
	stop)
		stop
		;;
	logs)
		logs
		;;
	test)
		test_weather
		;;
	*)
		echo "Usage: $0 [pull|sync|start|restart|stop|logs|test]"
		exit 2
		;;
esac
