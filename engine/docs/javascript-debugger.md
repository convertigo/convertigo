# JavaScript debugging

The `Backend Debug` panel of the web Studio and the Eclipse `Backend Debug` view use the same administrator API and Monaco component; they debug the server-side JavaScript, unlike `FrontEnd Debug`. The Eclipse view replaces the former Swing view of the same name and keeps its historical ID (`com.twinsoft.convertigo.eclipse.views.rhino.RhinoDebugView`) so saved layouts reopen the web implementation in the same slot. Workspaces that used the temporary web-view ID (`com.twinsoft.convertigo.eclipse.views.rhino.JavascriptDebugView`) in develop may retain a missing view tab; close that tab and open `Backend Debug`, or reset the perspective. In Eclipse, **Debug Eclipse launches** is checked by default when starting the debugger. Run from the sequence, connector, test case or Execute commands as usual. The Project / Sequence form is optional, under **Run a sequence from this view**.

## Enable and authorize

On a server, enable `javascript.debugger=true` explicitly, in the administrator configuration or with `-Dconvertigo.engine.javascript.debugger=true`. Debugging defaults to false on a server. Studio mode permits it. Generic participant debugging is available without the web Studio. Interpreted JavaScript source debugging additionally requires an editing-enabled Studio (`web_studio=enabled` for the web Studio); it is not offered on a production server without that Studio. All debugger operations require the `WEB_ADMIN` role, administrator authentication and normal XSRF controls. No ordinary request is selected implicitly.

On a server without an editing-enabled Studio, evaluation and watches accept scope paths only (for example `input.items[0].name`): no calls, operators, assignments or code. Paths read own data properties only; accessors, proxies, strings by index and host objects are shown as opaque. Free JavaScript evaluation is available in Studio mode or when `web_studio=enabled`; enabling it on a server lets administrators run arbitrary code in paused requests.

The effective mode is the more restrictive of the mode captured at the pause and the current server mode. Enabling the Studio during a server pause does not enable free evaluation for that pause. Disabling it restricts evaluation immediately and clears source breakpoints. Unknown configuration values mean server mode. Request parameters do not select this mode. Each attempt against a current paused frame writes fixed audit metadata (mode and frame type), without the expression, path, result, source or session token. Hovers only display inspected bindings.

The grammar is `root` followed by `.identifier` or `[index]` segments (at most 32 segments including the root), at most 4,096 characters. Roots are `input`, `arguments`, `output`, `local`, `result`. Identifiers use ASCII `[A-Za-z_$][A-Za-z0-9_$]{0,127}`; indices are `0` or `[1-9][0-9]{0,8}`. Indices apply only to JavaScript arrays and Java object arrays. Primitive arrays, sparse array holes, unknown host values and inherited properties are opaque; no accessor or conversion method is called. Quoted keys, whitespace, escapes and the configuration scopes are excluded.

## Protocol

POST form parameters to `admin/services/studio.debug.Debugger`. Responses contain `state`: `enabled`, `attached`, `sessionId`, `serial`, `pauseId`, `running`, `workers`, `stopped`, `breakOnExceptions`, `sources`, `frames` (innermost first), `notice`, `pauseTimeoutMillis`, and a bounded `events` history. Poll `state` once per second while attached. Events have a monotonic serial and a type, including `started`, `source`, `pause`, `exception`, `resumed`, `finished`, `detached`, `breakpoints`, `settings`, `breakRequested`, `autoResumed`, `breakpointsRemapped`, `unsupportedRuntime`.

| action | additional parameters | response |
|---|---|---|
| `start`, `state`, `stop`, `pause` | none | state |
| `attach` | opaque `target` from state.targets; same administrator HTTP session, no source breakpoints | state |
| `studioRequests` | `value=true/false`, Studio mode only; one administrator owns the Eclipse launch bridge | state |
| `breakOnEnter`, `breakOnReturn` | `value=true/false`; JavaScript functions only | state |
| `breakOnExceptions` | `value=true/false` | state |
| `breakpoint` | `url`, `line` (one-based), `set=true/false`, `kind=participant` for logical locations (default: Rhino source) | accepted, breakpoints |
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

Nested requestables retain their root binding. Subsequences on separate workers and parallel steps join through explicit scheduling boundaries; no inheritable thread state is installed. Each worker has its own stack. Source-debugging workers use an interpreted context; participant-only workers keep their ordinary compiled context. A requestable called on its own worker by a source-debugged request, such as a sub-sequence, gets an interpreted context too: its breakpoints stop it. At most one worker of a root request pauses at a time; siblings continue. Step commands follow the selected worker. A session retains at most 64 workers.

