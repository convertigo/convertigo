# Convertigo Studio Electron

The Electron Convertigo Studio: a desktop window on the web Studio (`convertigo-studio-web`, under
`/convertigo/studio/`), served by a Convertigo engine that the application starts with its own Java
runtime and stops when it quits.

- `main.js` shows a splash page, starts the engine on the port of the workspace of the user (from
  28080, kept per workspace in `settings.json`, so that the preferences the Studio keeps in its page stay
  with the workspace), opens the Studio authenticated with a token given to the engine, and handles the
  workspace choice and switch, the menus, the unsaved-changes prompt and the DevTools bridge.
- `preload.js` exposes `window.convertigoStudio` to the Studio page, and `window.java` to the
  Marketplace frame, as the Eclipse Studio does.
- `agentBrowser.js` is the browser the agents of the Assistant drive to check the applications they
  build, as the browser of a project in the Eclipse Studio: the application started again in a process
  of its own, while the workspace has the agent stack (`lib_ConvertigoMCP`) and File > Agent Browser
  is checked. Its remote debugging port listens on the loopback only, the engine gets it as
  `-Dconvertigo.studio.cdp.port`, and it shows a single page, authenticated as the Studio, never the
  Studio itself. `Target.createTarget` is not supported: the agents drive that page.
- `engine/DevEngine.java` serves the exploded engine war in an embedded Tomcat.

## Prerequisites

- Node.js 22.17 or later (the web Studio needs it; CircleCI uses `studioWebNodeVersion` of
  `build.gradle`).
- JDK 25 to build the engine war and compile `DevEngine` (`JAVA_HOME` or `javac` on the `PATH`).
- `zip`, `unzip` and `tar`.
- macOS with the Xcode command line tools (`codesign`, `ditto`) for the macOS targets. Windows and
  Linux can be packaged from any system.

The build downloads the Tomcat jars from Maven Central and the Temurin JDK and jmods from GitHub,
each checked against its published checksum, into `.cache`.

## Build

From the root of the repository:

```bash
(cd convertigo-studio-web && npm ci && npm run build)   # writes eclipse-plugin-studio/tomcat/webapps/convertigo
./gradlew :engine:war                                   # writes engine/build/libs/convertigo-<version>.war
cd convertigo-studio-electron
npm ci
npm run engine                  # resources/engine: the war exploded, Tomcat, DevEngine, the Tomcat conf
npm run jre -- darwin-arm64     # resources/darwin-arm64/jre: the Java runtime of a target
npm run package -- darwin-arm64 # dist/Convertigo Studio-darwin-arm64 and its archive
npm run smoke-test              # starts the packaged application once, see below
```

The targets are `darwin-arm64`, `darwin-x64`, `win32-x64` and `linux-x64`; `all` names them all,
and no target means the one of the machine. `scripts/engine.mjs --war <file>` takes another war.

- The engine embeds the Tomcat of `tomcatVersion` in `build.gradle`, with the `context.xml` and
  `web.xml` of `eclipse-plugin-studio/tomcat/conf`.
- The Java runtime is a jlink of the Temurin release of `installers/nsis/download-jdk.sh`, the one of
  the Eclipse Studio. jlink links the runtime of any target from the jmods of that target, with the
  JDK of the machine of the same release (downloaded, or `JLINK_JDK`). Its legal notices are copied
  as files, since the links jlink makes break codesign.
- The Electron version is the one of `package.json`.

The archives are named as those of the Eclipse Studio:
`convertigo-studio-electron-<version>-{macosx-arm64.zip,macosx-x86_64.zip,win64.zip,linux64.tar.gz}`.

## Run

`npm start` runs the application from the sources, with the resources of the machine's target
(`npm run engine` and `npm run jre` first). The packaged application runs from `dist`.

The application keeps its data (settings, `engine.log`, the Tomcat base) in the Electron user data
folder (`~/Library/Application Support/Convertigo Studio` on macOS) and the projects in the workspace
chosen with File > Switch Workspace (`<data>/workspace` by default).

