# Native macOS PDE Studio launcher

`create-macos-pde-app.mjs` creates a small, separately identifiable macOS application
from an existing Eclipse installation and a working PDE launch configuration.
It uses the existing development classes; it is not a packaged Studio distribution.
The native launcher loads the JVM in-process, so desktop automation can target the
Studio application independently of its parent Eclipse.

Stop the test Studio before rebuilding. Compile the engine with Java 21, refresh
the projects in the parent Eclipse, and wait for its automatic Java build to finish.
Build `convertigo-studio-web` as usual when changing the embedded web assets.

Create the launcher once, supplying absolute paths:

```sh
node eclipse-plugin-studio/dev-tools/create-macos-pde-app.mjs \
  --eclipse-app '/Applications/Eclipse 2026-06.app' \
  --pde-config '/absolute/dev-workspace/.metadata/.plugins/org.eclipse.pde.core/ConvertigoStudioFlow' \
  --workspace '/absolute/test-workspace' \
  --java-home '/absolute/java-21-home' \
  --output '/absolute/ConvertigoStudio Develop.app'
```

The output must not exist. The script does not modify Eclipse, the source PDE
configuration, or an installed Convertigo Studio. It copies the small native launcher
and PDE configuration, retaining absolute references to the original bundles and
compiled project output. No classes or Linux artifacts are copied into the app.

Launch the resulting app normally (or target its exact path with desktop automation).
Never run it and the PDE debug launch against the same workspace at the same time.
Stop it before the next compile; relaunch to load the new classes. If Eclipse,
dependencies, or the PDE launch configuration change, generate a new launcher in
a fresh output directory. Keep these generated applications outside Git.

Validated on macOS arm64 with Eclipse 2026-06 and its bundled Java 21. The initial
project-loading phase can temporarily delay accessibility responses.

For UI automation with an AZERTY/custom layout, prefer menu actions to alphabetic
shortcuts. On the tested Mac, a requested `Cmd+A` coincided with application Quit
(suspected physical A/Q mapping). Arrow keys, F2, text entry and mouse actions
worked; do not infer that letter shortcuts have been validated.
