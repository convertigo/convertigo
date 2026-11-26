# PouchDB server runtime

When FullSync uses PouchDB (`convertigo.engine.fullsync.pouchdb=true`), the engine runs
pouchdb-server with the default Node.js runtime (see `node-runtime.md`). pouchdb-server is a
bundle built into the CouchDB provider JAR (`convertigo-provider-couchdb`): one script,
`pouchdb-server.cjs`, the leveldown binaries of every platform, `THIRD-PARTY-NOTICES.txt` and
`bundle-info.json`. It is the same for every platform.

Resolution, on each start of PouchDB:

1. The engine checks the manifest in `<webapp>/WEB-INF/pouchdb` against the copy in the provider JAR.
   It verifies the package lock, patch version and file checksums, and requires regular files within
   the bundle directory. The selected Node.js must load the bundled LevelDOWN addon before the
   server opens any databases. The engine uses the bundle in place and can run with read-only access.
2. Without a usable bundle the engine installs PouchDB with `npm ci` in a staging directory.
   It applies the provider patches, checks the installed script and addon, and atomically activates
   the installation under `<workspace>/pouchdb/npm`, keyed by the package lock and patch version.
   The first installation of each revision requires access to the npm registry.

The databases, `config.json` and `log.txt` are always in `<workspace>/pouchdb`. Any previous
`node_modules` directory in that workspace is left in place.

When a separate account runs Convertigo, keep the provider JAR, the installed bundle and their parent
directories owned by the deployment account, and give the service account read and traversal access.

## Packaging

`:engine:installPouchDbBundle` installs the bundle in
`eclipse-plugin-studio/tomcat/webapps/convertigo/WEB-INF/pouchdb`: the dev Studio started from
Eclipse uses it after `./gradlew setupStudioDev` (or `writeManifest`), and the Eclipse Studios
always get it from this directory. The task extracts the bundle from the provider JAR on the Studio
plugin's classpath. The WAR prepares its own copy from the provider JAR on its runtime classpath.
Docker images, Electron Studios and Windows Server get their copy from the portable WAR unless
`pouchDbInWar` is false.

The macOS Studio packagers sign the native addon before signing the application. They update its
digest and its bytes in both the installed bundle and the accompanying provider JAR.

`ext.pouchDbInWar = true` in the root `build.gradle` is the only switch: with false, the WAR, hence
the Docker images, does not include the PouchDB server, and FullSync with PouchDB installs it with
npm on first use.
