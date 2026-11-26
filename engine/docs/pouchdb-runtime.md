# PouchDB server runtime

When FullSync uses PouchDB (`convertigo.engine.fullsync.pouchdb=true`), the engine runs
pouchdb-server with the default Node.js runtime (see `node-runtime.md`). pouchdb-server is a
bundle built by the CouchDB provider (`convertigo-provider-couchdb`, `pouchdb` zip): one script,
`pouchdb-server.cjs`, the leveldown binaries of every platform, `THIRD-PARTY-NOTICES.txt` and
`bundle-info.json`. It is the same for every platform.

Resolution, on each start of PouchDB:

1. `<webapp>/WEB-INF/pouchdb`, when its `bundle-info.json` matches the package lock and the
   patches of the provider and it has the leveldown binary of the platform. It is used in
   place: the engine does not modify this directory, it can be read-only.
2. Otherwise the engine logs why and installs pouchdb-server with npm in `<workspace>/pouchdb`,
   as before (Internet access to the npm registry is required).

The databases, `config.json` and `log.txt` are always in `<workspace>/pouchdb`. A
`node_modules` installed there by a previous version is not used anymore with the bundle, and
is left in place.

## Packaging

`:engine:installPouchDbBundle` installs the bundle in
`eclipse-plugin-studio/tomcat/webapps/convertigo/WEB-INF/pouchdb`: the dev Studio started from
Eclipse uses it after `./gradlew setupStudioDev` (or `writeManifest`), and the Eclipse Studios
always get it from this directory. `:engine:war` includes it too, so the portable WAR, Docker
images, Electron Studios and Windows Server have it.
