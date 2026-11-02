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
| `source` | `url` | source: url, source text, breakable lines, breakpoints |
| `go`, `stepInto`, `stepOver`, `stepOut` | current `pauseId` | state |
| `variables` | current `pauseId`, `frame` (zero-based) | scope, parent scopes, this |
| `eval` | current `pauseId`, `frame`, `expression` | result preview, evaluationError |

Instead of `url`, an object editor supplies `id` and `property`. The response supplies `url` and `offset`; breakpoint lines are relative to the property, while frame lines refer to the executed source including generated prefixes. An expired or stale pause rejects inspection and resume commands.

Pass `__javascriptDebug=<sessionId>` in the **body** of a requestable request, using the same authenticated HTTP session. The web Studio marks its requests while attached. Selected requests bypass response caches without replacing ordinary cached responses. Unmarked requests keep their compiled execution path. A second independent request in the same debug session is rejected; another administrator HTTP session may run its own debug request.

## Workers and lifetime

Nested requestables retain their root binding. Subsequences on separate workers and parallel steps join through explicit scheduling boundaries; no inheritable thread state is installed. Each worker has its own stack and interpreted context. At most one worker of a root request pauses at a time; siblings continue. Step commands follow the selected worker. A session retains at most 64 workers.

`javascript.debugger.pause_timeout` sets the maximum duration of a pause in seconds: default 120, bounded to 1..3600, captured when the session starts. Polling does not extend this deadline. A pause automatically resumes with an `autoResumed` event and visible notice. A session also expires after 60 seconds without an administrator operation. Stopping, disabling, logging out, or destroying the HTTP session releases pauses and clears ownership. Detaching does not cancel the business request; pending child work continues without debugging.

Only selected requests enter private Rhino factories. No global listener or cache invalidation is installed. Ordinary requests retain their compiled scripts, including when another request is paused.

## Inspection and source changes

Snapshots include recursive object properties, arrays, Rhino lexical bindings and public Java instance fields / Java array elements. Java methods, accessors, proxies and custom string conversion are not invoked. Opaque host objects remain previews. Limits: eight scope levels, three object levels below a scope, 100 properties per object, 1000 nodes per snapshot, 500 characters per name/primitive preview. Cycles and depth/size limits are marked `limited`; the tree displays them explicitly. Very large arrays show only their first 100 elements.

Breakpoints survive between executions in an attached session. When a source URL is compiled with changed text, unchanged prefixes/suffixes and unique surviving lines remap its breakpoints before execution. Deleted, ambiguous or nonexecutable lines are cleared with a visible notice. Unsaved edits do not change a running script. Refreshing/rerunning after applying an edit loads the new positions; a renamed source is a new source. Detaching clears the session's sources and breakpoints.

Other limits: 16 sessions, 64 sources, 262144 characters per source, 2097152 source characters per session, 64 event records, 256 breakpoints per source, and 4096 characters per evaluation. Evaluation has a 500 ms JavaScript instruction deadline. Blocking native/Java calls cannot be forcibly terminated safely.

## Covered and refused execution

JavaScript steps, step expressions, transaction handlers, `include()` and `use()` execute interpreted code for selected requests. Included files are named by project/path. `use()` evaluates private functions for debugging without reading or publishing shared cached functions; ordinary cache identity is preserved.

Prepared provider runtimes and shared compiled modules are explicitly refused before accessing their caches. Their entry points publish a value-free `unsupportedRuntime` notice displayed in the same component in both Studios. Detach the debugger to execute such a request. Native Java code is not line-debuggable.

No debugger payload is written to generic administration response logs. Sources and values stay in memory, not local storage. Tabs sharing an HTTP session share its debugger; isolation is by administrator HTTP session, not browser tab. Business request timeouts remain effective while paused.

## Eclipse launch bridge and editor markers

Only `ConvertigoPlugin.runRequestable` marks local `InternalHttpServletRequest` instances with an in-process session object. The administrator API action `studioRequests` explicitly enables the bridge for the owning session, and refuses servers and competing owners. InternalRequester alone, a session named "studio", and an HTTP request without the administrator's existing marker cannot select it. Disabling the checkbox, stopping, expiry and logout remove the bridge; disposing the Eclipse view releases the bound session. An already selected business request continues when detached. A second root request is refused with a visible `requestRejected` notice; nested workers keep the existing root lease.

Before a conventional Eclipse launch, the view copies enabled line breakpoints from Eclipse's breakpoint manager (including its global enable switch). A 250 ms SWT timer follows edits, removals and pause state without evaluating code or touching session lifetime. The exact engine URL is stored on each property editor file: short qualified object name plus the API's property script name (`expression`, `LogStep`, `message`, transaction name for `handlers`, `ctxName`, or `property-smartType` for JavaScript SmartType properties). Property editors use separate backing files for separate properties. They already contain the generated helper prefix, so marker and frame lines map directly. Ordinary project `.js` files map to `project/relative/path.js` and account for the optional generated helper prefix. Files outside the project and generated private files without source metadata are ignored. Synchronization from Monaco back to Eclipse markers is not implemented.

At a new pause the native view opens or reveals the matching Eclipse file, annotates the stopped line with the standard current instruction pointer, and clears the annotation on resume, detach or disposal. Only the innermost frame drives this native annotation; frame selection in the shared panel still selects its own source and inspection scope.

The shared panel provides **this**, **Locals**, **Alls** (nearest binding wins across parent scopes), a selectable call stack, evaluation history and up to 20 watched expressions. Watches are reevaluated on every pause and frame selection, and cleared on detach. Results from stale pauses or frame selections are discarded. Watches execute administrator code through the existing bounded evaluation API, with the same fixed audit message and possible side effects; values and expressions remain in memory. Function entry and normal return pauses keep the function frame inspectable. Throwing exits use the exception setting instead.
