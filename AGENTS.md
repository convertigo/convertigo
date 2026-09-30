# Work in progress

These repo-wide notes are still evolving. Prefer more specific `AGENTS.md` files in subdirectories when they exist.

## Dependency maintenance

- For hotfix/develop dependency update work, read [.agents/skills/convertigo-studio-update/SKILL.md](.agents/skills/convertigo-studio-update/SKILL.md) and use its lane-specific procedure and helper scripts.
- Prefer this repository's version of the procedure over a separately installed copy. Commands operate on the current checkout; do not assume personal paths or a neighboring clone.
- Apply dependency and toolchain version edits before `generateEclipseConfigurationWithManifest`. Eclipse/platform and stable Tycho updates are permitted on develop, with a fresh Maven Studio build when either changes.
- Updating dependencies does not authorize committing, merging, or pushing. Perform those actions only when explicitly requested.

## Local code graph

- For optional Java/Studio web relationship searches, use [.agents/skills/convertigo-code-graph/SKILL.md](.agents/skills/convertigo-code-graph/SKILL.md). Generated graphs stay local under ignored `build/graphify/`; do not commit them.
- Prefer this scoped recipe over a generic installed Graphify workflow. It does not index repository documents or private notes, and does not use a model provider.
- Check the local index's content fingerprint before relying on it. Build or refresh it when useful for the current task, not on every edit, dependency update, build, or commit. Ordinary targeted source searches do not require Graphify.
- Community names are structural hints, not architecture claims. Verify important results in the source, especially Svelte and inferred edges.
- For web-to-Java admin relationships, prefer the recipe's bounded `services`/`impact` queries and `services.html` detail view. Keep call sites, types, source references, confidence, and unresolved expressions visible; an import alone does not imply calls to every service in a module.

## Changelog review

- Treat `CHANGELOG.md` entries marked with `- *` as generated drafts that still need editorial review.
- Review the linked GitHub issue and the associated fix commit before rewriting a changelog entry.
- Keep only user-visible outcomes. Remove duplicate issues, minor dependency bumps, internal refactors, and anecdotal one-off adjustments unless they materially affect users.
- Prefer the functional consequence of the fix over the literal issue title.
- Normalize square-bracket categories to the impacted product area, such as `[Admin]`, `[Dashboard]`, `[Studio]`, `[Engine]`, `[FullSync]`, or `[Redis]`.
- In `#### Bug Fixes`, write entries as `[Cat] Fixed, ...`.
- In `#### Improvements` and `#### New Features`, use concise user-facing sentences and avoid implementation detail.
- If several issues describe the same shipped behavior, keep a single changelog entry and use the most relevant issue reference.
