# Participant transport v2: explicit child activations

The additive `JavascriptDebugger.participantContract()` contract advertises
`explicitParents`, `workerStepping`, `singlePause`, `cancellation` and
`activationMetadata`. Existing v1 plugins and source breakpoints remain valid.
A provider must probe optional primitives once, outside its ordinary block path.

`captureParent()` creates a server-only continuation on the calling thread.
`bindChild(link)` binds it for the child lifetime, with no inherited thread-local
or extra public attachment token. Requestable workers and pooled parallel lanes
pass this link before scheduling. Binding changes neither compilation mode nor
Rhino factory. A construction barrier is carried explicitly and suppresses child
participants. Closing a lease removes all state from a pooled thread. A missing
or detached parent cannot create a new independent debug root.

Each frame has an opaque activation ID and explicit parent ID, root request,
worker, invocation, depth and iteration ordinal. Source identities remain owned
by the provider. A child stack includes its captured caller ancestry. Each
worker retains its own active frames; there is one paused worker per session.
Sibling breakpoints are passed while another worker is paused, without queuing
an extra pause. Into follows descendants; Over skips them unless an explicit
breakpoint wins; Out follows the returning child back to its caller. A completed
child exposes a `return` boundary in its caller for Into/Out, but not Over.

Only the paused worker executes inspection callbacks. Ancestor frames advertise
`inspectable:false`, and the contract declares `ancestorScopes:false`: callers
may still be running/settling siblings, so their live scopes cannot be safely
read from the child's thread. Scope inspection of these frames is refused before
queueing work. Their source/activation metadata remains available in the stack.

Stop releases the pause and detaches, without cancelling business work. Business
cancellation is checked while paused and again on the safe participant boundary;
it releases the pause and propagates cancellation instead of running the block.
It does not close the administrator's debug session. Interruption has the same
release behavior. No inspection callback or block is invoked under a preparation
lock.

The invocation guard retains the original requestable context and observes its
existing volatile removal request (`ContextManager.requestAbort`), as well as
the worker/sequence cancellation state. Normal end-of-context disposal after a
response is not cancellation. Mutable cleanup fields are not the cancellation
owner.

The existing requestable deadline is the sole execution budget. Synchronous child
pauses suspend the waiting caller chain. A selected parallel loop supplies an
explicit wait-domain budget to `bindChildWithBudget`, separate from parentage.
The common budget keeps running while a sibling task executes or waits for
ordinary I/O. It freezes only while the caller polls for an outcome, all active
tasks are debugger-paused, no ordinary task is queued and no outcome is ready.
Completing a sibling therefore freezes only the remaining budget. Poll return,
resume and cancellation release the lease. The task/interrupt lock is preserved.
No new watchdog, thread model or Rhino context sharing is introduced.

Suspension leases are idempotent and credit the union of overlapping pauses;
admission never revives an expired budget. Invocation-owned abort notifications
wake the pause after Sequence/Transaction publish their business flags. The
250ms bounded poll remains a fallback. A published cancellation wins over Stop
or pause expiry, before business execution and result publication. Completed
invocation guards cannot credit a subsequent run of the same requestable.

The wait-domain is local to one structured ParallelLanes loop. A nested loop
started by a lane without a bound requestable gets no automatic common-budget
credit; detached and first-result waits are not represented by this API. These
rare domains need an explicit waiting relation before any wider credit is safe.
The pool stays bounded, each worker owns its context, and no inheritable thread
local, virtual thread, ScopedValue or preview API is added. Each pause still
resumes at its configured bound (120s by default). The 4 November engine-owner
advice sections 1, 2 and 5 is applied; inspection/benchmark advice belongs to D4.
