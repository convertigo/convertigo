# Default Node.js runtime

`nodeRuntimeVersion` in the root `build.gradle` selects the default Node.js LTS
distribution. Gradle generates `com/twinsoft/convertigo/engine/node-runtime.properties` on the
engine classpath; do not edit that generated resource. The dependency maintenance helper
checks this Gradle property against the official Node.js LTS index. The separate
`studioWebNodeVersion` selects the tool used to build the admin web application.

The default runtime is used by services such as PouchDB and by callers of
`ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion())`. Resolution is:

1. `-Dconvertigo.node.path=/absolute/path/to/node-distribution`, when set. This
   may name the distribution root or the directory containing `node`/`node.exe`.
   It must contain an executable; an invalid override fails explicitly. Select a
   Node/npm version compatible with the services you run. This setting is read
   by the engine, not inherited from the shell's `PATH`.
2. The platform-specific distribution under `<webapp>/WEB-INF/node`, whose
   `.convertigo-node-version` marker must match the configured default version.
   The engine does not modify this directory; it can be read-only.
3. The existing runtime cache, followed by an on-demand download if necessary.
   The cache remains in the local working directory when configured, otherwise
   in the workspace. A portable WAR includes no Node binary and follows this path.

`getDefaultNodeDir()` discovers the location without downloading. `getNodeDir()`
ensures that the requested runtime is available and returns its executable
directory. An already loaded default path registered by the Studio is reused
after the override and bundle; it does not change the version selected by Gradle.

Project templates keep their own `nodeJsVersion` from `version.json`. A request
for another version ignores both the default override and the bundled runtime
and uses that version's cache/download. CLI builds, local builds and NGX editors
retain this behavior. Libraries with explicit executable paths retain their
own choices too.

## Packaging

- `generateDockerfile` renders the default and AKS Dockerfiles with the Gradle
  version. Both download the official Linux archive for the image architecture,
  verify its SHA-256 and install it after expanding the WAR.
- `bundleStudioNodeRuntimes` adds the appropriate distribution to each assembled
  Eclipse Studio before signing and archiving. Run it after the Tycho product
  build. CI invokes it for Windows x64, Linux x64 and macOS x64/arm64.
- `bundleElectronNodeRuntimes` prepares the four Electron packaging targets.
  `scripts/package.mjs` copies only the selected target into the engine's webapp,
  before signing. This runtime is separate from Electron's internal JavaScript
  runtime.
- `makeWin64ServerZip` includes `copyWin64ServerNode` and the Windows x64 archive.
- `prepareNodeRuntimes` prepares all five supported distributions under
  `build/node-runtime` for inspection. Each individual `prepareNodeRuntime*`
  task can also be run separately.

The packaging tasks verify the archive against the official `SHASUMS256.txt`,
keep Unix executable modes and npm/npx symlinks, and retain the distribution's
original `LICENSE` and other notices. `legal/third-party-notices.txt` documents
the bundled runtime and is included beside it. No binary is committed.

When changing the default version, regenerate the engine resource and Dockerfiles,
prepare the distributions, run `:engine:nodeRuntimeTest` and exercise `node`, npm
and npx from each target. The final signed installers still require platform
qualification; inspecting their staging directories is not a Windows launch test.
