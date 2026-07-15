# Campaign protocol

## Prepare outside the fresh context

1. Record the Convertigo, Flow engine, Flow MCP and Svelte frontbuilder commit
   ids in a copy of `result-template.json`.
2. Confirm that the target project name and every previous RetailStoreFlow
   project are absent from the fresh agent's visible project list.
3. Bootstrap only the target project shell with Svelte enabled.
4. Copy the five declared fixture files into
   `resources/retailstore/` unchanged and verify their manifest hashes. Also
   mirror the five unchanged files under `libs/flow/resources/retailstore/`,
   because typed Flow resource blocks intentionally read from that project
   resource root. The mirror is fixture input, not an implementation.
5. Confirm that the target has no FullSync connector, design document,
   application Flow implementation or generated frontend artifact.
6. Confirm that the runtime FullSync store is reachable and persistent before
   the timed run. This is infrastructure readiness, not application setup.
7. Start a new agent context with only the fresh-agent prompt, target project
   name, fixture contract and supported Flow MCP/Playwright tools.

The preparation phase must not choose connector, design-document, view,
transaction, Flow or component names. It must not precompute CouchDB documents
or expose the legacy project.

## Capture during authoring

Preserve MCP request/response logs and timestamps. Count calls, failures and
corrective mutations. Save the first successful scaffold dry-run and the
second idempotent apply result. Record the first successful seed, sync, local
view, local get and useful browser render.

Schema learning is explicit. Preserve the `schemaPending` operation emitted by
`flow-app-progress` and the corresponding
`frontend-svelte-fullsync-schema` result. A copied or manually reconstructed
schema does not satisfy the campaign.

`flow-app-progress` reaching 100% is necessary but not sufficient. Every
binding warning must be resolved, and a data-bound iterator must contain a
visible child before browser validation starts.

## Validate the final application

Run the production generate/build actions and `svelte-check`. In Playwright,
capture one online synchronization and browse from the root to a product
detail. Disable browser network access without replacing the browser context,
then repeat one local view and one local get. Check desktop and 390 px layouts,
console errors and horizontal overflow.

Use a standalone Playwright script against the fixed production URL so browser
validation remains reproducible if an interactive browser transport closes.
Assert rendered category and product data, not only page availability or an
empty application shell.

Complete the result record, retain the Playwright script/screenshots and leave
the target project available for review. Do not compare with the legacy project
until the fresh-agent run and metrics are closed.