`javascript.debugger.pause_timeout` sets the maximum duration of a pause in seconds: default 120, bounded to 1..3600, captured when the session starts. Polling does not extend this deadline. A pause automatically resumes with an `autoResumed` event and visible notice. A session also expires after 60 seconds without an administrator operation. Stopping, disabling, logging out, or destroying the HTTP session releases pauses and clears ownership. Detaching does not cancel the business request; pending child work continues without debugging.

Only selected requests with source breakpoints in an editing-enabled Studio enter private Rhino factories. No global listener or cache invalidation is installed. Ordinary requests retain their compiled scripts, including when another request is paused.

## Inspection and source changes

Inspection returns shallow property rows with name, preview, type, an opaque reference and expansion availability. Initial participant scopes contain at most 20 rows and property pages at most 100. One command shares a budget of 1,000 examined candidates and 50 ms monotone elapsed time across all scopes, skipped/non-enumerable/duplicate keys, previews and controlled response serialization. Exhaustion returns a partial page and its reason, without calculating an exact total. CPU time is diagnostic only; an unavailable measurement is `null`, never zero.

Only indexed arrays and qualified own-data descriptors of the bundled Rhino 1.9.2 objects/call frames are traversed. Accessors, unknown wrappers, Java public fields, generic Java Maps/collections, business iterators, streams and result sets are opaque. No arbitrary getter or `toString` is invoked by these adapters. Inspection is a non-atomic live view: other workers continue. Concurrent structural mutation can invalidate its continuation. The adapter fails closed if the bundled Rhino layout is not recognized.

References identify values; cursors identify independent traversals, including two views of the same value. The existing `nextOffset` field now carries an **opaque single-use cursor**, not an index: pass it unchanged with the same reference, or send it as `cursor` on the `properties` action. Zero starts a new traversal. Arbitrary offsets and stale/replayed page tokens return `reason: "expired-cursor"`; no preceding entries are rescanned. A candidate crossing the elapsed-time limit is retained for the following page. Cursors and references belong to the paused worker and pauseId and are invalidated by resume, Stop, cancellation or auto-resume.

Limits: eight scope levels, 4,096 value references, 256 live continuations and 262,144 retained full-string characters per pause. Names and previews display at most 500 characters. Scalar strings over 65,536 characters, exceptionally large numbers and property names over 65,536 characters are limited rather than converted in full. Smaller full values remain available by reference. Display nesting is bounded to 32 levels. Lexical merging has bounded duplicate bookkeeping. Source/frame metadata retains the exact internal breakpoint identity; only its outward presentation is masked.

Output masking replaces the exact values of protected global symbols and the values of sensitive keys. It does not cover values that the application derived from them (encoded, hashed, split or truncated) nor credentials that are not global symbols, such as tokens obtained at runtime or values read from requests, databases or files. Path-only evaluation and masking reduce accidental disclosure in displays, logs and screenshots; they are not a boundary against administrators, who can already deploy and run code.

The 3-second inspection future bounds the caller's wait, not an already executing worker callback. The 500 ms interpreted-instruction marker does not preempt compiled participant code or a blocking Java call. The traversal time budget is cooperative: checked before/after qualified units and after callbacks. A trusted participant must use `scope`/`previewPath` and return bounded text; a blocking callback remains responsible for its termination. Stop/cancellation immediately expire references and pending caller results, but cannot force arbitrary callback code to terminate.

Breakpoints survive between executions in an attached session. When a source URL is compiled with changed text, unchanged prefixes/suffixes and unique surviving lines remap its breakpoints before execution. Deleted, ambiguous or nonexecutable lines are cleared with a visible notice. Unsaved edits do not change a running script. Refreshing/rerunning after applying an edit loads the new positions; a renamed source is a new source. Detaching clears the session's sources and breakpoints.

Other limits: 16 sessions, 64 sources, 262144 characters per source, 2097152 source characters per session, 64 event records, 256 breakpoints per source, and 4096 characters per evaluation. Evaluation has a 500 ms JavaScript instruction deadline. Blocking native/Java calls cannot be forcibly terminated.

## Covered and refused execution

JavaScript steps, step expressions, transaction handlers, `include()` and `use()` execute interpreted code for selected requests. Included files are named by project/path. `use()` evaluates private functions for debugging without reading or publishing shared cached functions; ordinary cache identity is preserved.

