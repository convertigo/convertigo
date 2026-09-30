---
name: convertigo-code-graph
description: Build, refresh, or query an optional local Graphify index of Convertigo Java and Studio web code for cross-file relationship searches. Use for local code-graph requests, not ordinary targeted searches or mandatory build/maintenance steps.
---

# Convertigo Local Code Graph

The recipe operates on the current checkout, on develop or hotfix. Only the
recipe is shared: graphs, HTML, provenance, and extraction caches remain local.
No Gradle/build/commit hook depends on this index. Do not commit or upload it.

## Check Or Generate

From the checkout root, check freshness without installing Graphify:

```sh
python3 .agents/skills/convertigo-code-graph/scripts/code_graph.py status
```

Exit 0 means the selected source contents and alias configuration match the
index and the extraction recipe match. Exit 1 means absent or stale; exit 2 means the check failed. Provenance
includes a content fingerprint, so uncommitted changes and branch switches are
handled without assuming that HEAD alone proves freshness.

When an index would materially help the task, generate or refresh it:

```sh
uv run --with graphifyy==0.9.51 python .agents/skills/convertigo-code-graph/scripts/code_graph.py build
```

The driver needs Git, Python 3.10+, Node.js, and the already-installed Studio web
dependencies (`svelte/compiler`, `@babel/parser`, `@babel/traverse`, `@babel/types`).
It resolves these from the checkout's `convertigo-studio-web/package.json`, records
parser versions, and fails without publishing if they are unavailable. Do not
install or upgrade application dependencies merely to generate a graph.
`uv` supplies the pinned Graphify package;
its first use may download dependencies. `--repo <path>` selects another checkout.
Generation takes a fresh snapshot of tracked Java files and supported Studio web
JS/TS/Svelte/JSON files, including working-tree edits. It excludes documents,
images, dependencies, and untracked source files. It never edits source files.

Java uses Graphify's AST extractor. Svelte uses the real Svelte compiler parser,
including templates; script and template expressions use Babel's lexical scopes.
Relative imports and the repository's standard `$lib` alias are resolved against
selected tracked files. Generated SvelteKit alias configuration is included in
the freshness fingerprint when present, but is not required for `$lib` resolution.
Other aliases, re-exports, dynamic imports, and arbitrary object dispatch are not
fully resolved. Parser failures and unresolved local imports are recorded.

Outputs are in `build/graphify/graphify-out/`: `graph.json`, `GRAPH_REPORT.md`,
`overview.html`, `graph.html`, `services.html`, `service-map.json`, `health.json`,
`index-state.json`, and local label sidecars. A failed
run leaves the previous published index intact. Temporary snapshots/caches are
removed after the run. Keep per-worktree indexes separate.

Use a **full rebuild**, not Graphify's incremental update/watch mode: the local
0.9.51 benchmark lost external import placeholders on an unchanged incremental
pass. A cold rebuild of the measured corpus took about 48 seconds, before this
recipe's additional diagnostics and naming. Re-evaluate this limitation before
changing that policy or the pinned version.

## Agent Queries

Prefer these directed, bounded queries for Studio web/admin Java relationships.
They need only standard Python after generation and reject stale indexes:

```sh
python3 .agents/skills/convertigo-code-graph/scripts/code_graph.py services StudioCopybookDialog.svelte
python3 .agents/skills/convertigo-code-graph/scripts/code_graph.py services StudioPalettePanel.svelte
python3 .agents/skills/convertigo-code-graph/scripts/code_graph.py impact studio.dbo.ImportCopybook
```

`services` accepts an exact component/module filename, repository-relative path,
function label, or ID. Ambiguous labels return candidates rather than choosing a
random match. `impact` accepts an exact Java admin service name. Use `--json` for
structured results and `--limit 1..200` (default 30) to bound results. Function IDs
returned by an ambiguity error can narrow a large module query.

Results include symbol chains, source paths/lines, relation types, confidence,
Java handler classes/entry points, and reachable unresolved expressions. Imports
alone never imply calls to all functions/services in a module. Root component
queries include their local functions; imported functions follow only calls and
their nested callbacks/default candidates. Child components are separate roots
(`renders` edges are available in the graph but not followed by service queries).
Focused queries stop at the verified shared RPC transport, so its infrastructure
authentication callbacks do not pollute every component's service list. The full
graph retains those structural links.

