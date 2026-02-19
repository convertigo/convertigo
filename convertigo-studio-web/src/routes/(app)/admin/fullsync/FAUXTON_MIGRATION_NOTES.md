# FullSync Admin Migration Notes

## Snapshot (2026-02-13)

- Legacy embedded Fauxton (current Convertigo sources):
  - Path: `/Users/nicolas/git/convertigo/eclipse-plugin-studio/tomcat/webapps/convertigo/admin_/_utils`
  - Release marker in `index.html`: `2020-02-20T00:04:51.699Z`
- Extracted latest Fauxton from Docker image:
  - Image: `couchdb:latest`
  - Digest: `sha256:c311385c44e9708952c3b9ada25eb538f2b5a57d0cf89bfd07f4a4dbf962697c`
  - Image creation: `2026-02-03T02:47:12.333360669Z`
  - Temporary extraction folder: `/tmp/fauxton-oRnAaP/fauxton`
  - CouchDB release found in container metadata: `3.5.1`

## UI reference sources used

- Fauxton upstream repository cloned for structure reference:
  - `https://github.com/apache/couchdb-fauxton`
- Main navigation reference:
  - `app/addons/fauxton/navigation/components/NavBar.js`
  - `app/addons/fauxton/assets/scss/_navigation.scss`
- Databases page reference:
  - `app/addons/databases/layout.js`
  - `app/addons/databases/components.js`
  - `app/addons/components/layouts.js`

## Why migrate to native Svelte

- Better visual integration with the new Convertigo admin (same cards/buttons/theme/dark mode).
- No iframe DOM patching required.
- Easier long-term maintenance and feature ownership inside `convertigo-studio-web`.
- Full control over role-specific actions and UX.

## Proxy compatibility checks

- Existing servlet endpoint: `/fullsync/` (proxied to CouchDB by `FullSyncServlet`).
- Admin role checks are enforced server-side for config actions.
- Existing security handling remains valid:
  - session + role checks
  - CORS handling
  - XSRF token headers
  - CouchDB path rewriting / prefix behavior

## Implemented MVP in this route

- Files added under `src/routes/(app)/admin/fullsync/`:
  - `FullSyncDatabasesPage.svelte`
  - `FullSyncDatabasePage.svelte`
  - `FullSyncDocumentPage.svelte`
  - `fullsync-route.js`
  - `fullsync-api.js`
  - `FAUXTON_MIGRATION_NOTES.md`
- `+page.svelte` now serves only the native Svelte implementation (no iframe mode in UI).
- Native mode now follows Fauxton functional organization while using Convertigo admin widgets/styles
  and route segments:
  - `/admin/fullsync/`:
    - databases list (no per-database sidebar)
  - `/admin/fullsync/{database}`:
    - per-database workspace
    - sidebar with `All Documents`, `Run A Query with Mango`, and design docs list
  - `/admin/fullsync/{database}/{docid}`:
    - document editor
    - URL includes document id
  - implemented features:
    - database list/filter/create/delete
    - database metadata
    - document list (pagination/filter)
    - document open/edit/save/delete (JSON)
    - document creation
    - Mango query execution (`_find`)

## Next incremental steps

1. Add advanced Fauxton equivalents only where needed (`_changes`, design docs tools, compaction).
2. Add domain-specific shortcuts for Convertigo FullSync workflows.
3. Add integration tests for critical CRUD paths against `/fullsync/`.
4. Add optional parity screens for `Active Tasks`, `Replication`, `Setup` if they are required in the new UX.