Provider calls run with the request debugger temporarily detached before accessing prepared runtimes or shared compiled modules. Providers can opt into private block implementations with `JavascriptDebugger.providerDebugging()`, `compileProviderImplementation(source, projectRelativeUrl, scope)` and `callProviderFunction(function, receiver, arguments, scope)`. A selected request gets interpreted implementations with debug information, compiled directly without consulting or publishing shared script/bytecode caches. Initialization runs without pauses. Only calls of these private functions reattach the request debugger; inspection stops before the supplied provider scope. Providers must create and retain these instances in invocation-owned state, after shared preparation has completed. Prepared-runtime builders suppress attachment, including nested callbacks.

With an ordinary prepared provider, logical debugging checks a live indicator inside ordinary prepared runners. A selected block-only request retains the compiled runtime, shared plan, prepared implementations, caches and library state. Rhino blocks are crossed as blocks, without source-line stepping. The detached boundary reads only the cached live signal; no debugger callback or frame is constructed. The administrative request registry is separate, bounded per request, and enables attachment after work has started.

Source breakpoints select the second level, only in an editing-enabled Studio. Invocation-owned Rhino adapters compile lazily, with real source URLs and line numbers. The current prototype still creates a complete private provider realm for every level-two invocation, including local block source breakpoints; libraries and dependent modules are reevaluated with fresh state. The panel warns about this state change. Reducing isolation for local block sources to their private adapters remains provider work. Orchestration stays compiled/detached, shared construction cannot pause, and private interpreted code is never published in shared caches. Legacy frames and context settings are restored on return or failure.

## Generic participants and late attachment

`JavascriptDebugger.signal` is a cached object whose volatile `active` field is read by ordinary runners. When active, the provider can call `participantSelected()` at an execution boundary, then `participant(kind, inspect)`. Participants own opaque frame descriptors and inspection callbacks. Participant metadata fields carry identifiers and labels only; business values must be returned through scopes or data fields. `enter(frameId, descriptorJson)`, `at(frameId, phase)` and `leave(frameId)` share the existing pause queue, stepping depth and pause generation. A participant-only worker uses the existing compiled context: no interpreted factory or Rhino debugger attachment. The construction barrier applies even without a Rhino binding, including to previously registered participants.

`state` additionally exposes `rhinoAvailable`, `rhinoRequested`, and `targets` containing opaque request IDs, requestable labels and selection status. When the debugger capability is enabled, requests in an authenticated WEB_ADMIN HTTP session are registered even before Start. The owning administrator can attach to one already running request; the next block boundary observes the selection. Attachment reserves its worker without executing code on the administrator thread. Requests belonging to another HTTP session, completed targets, concurrent selections and sessions with source breakpoints are refused. This is not a general discovery facility for production business sessions. No request values or cookies are included in the target list.

Late attachment cannot convert already running compiled code to interpreted code. A source breakpoint added during a level-one request affects a subsequent execution. On a server without an editing-enabled Studio, source breakpoint installation and Rhino pause settings are refused by the administrator service; only participant stepping is available.

Participant scope groups share the bounded property adapters, references and pagination used by ordinary scope inspection. In server mode, the Java entry point parses a path into an immutable token; only a participant declaring `pathEvaluation()` receives the distinct `path` action. Its callback selects the token’s root and calls `previewParsedPath(rootValue, token)`. No raw expression is supplied, and an older participant without this capability is refused rather than sent an `evaluate` action. Studio retains the participant’s historical `evaluate` action. Each participant may accept a narrower contract, such as dotted paths only; Studio mode does not broaden a provider’s evaluator. Compiled plugin callbacks are trusted and have no interpreted instruction watchdog; providers must bound their work, avoid accessors and arbitrary evaluation, and not mistake the JSON response-size bound for an execution-time bound. Explicit parent links retain the logical stack across child and parallel workers.

No debugger payload is written to generic administration response logs. Sources and values stay in memory, not local storage. Tabs sharing an HTTP session share its debugger; isolation is by administrator HTTP session, not browser tab. Business request timeouts count execution time only. A debugger pause suspends the existing requestable deadline, for sequences and transactions and their waiting callers. On resume, the unspent execution budget remains; actual execution can still time out. The separate maximum pause duration is wall-clock time and remains effective, even while polling.

## Eclipse launch bridge and editor markers

