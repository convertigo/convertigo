# Live JavaScript debugger recipe

Use a private runtime, never a shared developer engine. Build with JDK 25 and Node 22+:

```sh
./gradlew :engine:check :engine:jar
cd convertigo-studio-web
npm ci
npm run build
```

Create your own container from `convertigo/convertigo-ci:develop`, bound only to a free loopback port. Enable `web_studio=enabled` and `javascript.debugger=true` through `JAVA_OPTS` and use the local test administrator `admin/admin`. Deploy the built engine JAR to `/usr/local/tomcat/webapps/convertigo/WEB-INF/lib/`, and the generated static web files from `eclipse-plugin-studio/tomcat/webapps/convertigo/` (including `_app`, `admin`, `studio`, `login` and `javascript-debugger`) to the corresponding paths in that container. Restart only this container after updating the engine.

Copy `project/` to `/workspace/projects/JavascriptDebuggerTest` in this container, and load it through the administration console (or `projects.Reload` with `projectName=JavascriptDebuggerTest`). It uses only an in-memory JavaScript sequence and a local included file.

From `convertigo-studio-web`:

```sh
C8O_DEBUGGER_TEST_URL=http://127.0.0.1:PORT/convertigo \
  npx playwright test --config playwright.debugger.config.js
```

The tests are skipped in ordinary integration runs unless this URL is supplied. They authenticate through the administrator service, then exercise the actual embedded page and the Debug dock of the Web Studio. No debugger response or token is logged. Tracing and HAR recording are not enabled.

The recipe checks the server-only refusal of `studioRequests`, unauthenticated access, explicit request selection, an editor-property breakpoint, `this` / Locals / Alls, a watched expression on different frames, evaluation, an ordinary request continuing during a pause, a second debug root being refused with a visible notice, stepping, pause-next, continuation and stopping during a pause. It can save fixture-only screenshots to the optional `C8O_DEBUGGER_SCREENSHOT_DIR`.

For native Eclipse verification, import the same project into a separate workspace and follow the Mission 27 report. This live server test cannot exercise SWT markers or the Studio-only internal launch bridge.
