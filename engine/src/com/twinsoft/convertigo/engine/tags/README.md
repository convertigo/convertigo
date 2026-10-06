# Studio tags

`TagManager` is the common editing domain for Eclipse and the web Studio. Tags organize instances without changing their database-object parent, name, invocation or permissions. The default explorer remains the normal technical tree; **Group by tags** is a local display preference.

## Sources and identities

* A project's `_c8oProject/tags.json` contains `schemaVersion: 1`, `tags`, `assignments` and `projectTags`.
* The real Convertigo workspace's `studio/tags.json` contains `schemaVersion: 1`, `tags` and `assignments`. Eclipse's `.metadata` and browser preferences do not store business memberships.
* Definition maps use UUID v4 keys. A label is presentation, so equal labels never merge identities. DBO tag IDs belong to their owning project; portable project-tag IDs survive import into another workspace.
* The explicit whitelist in `TagPolicy` allows projects (`workspaceProjects`) and, as project objects (`projectObjects`), sequences, transactions and the pages, shared components and shared actions of an NGX application. All the objects of a project share its tag list, so one tag such as "Payment" can mark the back-end and front-end objects of the same functional area. Steps, connectors, variables, UI components and other DBO types are excluded. Adding another type requires changing that shared policy and its regression tests. Both Studios use this policy for groups, assignment actions, properties and badges; mutations and clipboard paste enforce it in the engine.
* Internal assignments use `DatabaseObject.getFullQName()`, such as `Demo.sq:Main`. Workspace assignments use project names. `projectTags` contains only declarations belonging to that file's owner, without a list of other members.
* Existing unsupported memberships are diagnosed and preserved on disk; their sources become read only in the tag manager and no unsupported object badge or group is displayed. No automatic deletion or reinterpretation of those memberships occurs.
* Missing documents mean no tags. A read never creates an empty project source. Portable discovery may update the local workspace document.

`TagDocument` reads strict UTF-8 JSON, rejecting duplicate keys, trailing values, unsupported schema versions and excessive sizes. Documents are limited to 2 MiB, 5,000 definitions, 100,000 assignments, nesting depth 64, 256-character labels, 4,096-character descriptions and 64 KiB per definition. Colors accept only `#RRGGBB`. Maps and membership sets serialize in sorted order, with two-space indentation, LF and a final newline. Unknown document/presentation/metadata values survive roundtrips.

Writes validate the whole document, check the expected source fingerprint, create an adjacent temporary file and replace atomically. Unchanged bytes skip the write. Sidecar or parent symlinks are refused for editing. Invalid documents remain available for diagnostics and do not block normal project loading or execution.

## Editing and lifecycle

Project edits remain drafts until the existing project **Save** command exports the project and its sidecar. Save failures retain dirty state. Reload/Discard drops drafts; after discarding a portable publication, the workspace organization remains and the manager reports `unpublished` until explicit republication. Workspace-only edits save locally after validation and never save a project implicitly.

Bindings retain actual original DBO instances. Rename includes descendants and works independently of optional external-reference refactoring (`UPDATE_NONE`). Reconciliation prepares all affected source drafts before applying a move or deletion. A missing target remains unresolved until project reload/explicit repair; later objects with the same name are not adopted automatically. External file changes reload clean snapshots and conflict with dirty drafts.

Clipboard attributes carry definitions and membership maps outside normal DBO serialization. Paste stages actual copied names in a batch, then commits after the entire operation succeeds. Same-project copies reuse IDs. Cross-project copies/moves reuse a definition only if its ID and content agree, otherwise they allocate and remap a local ID. Project import/duplication rebases only the owner's QName prefix. Project rename defers workspace membership until success and restores tag sources/drafts on failure.

Both clipboard adapters track newly attached roots until the batch's tag commit succeeds. Invalid metadata, a failing child or later clipboard root, a read-only sidecar or an external source conflict removes these copied subtrees and restores the existing ancestors' dirty flags. Available contribution fields are validated before committing; unavailable namespaces travel unchanged. Existing model objects and sources are preserved. Rename reconciles memberships before running bean-name callbacks so a failed tag check cannot leave a step expression renamed.

