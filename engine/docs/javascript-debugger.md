# JavaScript debugging

The web Studio and Eclipse `JavaScript Debug (Web)` view use the same administrator API and Monaco component. The existing Eclipse Swing debugger remains available separately. Do not attach both debuggers simultaneously. In Eclipse, **Debug Eclipse launches** is checked by default when starting the debugger. Run from the sequence, connector, test case or Execute commands as usual. The Project / Sequence form is optional, under **Run a sequence from this view**.

## Enable and authorize

On a server, enable `web_studio=enabled` and `javascript.debugger=true` explicitly, in the administrator configuration or with `-Dconvertigo.engine.javascript.debugger=true`. Debugging defaults to false on a server. Studio mode permits it. All debugger operations require the `WEB_ADMIN` role, administrator authentication and normal XSRF controls. No ordinary request is selected implicitly.

Expression evaluation executes administrator code in the paused request and can have side effects. Each evaluation attempt writes a fixed message to the audit logger, without the expression, result, source or session token. Hovers only display inspected bindings: they do not evaluate expressions or invoke getters.

## Protocol

POST form parameters to `admin/services/studio.debug.Debugger`. Responses contain `state`: `enabled`, `attached`, `sessionId`, `serial`, `pauseId`, `running`, `workers`, `stopped`, `breakOnExceptions`, `sources`, `frames` (innermost first), `notice`, `pauseTimeoutMillis`, and a bounded `events` history. Poll `state` once per second while attached. Events have a monotonic serial and a type, including `started`, `source`, `pause`, `exception`, `resumed`, `finished`, `detached`, `breakpoints`, `settings`, `breakRequested`, `autoResumed`, `breakpointsRemapped`, `unsupportedRuntime`.

| action | additional parameters | response |
|---|---|---|
| `start`, `state`, `stop`, `pause` | none | state |
| `studioRequests` | `value=true/false`, Studio mode only; one administrator owns the Eclipse launch bridge | state |
| `breakOnEnter`, `breakOnReturn` | `value=true/false`; JavaScript functions only | state |
| `breakOnExceptions` | `value=true/false` | state |
| `breakpoint` | `url`, `line` (one-based), `set=true/false` | accepted, breakpoints |
| `breakpoints` | `url` | breakpoints |
| `source` | `url` | source: url, source text, breakable lines, breakpoints, location |
| `go`, `stepInto`, `stepOver`, `stepOut` | current `pauseId` | state |
| `variables` | current `pauseId`, `frame` (zero-based) | scope, parent scopes, this |
| `properties` | current `pauseId`, opaque `ref`, `offset` (0, 100, …) | shallow variables page, nextOffset (-1 at end) |
| `value` | current `pauseId`, opaque primitive `ref` | full value (up to 65536 characters), truncated |
| `eval` | current `pauseId`, `frame`, `expression` | result preview, evaluationError |

Instead of `url`, an object editor supplies `id` and `property`. The response supplies `url` and `offset`; breakpoint lines are relative to the property, while frame lines refer to the executed source including generated prefixes. An expired or stale pause rejects inspection and resume commands.

Pass `__javascriptDebug=<sessionId>` in the **body** of a requestable request, using the same authenticated HTTP session. The web Studio marks its requests while attached. Selected requests bypass response caches without replacing ordinary cached responses. Unmarked requests keep their compiled execution path. A second independent request in the same debug session is rejected; another administrator HTTP session may run its own debug request.

## Workers and lifetime

Nested requestables retain their root binding. Subsequences on separate workers and parallel steps join through explicit scheduling boundaries; no inheritable thread state is installed. Each worker has its own stack and interpreted context. At most one worker of a root request pauses at a time; siblings continue. Step commands follow the selected worker. A session retains at most 64 workers.

`javascript.debugger.pause_timeout` sets the maximum duration of a pause in seconds: default 120, bounded to 1..3600, captured when the session starts. Polling does not extend this deadline. A pause automatically resumes with an `autoResumed` event and visible notice. A session also expires after 60 seconds without an administrator operation. Stopping, disabling, logging out, or destroying the HTTP session releases pauses and clears ownership. Detaching does not cancel the business request; pending child work continues without debugging.

Only selected requests enter private Rhino factories. No global listener or cache invalidation is installed. Ordinary requests retain their compiled scripts, including when another request is paused.

## Inspection and source changes

Inspection returns shallow property rows with name, preview, type, an opaque reference and expansion availability. Objects, arrays and Java public instance fields are loaded only when expanded. Each property page contains at most 100 rows and a `nextOffset`; pagination also applies to root scopes. Rhino top-level lexical bindings and globals remain available in Locals. All scopes exposes each lexical/parent scope explicitly instead of flattening away shadowed bindings. Cycles reuse their reference and can be opened deliberately; the UI bounds display nesting to 32 levels.

Java methods, accessors, proxies and custom string conversion are not invoked. Opaque host objects remain previews. Limits: eight scope levels, 4096 live references per pause, 500 characters per name/primitive preview; clicking a value displays its complete primitive text up to 65536 characters with an explicit truncation notice beyond that. Array pagination covers up to one million elements. References are invalidated on resume, a new pause, auto-resume and detach. Source and frame metadata include readable project / sequence / step / property labels and the editable object/property identifiers; the runtime URL is still the exact breakpoint key. Rhino evaluation failures return their detailed message only through the protected debugger response.

Breakpoints survive between executions in an attached session. When a source URL is compiled with changed text, unchanged prefixes/suffixes and unique surviving lines remap its breakpoints before execution. Deleted, ambiguous or nonexecutable lines are cleared with a visible notice. Unsaved edits do not change a running script. Refreshing/rerunning after applying an edit loads the new positions; a renamed source is a new source. Detaching clears the session's sources and breakpoints.