Admin bridges verify the current `getUrl`/`call` protocol and Java `AdminServlet`
dispatch pattern, then match the actual package/class and `ServiceDefinition`.
`calls_http`, `configures_service`, `references_resource`, and `handled_by` are
distinct. Resource URLs, including a WebSocket URL, are references, not evidence
of an executed request. `ServiceHelper` configurations/imported configured state,
object members, callback defaults, and one-boundary local argument substitution
are explicitly `INFERRED` potential relations. Dynamic expressions are retained;
never invent a handler or claim a complete runtime call graph.
The object-member rule also recognizes static destructuring through unshadowed
Svelte `$derived(state)`; arbitrary reactive computations are not evaluated.

For Java structural navigation, the general Graphify query is also available:

Use an existing fresh index when it helps; inspect source files to verify results:

```sh
GRAPHIFY_NO_AUTO_REFRESH=1 GRAPHIFY_QUERY_LOG_DISABLE=1 uv run --with graphifyy==0.9.51 graphify query "FlowStudioSupport" --graph build/graphify/graphify-out/graph.json --budget 1500
```

The flags prevent global skill refreshes and query-history writes. Build/query
processing is local and code-only; no semantic LLM extraction or naming backend
is invoked. Do not add a model provider or index private notes without an explicit
request. Treat queries as navigation, not proof of runtime behavior.

## Inspect Visually

Open **`services.html` first**. It presents real typed symbols and source-bearing
relations for a selected component/module or reverse service impact, with a
searchable selector, node/edge inspection, Java entry points, and unresolved
expressions. It caps the detailed traversal at 160 nodes rather than replacing
symbols with empty community nodes. A dashed edge denotes an inferred candidate.
This view is derived from `service-map.json`, which preserves separate call sites
and provenance even when Graphify's simple graph collapses parallel edges.

`overview.html` is a smaller view grouped by package/directory
families; it omits nodes without a project source path. These groups come from
source layout, not an inferred architecture. Edges aggregate the same structural
relations as the graph; they are not runtime traffic or Maven/Gradle dependencies.

Use `graph.html` for the denser community overview and `GRAPH_REPORT.md` for member
symbols and source references. Names combine the dominant package/directory and
Graphify's structural hub, such as `beans.ngx.components / IonIcon`.
They are indicative, not curated architectural boundaries. Above 5,000 symbols,
the HTML aggregates communities rather than displaying every symbol; use queries
or the report for detail. Labels are not generated by an LLM. The HTML renderer
loads vis-network from a CDN; offline viewing may require caching that asset.

Known limits: external/JDK imports can have absent endpoints; inferred edges need
verification. The web layer does not perform general interprocedural value/type
analysis, arbitrary mutable object dispatch, or project/sequence `.json` routing.
Local argument substitution follows one call boundary only and keeps unresolved
generic wrappers visible. Template-local each/snippet/await/let/const bindings
are conservatively blocked when their values cannot be resolved. These may
cause omissions, not fabricated service edges. Inspect warnings and metrics in
`service-map.json` and `index-state.json`; zero component parse errors does not
prove complete semantic coverage. An index should be refreshed after
significant relevant changes when it is needed, not automatically after every
edit.

## Recipe Validation

```sh
python3 -m unittest discover -s .agents/skills/convertigo-code-graph/tests -v
node .agents/skills/convertigo-code-graph/tests/test_web_graph.cjs
uv run --with graphifyy==0.9.51 python -m unittest discover -s .agents/skills/convertigo-code-graph/tests -v
python3 .agents/skills/convertigo-code-graph/tests/verify_index.py
node .agents/skills/convertigo-code-graph/tests/verify_view.cjs
```

The optional view check needs an already-installed Playwright/browser runtime;
do not make graph generation depend on downloading a browser.

After changing the Graphify pin or API integration, also run a real local build,
check `status`, inspect HTML views and the health report, and verify the real
copybook/palette chains, a wrapper-only query, a reverse impact query, and an
unresolved dynamic call against source. Generated output
is disposable; no source commit needs reverting to remove a local index.
