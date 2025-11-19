# Known Issues

## Svelte `animate:flip` warnings

**Status:** open — tracked upstream at [sveltejs/svelte#17181](https://github.com/sveltejs/svelte/issues/17181)

**Symptoms**
- Console warnings: `Invalid keyframe value height: NaNpx`
- Happens when Skeleton panels use `animate:flip` while elements toggle between `display: none` and auto heights.

**Why we keep it**
- The dashboard/list UIs feel much better with FLIP animations than without.
- Removing `animate:flip` would regress perceived performance and spatial continuity across the admin UI.

**Currently affected files**
- `src/routes/(app)/dashboard/[[project]]/+page.svelte` (project cards grid)
- `src/routes/(app)/dashboard/[[project]]/backend/+page.svelte` (requestable accordions)
- `src/lib/admin/components/RequestableVariables.svelte`
- `src/lib/admin/components/LogViewer.svelte`

**Workaround**
- Keep `animate:flip` in place.
- Ignore the warnings until the upstream fix lands; once Svelte releases a patched version, retest the screens above and remove this note.
- If the warnings become blocking in CI, gate them behind a runtime flag so we can switch animations off temporarily.