This rollback covers the batch of newly copied DBOs. Virtual clipboard actions, existing cut/move behavior and builder callbacks after a successful tag commit retain their existing semantics; they are not part of a general model transaction.

The YAML writer owns `.yaml` files in `_c8oProject`, so it still removes obsolete YAML while preserving JSON sidecars. CAR export/import includes those source files.

## Projects edited with an older Studio

Tags do not raise the minimal Convertigo version of a project. A server of an older version loads and runs a tagged project: it ignores `_c8oProject/tags.json`, and never serves it over HTTP, since every version from 8.3.0 forbids `_c8oProject/`. At runtime, only Flow requests read tags.

Studios before 8.4.6 delete the tags of a project when they save it: 8.3.0 empties `_c8oProject`, and 8.3.13 to 8.4.5 delete every file of `_c8oProject` they did not write. After saving a tagged project with such a Studio, restore its tags before committing: `git restore _c8oProject/tags.json`. From 8.4.6, the YAML writer of the 8.4 Studio only removes obsolete YAML sources and keeps `tags.json`, without using it.

The tags stay in `_c8oProject`: at the root of the project, servers before 8.4.4 would serve them, since `.httpignore` does not exist there, and `_private/` is ignored by the `.gitignore` of projects.

## Local and shared project tags

Shared is a portable declaration, not a permission or a network/Git operation. Preparing sharing, removal, republication or alignment checks every affected member is open and writable, then marks its project draft dirty. Each project needs an explicit Save. Publication is not atomic across projects; the manager compares local intent, project drafts and saved sources and reports `published`, `pendingSave`, `unpublished` or `conflict` per member.

Discovery preserves IDs. Unknown portable IDs are imported locally without rewriting the project; differing definitions under the same ID remain conflicts. Users choose a local presentation explicitly and may separately prepare alignment of known sources. Choosing presentation alone leaves portable sources untouched. Libraries acquire memberships only through explicit assignment.

## Declarative metadata contributions

Register a descriptor once with `TagManager.get().contributions().register(namespace, descriptor)`. The descriptor has a `label` and a `fields` object; each field has `label`, `type` and optional `required`. Types are `string`, `boolean`, `integer`, `number`; string fields may declare an `enum` array. Registration accepts up to 128 fields and 128 unique enum choices. The engine validates edits and supplies the same descriptors to both Studios.

For example, the neutral test descriptor is:

```json
{
  "label": "Neutral test contribution",
  "fields": {
    "count": { "label": "Count", "type": "integer" },
    "enabled": { "label": "Enabled", "type": "boolean" },
    "level": { "label": "Level", "type": "string", "enum": ["low", "high"] }
  }
}
```

Values live under `definition.metadata[namespace]`. Unknown namespaces and fields are shown as unavailable/read-only and must be preserved on edit. Suggestions from another open project copy only presentation and allocate a new ID, without importing metadata. `badges(dbo)` exposes the definitions and metadata assigned to a target; it does not inherit from a parent or run business callbacks. Metadata is not secret storage.

## Admin facades and explorer adapters

`studio.tags.Get` accepts an explicit `scope` (`projectObjects` or `workspaceProjects`) and `project` for object scope. It returns revision, definitions, memberships, descriptors, diagnostics, dirty state, portable origins/conflicts/publication and open-project suggestions. The `targets` array lists the QNames of the loaded project's eligible objects, including objects without tags, by kind (sequences, transactions, pages, shared components, shared actions) then by name; `targetDetails` gives the `kind` and `label` of each one (a transaction is labeled after its connector) for both Studios. Object targets are resolved within their owning loaded project. Workspace names come from the existing project manager; clients cannot supply storage paths.