Only `ConvertigoPlugin.runRequestable` marks local `InternalHttpServletRequest` instances with an in-process session object. The administrator API action `studioRequests` explicitly enables the bridge for the owning session, and refuses servers and competing owners. InternalRequester alone, a session named "studio", and an HTTP request without the administrator's existing marker cannot select it. Disabling the checkbox, stopping, expiry and logout remove the bridge; disposing the Eclipse view releases the bound session. An already selected business request continues when detached. A second root request is refused with a visible `requestRejected` notice; nested workers keep the existing root lease.

Before a conventional Eclipse launch, the view copies enabled line breakpoints from Eclipse's breakpoint manager (including its global enable switch). A 250 ms SWT timer follows edits, removals and pause state without evaluating code or touching session lifetime. The exact engine URL is stored on each property editor file: short qualified object name plus the API's property script name (`expression`, `LogStep`, `message`, transaction name for `handlers`, `ctxName`, or `property-smartType` for JavaScript SmartType properties). Property editors use separate backing files for separate properties. They already contain the generated helper prefix, so marker and frame lines map directly. Ordinary project `.js` files map to `project/relative/path.js` and account for the optional generated helper prefix. Files outside the project and generated private files without source metadata are ignored. Synchronization from Monaco back to Eclipse markers is not implemented.

At a new pause the native view opens or reveals the matching Eclipse file, annotates the stopped line with the standard current instruction pointer, and clears the annotation on resume, detach or disposal. The embedded page sends a selected frame index, session ID and pause ID through its main-frame Java bridge. Java validates them against the live session and reads the source and line from its own stack before opening the Eclipse editor. Selecting a caller moves the native annotation to that frame. No arbitrary file path or line is accepted from the page. A property editor that has never been opened can be created from the recorded object/property metadata.

The shared panel provides **this**, **Locals**, **All scopes** (expandable lexical and parent scopes), a selectable call stack, evaluation history and up to 20 watched expressions. Watches are reevaluated on every pause and frame selection, and cleared on detach. Results from stale pauses or frame selections are discarded. Watches use the same mode restriction and audit metadata as evaluation; values and expressions remain in memory. Function entry and normal return pauses keep the function frame inspectable. Throwing exits use the exception setting instead.


## Debugger presentation and the main editors

The Eclipse view contains controls, stack, property table, evaluation and watches. It has no source editor, script list or request launch form; launches come from Eclipse editors. Admin Skeleton switches and grouped Studio icon buttons share the admin themes. A Ready / Running / Paused / Detached badge remains visible, with an inspection placeholder while execution is between pauses.

In the `Backend Debug` dock of the Web Studio, selecting a paused frame opens the existing property or project-file tab in `StudioEditorPanel`. That Monaco shows the selected frame line, debugger breakpoint margin and bounded binding previews. Existing unsaved content is preserved. Scripts without a resolvable editable object use a read-only runtime snapshot in the same central editor. The independent `/javascript-debugger` administration page retains its source selector/editor and optional request form for use outside either Studio.

Evaluation history and request-form status are reset at each new pause. Late evaluation, watch, source and property-page responses cannot replace a newer pause/frame selection. Stopping explicitly detaches and lets the paused business request finish. The legacy Swing view and its engine attachment mode are removed.

The session snapshot exposes `breakpoints` as an object keyed by the existing opaque source URL, with
line arrays as values. Tree and graph buttons read this same server-owned map, including changes made
in another view. The graph carries each tree item's `debugTarget` unchanged through hydration and
layout; matching includes its source URI, revision and JSON Pointer. The selected frame has an amber
arrow and left border in both views, independently of selection. Resume, automatic resume and Stop
remove pause markers. A new pause or selected frame reveals its graph node, expanding its ancestors;
regular polling does not recenter the viewport. Selecting a graph block reveals it in the tree, and a
tree selection selects and reveals the corresponding graph node.

The common web/Eclipse panel labels the **current request** and the **next request** separately:
level 1 means logical blocks; level 2 also enables private Rhino sources and fresh library state.
`currentLevel` is captured by the existing worker's execution mode (null without active work), while
`nextLevel` reflects the current source breakpoints and server capability. Adding a source breakpoint
during level 1 prepares the next execution, without retrofitting the current worker. Removing the last
source breakpoint during level 2 likewise changes only the next request. The panel keeps the fresh
state warning visible for an active level 2 request even when the next request is level 1. The Eclipse
frame-opening bridge continues to validate the live session, pause and frame on the server.
