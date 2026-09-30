# Work in progress

These repo-wide notes are still evolving. Prefer more specific `AGENTS.md` files in subdirectories when they exist.

## Dependency maintenance

- For hotfix/develop dependency update work, read [.agents/skills/convertigo-studio-update/SKILL.md](.agents/skills/convertigo-studio-update/SKILL.md) and use its lane-specific procedure and helper scripts.
- Prefer this repository's version of the procedure over a separately installed copy. Commands operate on the current checkout; do not assume personal paths or a neighboring clone.
- Apply dependency and toolchain version edits before `generateEclipseConfigurationWithManifest`. Eclipse/platform and stable Tycho updates are permitted on develop, with a fresh Maven Studio build when either changes.
- Updating dependencies does not authorize committing, merging, or pushing. Perform those actions only when explicitly requested.

## Changelog review

- Treat `CHANGELOG.md` entries marked with `- *` as generated drafts that still need editorial review.
- Review the linked GitHub issue and the associated fix commit before rewriting a changelog entry.
- Keep only user-visible outcomes. Remove duplicate issues, minor dependency bumps, internal refactors, and anecdotal one-off adjustments unless they materially affect users.
- Prefer the functional consequence of the fix over the literal issue title.
- Normalize square-bracket categories to the impacted product area, such as `[Admin]`, `[Dashboard]`, `[Studio]`, `[Engine]`, `[FullSync]`, or `[Redis]`.
- In `#### Bug Fixes`, write entries as `[Cat] Fixed, ...`.
- In `#### Improvements` and `#### New Features`, use concise user-facing sentences and avoid implementation detail.
- If several issues describe the same shipped behavior, keep a single changelog entry and use the most relevant issue reference.
