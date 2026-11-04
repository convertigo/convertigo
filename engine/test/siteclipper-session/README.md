# SiteClipper session store regression fixture

This disposable stack runs a local HTTP target, a Tomcat session store engine,
and two engines sharing a Redis session store. The project has an `Initialize`
sequence that creates the named `clip` context when called with
`__context=clip&__removeContext=false`. It returns a SiteClipper URL. The target
sets a cookie on the first call; the second call must send it back. A subsequent
`State` sequence checks that the context remains usable.

The fixture is isolated under the Compose project `eph-f911de8d`. The only
published ports are loopback HTTP ports 29481, 29482 and 29483. Redis has no
published port. Credentials are test-only. All containers, named volumes and
the network carry the `pilotage.owner` label. Each engine's `/workspace` has a
named volume, and `/workspace/classes` is a read-only bind mount.

From this worktree, before a fix use an empty classes directory:

```sh
export C8O_CLASSES_DIR="$PWD/engine/test/siteclipper-session/baseline-classes"
docker compose -p eph-f911de8d -f engine/test/siteclipper-session/compose.yaml up -d --pull never
```

After the Java build, point `C8O_CLASSES_DIR` to `engine/build/classes/java/main`
and recreate only this stack's engines. For either variant, load the project:

```sh
for service in tomcat redis-engine redis-second; do
  docker compose -p eph-f911de8d -f engine/test/siteclipper-session/compose.yaml exec -T "$service" mkdir -p /workspace/projects/SiteClipperStoreTest
  docker compose -p eph-f911de8d -f engine/test/siteclipper-session/compose.yaml cp engine/test/siteclipper-session/project/. "$service":/workspace/projects/SiteClipperStoreTest/
done
docker compose -p eph-f911de8d -f engine/test/siteclipper-session/compose.yaml restart tomcat redis-engine redis-second
python3 engine/test/siteclipper-session/probe.py
```

The probe creates separate sessions for each scenario. The CI image's session
quota may require clearing this disposable stack's Redis data between runs;
use only the labeled `eph-f911de8d-redis-1` container. Never clear another
Redis instance. The probe reports PASS/FAIL without printing session cookies.

At the end, stop this stack without deleting its resources:

```sh
docker compose -p eph-f911de8d -f engine/test/siteclipper-session/compose.yaml stop
```
