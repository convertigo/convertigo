# Convertigo Admin UI Good Practices

## Philosophy

- Build on Svelte 5 with Skeleton 3 and Tailwind 4; respect their patterns but always route through our thin abstraction layer before falling back to raw classes.
- Default to the custom utility system; it encodes the responsive rhythm (mobile/desktop) so we rarely hand-write raw Tailwind recipes.
- Prefer KISS structures: small Svelte files, explicit data flow, reuse shared snippets/components before creating new variants.
- Keep visuals coherent by leaning on the existing utilities (`button-*`, `layout-*`, `preset-*`) and theme tokens defined in `convertigo.theme.css`.
- The class priority order is `src/app.css` utilities first, `convertigo.utilities.css` second, and only then Skeleton/Tailwind primitives if no dedicated helper exists.
- When a pattern appears twice, factorise it (component, snippet or helper) instead of copy/pasting class stacks or logic.

## Layout & Spacing Utilities

- `src/convertigo.plugin.js` generates `convertigo.utilities.css`; spacing tokens are `''` (standard), `-low`, and `-none`, and auto-scale with the `md` breakpoint.
- Use the semantic utilities (`gap`, `px`, `layout-x`, `layout-y`, `layout-grid`, etc.). They already expose negative forms (`-px`, `-gap`) and margin/padding variants (`layout-x-m`, `layout-y-p`, ...), so avoid reintroducing raw Tailwind spacing values.
- `layout-x*` → horizontal flex wrappers, `layout-y*` → vertical stacks, `layout-grid*` → responsive auto-fit grids. The suffix sets spacing: `layout-x-low`, `layout-grid-[300px]`, `layout-y-stretch` (with alignment suffixes like `-start`, `-end`, `-stretch`).
- Do not sprinkle `md:` prefixes for spacing; the utilities already inject the desktop spacing. Add responsive modifiers only for behaviour (visibility, order, width).
- When we need tight spacing on one axis only, combine the utilities: e.g. `layout-x gap-none` or `layout-y-low mt`.

## Visual Style Tokens

- Global utilities live in `src/app.css`. Buttons (`button-primary`, `button-secondary`, ...), icon buttons (`button-ico-*`), chips, inputs and motif backgrounds are declared there. Always prefer those over ad-hoc colour stacks.
- The theme in `convertigo.theme.css` centralises colours (`--color-*-*`), radii and spacing; reference those tokens (via the utilities) instead of hex values.
- Shadows/gradients: reuse `shadow-follow` and the `preset-*` helpers; if a new tone is required, extend the preset utility rather than duplicating `@apply` blocks in components.

## Component Baseline (src/lib)

- **Buttons**: use `$lib/admin/components/Button.svelte` for any clickable element that needs consistent spacing/ARIA. Pass `cls` with one of the `button-*` utilities. Use `ResponsiveButtons` when aligning multiple actions.
- **Card**: wrap admin panels/forms in `Card` to inherit padding, border, shadow and optional title slot. Prefer `cornerOption` snippet for right-aligned header content.
- **AccordionGroup/AccordionSection**: thin wrappers over Skeleton’s accordion to enforce Convertigo spacing, rounded surfaces and header layout. Pass snippets via `title`/`subtitle`/`meta`/`panel` rather than Svelte 4 slots.
- **TableAutoCard**: default for tabular data. Supplies responsive card-mode automatically (`layout-grid-low-*`), placeholders via `AutoPlaceholder`, and custom cell snippets.
- **InputGroup / PropertyType / CheckState**: base form controls with built-in label/icon layout and standardised classes. Reach for them before rolling new inputs.
- **ModalDynamic**: promise-based modal that pairs with Skeleton's `<Modal>`. Opens with `await modal.open(params)`; always call `close()` or update `setResult` to resolve.
- **Ico**: central icon registry mapping `mdi:*` ids → raw SVG. Use this component so tree-shaking continues to work; do not import `@iconify` directly in feature code.
- **ServiceHelper**: pattern for calling backend services. Provides lazy loading (`values.refresh()`), auto refresh delay, array normalisation and error handling. Call `onDestroy(Service.stop)` after destructuring helpers from it.

## Svelte 5 Patterns

- Use `$props`, `$state`, `$derived`, `$effect`, `$bindable` consistently; they keep components terse and declarative.
- Derived values should remain cheap/side-effect free; expensive computation belongs in helpers or memoised stores.
- When destructuring `$props()`, avoid defaulting to `undefined`; omit the default and let absence propagate naturally.
- Group snippets (`{#snippet ...}`) so reusable UI blocks stay colocated with their logic (e.g. modal contents, row renderers). Reviewers should ensure snippets remain pure and small.
- Optional snippets can be rendered inline with `{@render snippet?.()}` rather than an explicit `{#if}` wrapper.

## Data & Services

- All HTTP calls go through `call`/`callRequestable` in `src/lib/utils/service.js` to benefit from XSRF headers, file handling and toaster integration. Avoid raw `fetch` unless there is no backend service.
- Arrays coming from SOAP/XML responses must pass through `checkArray` to keep Svelte loops stable.
- Long-running panels should expose `values.delay` to refresh regularly and call `values.stop()` in `onDestroy` (see Projects page) to prevent orphaned timers.

## Routing & Layout

- The app layout (`src/routes/(app)/+layout.svelte`) defines the main column/rail structure. Reuse `<PagesRail />` for navigation and respect the left rail toggles (`PagesRailToggle`, `Topbar`).
- Secondary panels live in `RightPart.svelte`; prefer exposing a snippet from feature routes instead of creating parallel layouts.

## Review Checklist

- Spacing & alignment rely on `layout-*`/`gap-*` utilities; no raw Tailwind spacing literals unless creating a new base utility.
- Shared components (`Button`, `Card`, `TableAutoCard`, `InputGroup`, `ModalDynamic`, `ResponsiveButtons`, `Ico`) are used instead of duplicated markup.
- Services use `ServiceHelper` or `call`, and reactive state follows the `$state/$derived` conventions.
- New utilities or theme extensions land in `convertigo.plugin.js` / `app.css`, not inline.
- Accessibility: button/link semantics via the `Button` component, labels associated through `InputGroup` or an explicit `for`/`id`.
- Factorisation opportunity flagged when similar snippets repeat—prefer extracting to `/lib/admin/components` or `/lib/common/components`.

## Further Factorisation Ideas

- Audit repeated modal layouts to see if we can provide higher-level modal presets (confirm, form, picker) built on `ModalDynamic`.
- Centralise table definition objects (columns, formatters) when multiple routes share similar grid setups, possibly via a `/lib/admin/tables` directory.
- Extract common service adapters (e.g. refresh/export workflows) into thin helpers to reduce page-level boilerplate.
