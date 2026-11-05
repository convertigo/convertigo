# SiteClipper session store concurrency fixture

This isolated stack runs a target, a Tomcat session store engine, and two engines
sharing Redis. `Initialize` creates the named `clip` context. The target serves
an HTML page with eight assets, delays each asset by 300 ms, and sets a cookie
on the second SiteClipper call. The probe checks that the next call sends the
cookie, that eight assets complete in parallel, and that an absent context is
not created while the initialized one remains available.

The Compose project and every service, volume, and network use the
`eph-f4e5d644` owner label. All image-declared volumes have named labeled
mounts: `/workspace` for the engines and `/data` for Redis. The only published
ports are `127.0.0.1:29591`, `:29592`, and `:29593`; Redis is private.

Run from the worktree with `convertigo/convertigo-ci:hotfix` already local:

```sh
export C8O_CLASSES_DIR=/Users/nicolas/Documents/convertigo/preuves/eph-f4e5d644/baseline-classes-exact
docker compose -p eph-f4e5d644 -f engine/test/siteclipper-session/compose.yaml up -d --pull never
for service in tomcat redis-engine redis-second; do
  docker compose -p eph-f4e5d644 -f engine/test/siteclipper-session/compose.yaml exec -T "$service" mkdir -p /workspace/projects/SiteClipperStoreTest
  docker compose -p eph-f4e5d644 -f engine/test/siteclipper-session/compose.yaml cp engine/test/siteclipper-session/project/. "$service":/workspace/projects/SiteClipperStoreTest/
done
docker compose -p eph-f4e5d644 -f engine/test/siteclipper-session/compose.yaml restart tomcat redis-engine redis-second
python3 engine/test/siteclipper-session/probe.py before
```

For the correction, set `C8O_CLASSES_DIR` to the absolute path of
`engine/build/classes/java/main`, recreate only the three engines with
`up -d --no-deps --force-recreate --pull never`, and run `probe.py after`.
The baseline classes of the four changed Java sources are compiled from commit
`5396a5cd3` into the private proof directory; the corrected classes are
compiled from this worktree. Reset
only this stack's Redis between variants to avoid the test edition session
quota. Check all container volume mounts and their labels with `docker inspect`
before probing. Stop the project without removing its containers or volumes:

```sh
docker compose -p eph-f4e5d644 -f engine/test/siteclipper-session/compose.yaml stop
```
