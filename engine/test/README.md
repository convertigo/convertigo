# Engine regression tests

Test source sets and verification tasks are configured in `engine/tests.gradle`,
included by `engine/build.gradle`. Run `./gradlew :engine:check` for the regression
suites, or select an individual task below. `:engine:soapuiSmokeTest` is a separate
opt-in smoke test.

## NGX application build output

`./gradlew :engine:ngxBuildSettingsTest` checks that redundant asset self-copies
inside the owning project's preserved `DisplayObjects/mobile/assets` are skipped.
It covers normalized paths, shared assets copied into a different project,
different output locations, old/new template output formats, and symbolic links.
The separate dev-server asset list remains unchanged (assets are served in memory).

Without compiling or launching Eclipse, these commands compile and exercise the
production progress/parser classes and build Job, using real npm processes and
stubbing only the Eclipse host:

```sh
node eclipse-plugin-studio/tests/ngx-build.test.cjs
node eclipse-plugin-studio/tests/ngx-build-job.test.cjs
```

They cover failure after bundle generation, npm post-build failures, successful
completion after lifecycle hooks, fatal watch errors that leave Node alive,
cancellation, cleanup, and bounded diagnostic history.

For an opt-in test with real Angular builds, pass an existing NGX `node_modules`:

```sh
node eclipse-plugin-studio/tests/ngx-angular-build.cjs /path/to/project/_private/ionic/node_modules
```

This creates and removes only a temporary fixture, linking existing dependencies
read-only. It reproduces late self-copy failures, verifies fast/prod outputs,
cross-project copies, and watch recovery after a compile error. The config uses
the independently tested asset predicate's expected result; this does not launch
the Studio GUI or exercise project generation end to end.

## Step sources

`./gradlew :engine:stepSourceTest` checks aggregation of classic and SmartType
sources, PDF step broken-source labels, updates after repairing a source, and
empty or malformed definitions. It runs without starting an engine or Studio.

## Studio object search

`./gradlew :engine:studioSearchTest` exercises the web search service and the
filters shared with Eclipse: type/filter-only searches, AND combinations,
symbols, classic/PDF/variable/multiple/additional step sources, text and regex,
project scope, unavailable workspace projects and result limits. It uses
in-memory projects without a server or Eclipse property editor classes.

## FullSync view reduce functions

`./gradlew :engine:designDocumentReduceTest` checks the count template, including
partial and nested partial counts, exact recognition of the old template despite
formatting differences, preservation of custom functions and malformed view data.

An optional integration case uses a disposable CouchDB server and creates/deletes
only a uniquely named test database. Start a dedicated container with
`COUCHDB_USER=admin` and `COUCHDB_PASSWORD=issue-1223-test`, then pass
`-PcouchDbReduceTestUrl=http://127.0.0.1:<test-port>` to the task. The case compares
the old template, the new template and `_count` on 6,000 documents, with and without
grouping. Never point it at a production database server.

The provider's own tests exercise the PouchDB patch separately, including
unchanged built-in reducers and patch idempotence; see its `test/README.md`.

## FullSync replication access

`./gradlew :engine:fullSyncReplicationPolicyTest` checks the public endpoint policy
for `allow`, `pullOnly` and `deny`, including POST reads, writable replication
checkpoints, refusal of document writes, and independent aggregation of access and
anonymous restrictions for shared databases. It uses the production request parser
and policy without starting CouchDB. A real client replication and checkpoint
resume still require runtime validation.

## MDC lifecycle

`./gradlew :engine:mdcLifecycleTest` runs the logging regression suite, also
included in `:engine:check`. It uses the production Log4j bridge, layout and
`nolog` filter to check rendered log fields, cleanup of reused workers, delayed
executor tasks, requestable threads, context snapshots, nested requester errors
(including cleanup failures), HTTP filter boundaries, `nolog` initialization before
alias forwarding, and device log environments.
The tests run in their own JVM with in-memory fixtures; they do not start a server
or contact SMTP, Redis or other external services.

## Project archive cleanup

`./gradlew :engine:projectArchiveTest` runs the archive regression checks, also
included in `:engine:check`. It uses disposable temporary directories and ZIP
fixtures. It covers internal YAML/XML project names, versioned filenames,
successful, skipped and failed deployments, and preservation of ZIP backups and
external archives. The deployment/extraction code is real; the project importer
is stubbed, so this is not an end-to-end server/Studio validation.

