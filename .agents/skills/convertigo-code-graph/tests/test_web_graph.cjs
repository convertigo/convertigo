const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { extract } = require("../scripts/web_graph.cjs");

const temporary = fs.mkdtempSync(
  path.join(os.tmpdir(), "convertigo-web-graph-test-"),
);
const dependencyRoot = path.resolve(process.argv[2] ?? process.cwd());
const base = "convertigo-studio-web/src/lib/";
const sources = {
  [base + "utils/service.js"]:
    `export async function call(service) {let url=getUrl()+service;return fetch(url)} export function getUrl(path='admin/services/') {return path}`,
  [base + "common/ServiceHelper.svelte.js"]:
    `import {call} from '../utils/service';export default function ({service}) {call(service)}`,
  [base + "wrapper.js"]: `import {call as rpc} from '$lib/utils/service';
    function categories(){return rpc('studio.palette.Get')}
    export function palette(id, load=categories){return load(id)}
    export function rename(){return rpc('studio.dbo.Rename')}
    export function remove(){return rpc('studio.dbo.Remove')}`,
  [base + "State.svelte.js"]:
    `import Helper from './common/ServiceHelper.svelte.js';import {call} from './utils/service';
    function run(action){return call(\`roles.\${action}\`)}
    let values={add(){return run('Add')},remove(){return run('Delete')}, unknown(action){return run(action)}};
    export default Helper({values,service:'roles.List'});`,
  [base + "StateView.svelte"]:
    `<script>import State from './State.svelte.js';let {remove}=$derived(State);</script><button onclick={()=>State.add()}>Add</button><button onclick={()=>remove()}>Remove</button>`,
  [base + "Test.svelte"]: `<script lang="ts">
    import {call as rpc, getUrl} from '$lib/utils/service';
    import {palette, rename} from './wrapper';
    import Helper from './common/ServiceHelper.svelte.js';
    const endpoint: string = 'studio.dbo.ImportCopybook';
    async function submit(){await rpc(endpoint);palette('id');rename()}
    function shadow(rpc){rpc('false.Positive')}
    function dynamic(service){rpc(service)}
    function mutate(){let endpoint='false.Mutable';endpoint=other;rpc(endpoint)}
    Helper({service:'roles.List'});
    const url = \`\${getUrl()}studio.dbo.GetIcon?iconPath=\${icon}\`;
    const socket = \`\${getUrl()}studio.ngxbuilder.WsBuilder\`;
    </script>
    {#each items as rpc}{rpc('false.TemplateShadow')}{/each}
    <button onclick={submit}>Submit</button>
    <button onclick={() => rpc('studio.Inline')}>Inline</button>
    <button onclick={(rpc) => rpc('false.InlineShadow')}>Shadow</button>`,
  [base + "Broken.svelte"]: `<script>const x = </script>`,
  [base + "Export.js"]:
    `function local(){return 1};export {local as renamed};export default local;`,
  [base + "Alias.ts"]:
    `import {renamed as imported} from './Export'; export function use(){return imported()}`,
  [base + "Template.svelte"]:
    `<script>import {call} from '$lib/utils/service';</script>{call('studio.Template')}`,
};
try {
  for (const [name, source] of Object.entries(sources)) {
    fs.mkdirSync(path.dirname(path.join(temporary, name)), { recursive: true });
    fs.writeFileSync(path.join(temporary, name), source);
  }
  const graph = extract(temporary, dependencyRoot, Object.keys(sources));
  assert.equal(graph.parse_errors, 1);
  const services = graph.requests.flatMap((r) => r.services ?? []);
  assert(services.includes("studio.dbo.ImportCopybook"));
  assert(services.includes("roles.List"));
  assert(services.includes("studio.Template"));
  assert(services.includes("studio.dbo.GetIcon"));
  assert(services.includes("studio.ngxbuilder.WsBuilder"));
  assert(services.includes("studio.Inline"));
  assert(services.includes("roles.Add"));
  const add = graph.nodes.find(
    (n) => n.source_file.endsWith("State.svelte.js") && n.label === "add",
  );
  const remove = graph.nodes.find(
    (n) => n.source_file.endsWith("State.svelte.js") && n.label === "remove",
  );
  assert(
    graph.requests.some(
      (r) =>
        r.owner === add.id &&
        r.services?.includes("roles.Add") &&
        r.confidence === "INFERRED",
    ),
  );
  assert(
    !graph.requests.some(
      (r) => r.owner === remove.id && r.services?.includes("roles.Add"),
    ),
  );
  assert(
    graph.edges.some(
      (e) =>
        e.target === add.id &&
        e.source.includes("StateView.svelte") &&
        e.relation === "calls",
    ),
  );
  assert(
    graph.edges.some(
      (e) =>
        e.target === remove.id &&
        e.source.includes("StateView.svelte") &&
        e.relation === "calls",
    ),
  );
  assert(
    graph.requests.some(
      (r) =>
        r.owner.endsWith("StateView.svelte") &&
        r.services?.includes("roles.List") &&
        r.configuration_source,
    ),
  );
  assert(!services.some((s) => s.startsWith("false.")));
  assert(
    graph.requests.some(
      (r) => r.services === null && r.expression === "service",
    ),
  );
  assert(graph.edges.some((e) => e.relation === "default_callback"));
  const byId = new Map(graph.nodes.map((n) => [n.id, n]));
  assert(
    graph.edges.some(
      (e) =>
        e.relation === "calls" &&
        byId.get(e.source)?.label === "use" &&
        byId.get(e.target)?.label === "local",
    ),
  );
  const copybook = graph.requests.find((r) =>
    r.services?.includes("studio.dbo.ImportCopybook"),
  );
  assert.equal(copybook.source_location, "L6");
  assert(!graph.nodes.some((n) => n.source_file.endsWith("Test.svelte.js")));
  const invalid = extract(
    temporary,
    dependencyRoot,
    Object.keys(sources).filter((n) => n !== base + "utils/service.js"),
  );
  assert.equal(invalid.requests.length, 0);
  console.log(
    "Web AST fixtures passed: aliases, lexical shadows, mutable/dynamic names, defaults, config, resources, lines, parse failure.",
  );
} finally {
  fs.rmSync(temporary, { recursive: true, force: true });
}
