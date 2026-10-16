# Invalid request targets and application sessions (#1235)

Opt-in, disposable HTTP regression fixture. It runs one Tomcat-backed engine and
one Redis-backed engine, with isolated workspaces and a test-only cookie name.
Only loopback HTTP ports are published. Do not point the probe at an existing
application server or reuse these credentials in production.

From the hotfix checkout:

```sh
./gradlew :engine:classes :engine:invalidRequestSessionTest
export C8O_HOTFIX_ROOT="$PWD"
docker pull convertigo/convertigo-ci:hotfix
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml up -d
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml exec -T tomcat mkdir -p /workspace/projects/SessionTest
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml exec -T redis-engine mkdir -p /workspace/projects/SessionTest
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml cp engine/test/request-session/project/. tomcat:/workspace/projects/SessionTest/
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml cp engine/test/request-session/project/. redis-engine:/workspace/projects/SessionTest/
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml restart tomcat redis-engine
```

Wait until both engines have started, then run:

```sh
python3 engine/test/request-session/probe.py
```

The probe checks missing projects (including `__project` overrides), sequences,
connectors and transactions without a session cookie; anonymous/authenticated
state and retained contexts after invalid calls and concurrent calls; SOAP
target resolution; and explicit session termination. It waits longer than the
old one-second error-cleanup timeout before checking state again.

Manual authenticated scenario, in the same browser and on the same port:

1. [Tomcat login](http://localhost:29135/convertigo/projects/SessionTest/.json?__sequence=Login&__removeContext=false)
2. [Invalid target](http://localhost:29135/convertigo/projects/Missing/.json?__sequence=Check)
3. Wait a few seconds, then [check authentication and state](http://localhost:29135/convertigo/projects/SessionTest/.json?__sequence=Check&__removeContext=false).

Repeat with port **29136** for Redis. The final response must still contain
`session-test-user` and `keep`. A fresh client requesting only the invalid target
must not receive `Set-Cookie` or create a stored session.

The Java regression suite additionally covers testcase target defaults, internal
requesters, sessions acquired earlier by the protocol, authentication occurring
before final cleanup, and the validation exemption for an already registered
asynchronous job. The HTTP probe does **not** validate asynchronous job execution:
an exploratory test exposed a separate `Context.clearRequest()` / asynchronous
`CacheManager.getDocument()` race (`requestedObject` becomes null), outside this fix.

The project fixture uses Convertigo's YAML format, including a space following
empty bean declarations (`]: `).

To remove only this disposable stack and its test workspace volumes:

```sh
docker compose -p c8o-1235 -f engine/test/request-session/compose.yaml down --volumes
```