## Workspace loading

`./gradlew :engine:workspaceLoadingTest` checks archive identification from entry
metadata (YAML, legacy XML, descriptor order, Unicode names and missing descriptors),
extraction into shared directories, buffered binary output, and project reference
discovery without reading included sequence files. It uses temporary fixtures and
does not start an engine.

## Optional default connector and transaction

`./gradlew :engine:optionalDefaultsTest` checks that a project without connector
answers the sequences of a same session through the servlet and internal
requesters without looking up a connector, the implicit choice of the default or
the only connector and transaction (also for an empty transaction step source),
the plain error of a request naming no requestable (it lists nothing of the
project), the removal of the defaults, the console moves and copies of a
default connector or transaction (not default in their new parent), the sober
errors of a private requestable or a missing secured connection, the error
document that keeps only the names the caller gave, the identical answers to an
unknown or a refused requestable for a caller who is not an administrator (the
administrators and the internal requests keep the precise errors), the
unchanged behavior of explicit defaults, and the test platform of the dashboard
that marks the explicit default connector and transaction only (never the only
connector or transaction, never a default transaction the caller may not see).
It uses in-memory projects and does not start an engine.

## Administration instance routing

`./gradlew :engine:adminInstanceRoutingTest` checks opaque administration
addresses, technical pod metadata and rejection of an address left by a pod
restarted under the same name. Routing over Redis, HTTP forwarding and SSE
require the Docker multi-instance validation.

## Request-scoped sessions

`./gradlew :engine:requestScopedSessionTest` checks the ephemeral sessions used by
terminating URL mapper operations and by requesters with `__removeSession` and
no valid HTTP session. The requester tests start a disposable embedded Tomcat:
they check cookies, session/context cleanup (including errors and end-hook
failures), GET/form/PUT/multipart parameters, invalid cookies, the timeout of an
existing session, independent concurrent calls and the session inherited by an
internal requester. Requestable execution and the two-slot license are fixtures;
no external server, session store or license file is used.

## Redis Sentinel

`./gradlew :engine:redisSentinelConfigurationTest` checks the default single-server
mode, Sentinel configuration validation, independent credentials, master-only
reads/subscriptions, TLS settings, reload equality and secret-free diagnostics.
It is offline and included in `:engine:check`. For opt-in Docker failover checks,
see [the disposable Sentinel fixture](redis-sentinel/README.md).

## Session serialization

Run the standalone suite (also included in `:engine:check`):

```sh
./gradlew :engine:sessionSerializationTest
```

No external service or test-framework dependency is required. The suite tests
the production codec and `RedisHttpSession.flush()` / a newly loaded session
against an in-memory `SessionStore`. It covers typed `Properties`, legacy JSON,
W3C DOM interfaces, namespaces, mixed content, XML signatures and XXE rejection.

`SessionSerializationTest --redis` runs the same session checks using
`RedisSessionStore`. Run this main class on the `test` runtime classpath
with `-Dconvertigo.engine.session.redis.host`,
`-Dconvertigo.engine.session.redis.port` and
`-Dconvertigo.engine.session.redis.prefix` pointing to an **isolated test Redis**.
It creates a uniquely named session and removes it afterwards. Never point this
test at production.

## Stored-value contract

- `Properties` values use the existing typed JSON envelope. Effective string
  defaults are flattened into the snapshot; the defaults chain is not retained.
- Ordinary untyped maps/lists and existing typed values remain readable. Old
  untyped JSON cannot reveal whether it originally came from `Properties`:
  recreate the session or write the attribute again after upgrading.
- DOM payloads store stable W3C interface names and XML, not parser-specific Java
  implementation classes. `Document` and `Element` take precedence over `Node`
  and `NodeList` when implementations expose multiple interfaces.
- `Node` covers attributes, text, CDATA, comments, processing instructions and
  document fragments. Node lists retain item order and each item's DOM type.
- Restored DOM values are detached snapshots, not references into the original
  tree. Node lists are not live queries; parent/document identity across list
  entries, user data, schema/ID annotations and parser implementation identity
  are not retained.
- XML is not pretty-printed. Namespace bindings (including inherited bindings
  of standalone elements) and significant whitespace are retained. The suite
  verifies an enveloped XML signature after a document round-trip.
- DTD/entity nodes and external entity resolution are deliberately rejected.
  Malformed/unsafe DOM values fail decoding rather than silently falling back
  to another Java type.