Other limits: 16 sessions, 64 sources, 262144 characters per source, 2097152 source characters per session, 64 event records, 256 breakpoints per source, and 4096 characters per evaluation. Evaluation has a 500 ms JavaScript instruction deadline. Blocking native/Java calls cannot be forcibly terminated safely.

## Covered and refused execution

JavaScript steps, step expressions, transaction handlers, `include()` and `use()` execute interpreted code for selected requests. Included files are named by project/path. `use()` evaluates private functions for debugging without reading or publishing shared cached functions; ordinary cache identity is preserved.

Provider calls run with the request debugger temporarily detached before accessing prepared runtimes or shared compiled modules. Providers can opt into private block implementations with `JavascriptDebugger.providerDebugging()`, `compileProviderImplementation(source, projectRelativeUrl, scope)` and `callProviderFunction(function, receiver, arguments, scope)`. A selected request gets interpreted implementations with debug information, compiled directly without consulting or publishing shared script/bytecode caches. Initialization runs without pauses. Only calls of these private functions reattach the request debugger; inspection stops before the supplied provider scope. Providers must create and retain these instances in invocation-owned state, after shared preparation has completed. Prepared-runtime builders suppress attachment, including nested callbacks.

With lib_flow_engine 0.2.19, the invocation gets a private execution plan whose Rhino block adapters bypass shared implementations and prepared runners, including stateless ones. Implementations use the real project/file source URL, preserve original file line numbers, and share the editor helper-prefix convention with included files. All explicitly selected requests take this path, even before a breakpoint is set, so Pause next and breakpoints set during execution work. Unselected requests retain their original plans, compiled runners and cache identity. Flow orchestration, libraries and engine modules remain compiled and do not produce debugger frames. The panel explains whether a private block implementation has been reached. Legacy Rhino frames, context settings and stepping are restored when the provider returns or throws. Stopping the debugger releases a pause and lets the business request continue. Direct shared-compilation entry points outside the suspension boundary remain guarded against debug cache reuse. Native Java code is not line-debuggable.

No debugger payload is written to generic administration response logs. Sources and values stay in memory, not local storage. Tabs sharing an HTTP session share its debugger; isolation is by administrator HTTP session, not browser tab. Business request timeouts remain effective while paused.

## Eclipse launch bridge and editor markers

Only `ConvertigoPlugin.runRequestable` marks local `InternalHttpServletRequest` instances with an in-process session object. The administrator API action `studioRequests` explicitly enables the bridge for the owning session, and refuses servers and competing owners. InternalRequester alone, a session named "studio", and an HTTP request without the administrator's existing marker cannot select it. Disabling the checkbox, stopping, expiry and logout remove the bridge; disposing the Eclipse view releases the bound session. An already selected business request continues when detached. A second root request is refused with a visible `requestRejected` notice; nested workers keep the existing root lease.

Before a conventional Eclipse launch, the view copies enabled line breakpoints from Eclipse's breakpoint manager (including its global enable switch). A 250 ms SWT timer follows edits, removals and pause state without evaluating code or touching session lifetime. The exact engine URL is stored on each property editor file: short qualified object name plus the API's property script name (`expression`, `LogStep`, `message`, transaction name for `handlers`, `ctxName`, or `property-smartType` for JavaScript SmartType properties). Property editors use separate backing files for separate properties. They already contain the generated helper prefix, so marker and frame lines map directly. Ordinary project `.js` files map to `project/relative/path.js` and account for the optional generated helper prefix. Files outside the project and generated private files without source metadata are ignored. Synchronization from Monaco back to Eclipse markers is not implemented.

At a new pause the native view opens or reveals the matching Eclipse file, annotates the stopped line with the standard current instruction pointer, and clears the annotation on resume, detach or disposal. The embedded page sends a selected frame index, session ID and pause ID through its main-frame Java bridge. Java validates them against the live session and reads the source and line from its own stack before opening the Eclipse editor. Selecting a caller moves the native annotation to that frame. No arbitrary file path or line is accepted from the page. A property editor that has never been opened can be created from the recorded object/property metadata.

The shared panel provides **this**, **Locals**, **All scopes** (expandable lexical and parent scopes), a selectable call stack, evaluation history and up to 20 watched expressions. Watches are reevaluated on every pause and frame selection, and cleared on detach. Results from stale pauses or frame selections are discarded. Watches execute administrator code through the existing bounded evaluation API, with the same fixed audit message and possible side effects; values and expressions remain in memory. Function entry and normal return pauses keep the function frame inspectable. Throwing exits use the exception setting instead.


## Debugger presentation and the main editors

The Eclipse view contains controls, stack, property table, evaluation and watches. It has no source editor, script list or request launch form; launches come from Eclipse editors. Admin Skeleton switches and grouped Studio icon buttons share the admin themes. A Ready / Running / Paused / Detached badge remains visible, with an inspection placeholder while execution is between pauses.

In the Web Studio dock, selecting a paused frame opens the existing property or project-file tab in `StudioEditorPanel`. That Monaco shows the selected frame line, debugger breakpoint margin and safe binding previews. Existing unsaved content is preserved. Scripts without a resolvable editable object use a read-only runtime snapshot in the same central editor. The independent `/javascript-debugger` administration page retains its source selector/editor and optional request form for use outside either Studio.

Evaluation history and request-form status are reset at each new pause. Late evaluation, watch, source and property-page responses cannot replace a newer pause/frame selection. Stopping explicitly detaches and lets the paused business request finish. Legacy Backend Debug attachment and the web debugger attachment refuse one another in both directions; a refused legacy view can be closed and reopened after the web debugger is stopped.
