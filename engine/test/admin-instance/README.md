# Administration forwarding after Redis discovery expiry (#1231)

Disposable local topology: two Convertigo instances and a private Redis, with test-only passwords.
Never run the crash probe against an existing customer or development stack.

From the repository root:

```sh
./gradlew :engine:classes
npm --prefix convertigo-studio-web run build
docker pull convertigo/convertigo-ci:hotfix
C8O_HOTFIX_ROOT="$PWD" docker compose -p c8o-1231-ttl -f engine/test/admin-instance/compose.yaml up -d
```

Wait for both instances to start. Copy the newly built frontend into both disposable containers:

```sh
for instance in a b; do
  for asset in _app admin login; do
    docker cp "eclipse-plugin-studio/tomcat/webapps/convertigo/$asset/." \
      "c8o-1231-ttl-engine-$instance-1:/usr/local/tomcat/webapps/convertigo/$asset/"
  done
done
node engine/test/admin-instance/probe.cjs
```

The probe uses the installed `@playwright/test` Chromium browser. It logs into
<http://127.0.0.1:29141/convertigo/admin/>, selects the other instance, and abruptly
stops **only** `c8o-1231-ttl-engine-b-1`. It checks the selector/banner immediately,
waits at least 45 seconds (discovery TTL is 30 seconds), verifies B is absent from
Redis discovery, and repeats the targeted requests and UI refreshes. These must
still return 502 identifying B, without switching to A or hiding the selector.
The explicit local-instance button must restore the local UI. The stopped container
is restarted in `finally`.

Screenshots, HTTP trace and summary are written to `/tmp/c8o-1231-ttl`, or the directory
set in `C8O_PROBE_OUTPUT`.

Cleanup:

```sh
C8O_HOTFIX_ROOT="$PWD" docker compose -p c8o-1231-ttl -f engine/test/admin-instance/compose.yaml down
```