The engine runs with `-Dconvertigo.engine.web_studio=enabled`, `-Dconvertigo.engine.server_build=studio`,
`-Dconvertigo.engine.javascript.debugger=true` and `-Dconvertigo.studio.desktop=true` (a desktop Studio for
one user, as the Eclipse Studio: its PSC, the local agent of the Assistant), and gets a random
`CONVERTIGO_STUDIO_AUTH_TOKEN` that opens the Studio without the login page while it runs.

At its first start, the application asks to accept the license of Convertigo (`legal/dual-license.txt`, in the
war as `licenses/license.txt`), as the Convertigo Setup wizard of the Eclipse Studio, and quits when it is not
accepted; the answer is kept in `settings.json`.

As the Convertigo Setup wizard of the Eclipse Studio, the Studio asks at its start for the Personal Studio
Configuration (PSC) of a workspace without one: the registration page of Convertigo Cloud gives it back through
`window.studio.setPSC`, which `preload.js` exposes to that page, or the user pastes it. The engine writes it in
`<workspace>/studio/psc.txt`.

These variables leave the user's data alone, for tests:

| Variable | Use |
| --- | --- |
| `CONVERTIGO_STUDIO_DATA` | the data folder of the application |
| `CONVERTIGO_STUDIO_WORKSPACE` | the workspace, not kept in the recent ones |

## Smoke test

`npm run smoke-test [-- <target> | <executable>]` starts the packaged application with Playwright on
new data and workspace folders, waits for the Studio, and fails when it does not open or when the
engine logs an error other than the `ClassNotFoundException` of `org.apache.jasper.servlet.JspServlet`
(the embedded Tomcat has no JSP). `SMOKE_TEST_DIR` keeps its logs and screenshot in a given folder.

## Signing

Signing is driven by environment variables and skipped without them:

| Variable | Use |
| --- | --- |
| `APPLE_SIGN_IDENTITY` | the Developer ID Application identity, in the keychain, that signs the macOS application with the hardened runtime (the Java runtime with `installers/entitlements.plist`, as the Eclipse Studio); the macOS native libraries inside the engine jars are signed too. Without it, the application is signed ad hoc and macOS asks to confirm its first launch. |
| `APPLE_ID`, `APPLE_PASSWORD`, `APPLE_TEAM_ID` | submit the archive of the signed macOS application to the notarization, without waiting for its result nor stapling it, as the Eclipse Studio: Gatekeeper checks it online at the first launch |
| `WINDOWS_PFX`, `WINDOWS_PFX_PASSWORD` | sign `Convertigo Studio.exe` (signtool on Windows, osslsigncode elsewhere) |
| `WINDOWS_TIMESTAMP_URL` | the timestamp server, `http://timestamp.digicert.com` by default |

The Linux archive is not signed. Electron's `chrome-sandbox` needs either user namespaces or
`chown root` and `chmod 4755` to run.

## CircleCI

`.circleci/config.yml` builds it after `build_engine_war`, on the `develop` branch only for now:

- `build_studio_electron` (Linux) downloads the war of the build from S3, prepares the engine, links
  the four runtimes, packages Windows and Linux (the executable signed with the certificate of the
  Eclipse Studio when `PFX_AES` and `PFX_PWD` are set), uploads them to S3 next to the war, and hands
  the macOS resources to the next job.
- `build_studio_electron_mac` (macOS) packages both macOS targets, signed with the Developer ID of the
  Eclipse Studio (`APPLE_CERT_DEV_ID_APP_BASE64`, `APPLE_CERT_PASSWORD`, `APPLE_TEAM_ID`) and submitted
  to the notarization without waiting for it (`APPLE_ID`, `APPLE_PASSWORD`), as `build_studio_mac`, when
  they are set, ad hoc otherwise, smoke tests the arm64 one and
  uploads them to S3. The smoke test logs and screenshot are in the job artifacts.