`studio.tags.Apply` additionally accepts the exact `revision`, `action` and a strict JSON `input`. Actions are `create`, `update`, `delete`, `assign`, `remove`, `clear`, `transfer`, `share`, `republish`, `resolve`. Definitions use `input.definition`, edit/delete/share operations use `input.id`, and memberships use `input.targets`/`input.tagIds`. Transfer also uses `fromTagId`. Destructive membership/tag operations require the domain's confirmation fields; delete validates the displayed `memberCount`. Mutations return `done`, the updated snapshot, `dirtyProjects`, `affectedTargets` and canonical `affectedContainers` for targeted refresh.

Get uses existing `WEB_ADMIN`/`PROJECT_DBO_VIEW` roles; Apply uses `WEB_ADMIN`/`PROJECT_DBO_CONFIG`. These services retain the framework's error response format. Existing HTTP filters forbid `_c8oProject` source downloads. Text is escaped by the standard UI renderers; colors cannot contain arbitrary CSS or URLs.

Eclipse invokes the domain directly. Both explorer adapters project real collections into virtual groups. Occurrence row IDs differ from target IDs, expansion is per occurrence, and commands/properties deduplicate canonical targets. Groups never become DBO parents or API targets. Dragging an existing target into a group assigns membership; explicit transfer removes only the source tag; Untagged removal asks for confirmation. Palette drops require a real parent. Read-only Information properties display current object/project labels.

Both Studios expose **Edit tag…** (on a tag group), **Object tags…**, **Project tags…**, **Create tag from references…**, **Group by tags** and **Display tags** in a **Tags** category of the explorer context menus, which lists only the tag actions available for the selection. **Display tags** is a local display preference, independent of the grouping, that removes the tag badges from the tree labels. Collections with no assigned tag keep their normal children directly, without an **Untagged** folder; this also applies when unused tag definitions exist. Rows inside a tag group omit that group's badge and retain any other assigned tags; the normal tree shows all badges.

Tags are edited from two points of view. **Object tags…** and **Project tags…** show the point of view of the selection: every existing tag of its scope (the project for objects, the workspace for projects) with a checkbox, partial when only some of the selected targets have it, the member count of each tag, a field that finds a tag or creates one with the typed label and assigns it, and, for a single target with several tags, their order. **Edit tag…** on a group, or the edit button of a tag in that view, shows the point of view of the tag: a tag list and a detail pane with label, color, description, extension fields and a checkbox for each eligible project or project object, shown with its kind. Membership checkboxes apply immediately; definition changes use **Create tag** or **Update tag**. Sharing and advanced metadata, suggestions, identity and deletion stay collapsed until requested.

## Validation

Run `./gradlew :engine:tagTest :engine:stepSourceTest :engine:studioSearchTest` for domain and nearby regressions. `TagManagerTest` covers persistence, malformed/concurrent sources, Save/Reload, original versus cloned projects, identity changes, copy/move, failed batches and project rename rollback, portable sharing/conflicts, metadata, YAML/CAR and lazy workspace projection. Its scale fixture measures 10 and 10,000 closed projects and asserts zero project loads. `TagPasteAdapterTest` exercises the real web paste adapter with live engine models. After `:engine:testClasses`, library synchronization and a Maven Studio build, run `node eclipse-plugin-studio/tests/tag-clipboard-paste.test.cjs` for the same six clipboard scenarios through production `ClipboardManager`, without starting an SWT display.

Web UI regressions are in `convertigo-studio-web/tests/studio.spec.js` (`studio tags…`). They use mocked services and prove context-menu editing and view switching, current-group badge suppression, editor/escaping/dirty refresh, typed values, independent occurrence expansion with a single canonical clipboard command, exclusion of steps/mixed selections, direct sequence URL availability, loaded workspace branch preservation and immediate group counter refresh. They also fill and submit inline renames using both the menu and F2 on duplicate occurrences, check project capabilities in normal view after workspace refresh, and hold a membership request pending before starting a new tag. They complement live tests; they do not prove source persistence or Eclipse behavior. Build the web Studio with `npm run build`; synchronize Eclipse libraries with `./gradlew :eclipse-plugin-studio:syncLib` before `mvn -DskipTests install` so Tycho compiles against the current engine JAR. Run the web and Maven builds sequentially: web output is copied into Tycho's packaged plug-in.
