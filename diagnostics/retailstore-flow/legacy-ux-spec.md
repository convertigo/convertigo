# RetailStore Flow black-box UX specification

This specification records observable behavior only. It intentionally omits
the legacy component tree, FullSync design-document names, view keys and
implementation code.

## Application shell

- Use a compact mobile-first application shell on a light background with a
  persistent cart/store mark and a top bar. Its uppercase title is
  `SYNCHRONIZATION`, `OPTIMIZATION`, `STORE` or `PRODUCT` for the current state.
- The catalog is the primary experience. Do not expose provisioning controls,
  seed diagnostics, raw JSON, transaction status cards or implementation help
  in the normal browsing UI.
- Catalog cards form a two-column grid at 390 px and expand to more columns on
  wider screens. Cards are light, compact and visually separated from the page;
  the image dominates and the name is below it. Product grids do not add
  packaging or price text that is absent from the reference browsing view.

## First launch

1. A new browser profile starts automatically; no provisioning button or blank
   shell is shown.
2. Server initialization, client synchronization and local index preparation
   are three ordered states. Only the currently active state is visible.
3. Each state has a concise label, its supplied animated fixture and an error
   state. Synchronization additionally exposes meaningful transferred/total
   progress when the SDK provides it.
4. Synchronization is terminal: `active 100/100` must transition to index
   preparation and cannot remain displayed indefinitely.
5. Index preparation executes a real local catalog query and waits for it to
   complete before navigation. A decorative delay does not satisfy this step.
6. The application replaces the working view with the catalog page only after
   all three operations have succeeded.
7. A returning profile may reuse completed work, but must still reach the
   catalog. Two consecutive launches in the same persistent browser profile are
   part of acceptance.

The synchronization title is `SYNCHRONIZATION` with the visible message
`Sync the database on the client.` The indexing title is `OPTIMIZATION` with
`Optimizing the database for the first time.` Initialization may use a concise
localized equivalent while the server fixture is being verified.

## Catalog navigation

- Root catalog: show the 14 top-level departments.
- Selecting a department keeps the user on the catalog route and replaces the
  grid with its children.
- Selecting a child category repeats that behavior until products are reached.
- Above every non-root grid, show a horizontal breadcrumb made of actionable
  segments. The first segment is `RAYONS`; following segments are selected
  ancestors.
- The breadcrumb is before the grid in normal document flow and remains visible
  without scrolling through all cards. It has sufficient contrast against the
  application background and does not collapse into one concatenated label.
- Selecting any breadcrumb segment immediately restores that level. It does not
  reload the application and does not create a separate browser-history entry
  for every category level.
- The selected category path and local query results remain available after
  returning from a product page.

## Product page

- Selecting a product opens a distinct detail route and creates one browser
  history entry.
- The page title is `PRODUCT` and a visible Back control returns to the exact
  product grid and category path.
- Show product image, name, unit price, quantity control, computed total and an
  `AJOUTER AU PANIER` command. The visible back action combines a familiar back
  icon with `RAYONS`. Money uses a consistent currency format, and the total
  visibly combines quantity and price (for example `x 1 = 5.40 EUR`).
- Browser Back and the visible Back control must have equivalent results.

## Offline behavior

- After initial synchronization, category navigation, breadcrumb navigation,
  product lists, product details and return navigation work with networking
  disabled.
- Offline browsing must not issue fetch/XHR requests.
- Static application assets required by the tested branch must remain visible
  offline.

## Acceptance path

1. Start in a fresh browser profile and observe automatic synchronization and
   optimization before the root catalog.
2. Open `EPICERIE SUCREE`, then `GOUTERS & BISCUITS`, then
   `BISCUITS AU CHOCOLAT`.
3. Confirm that the product list contains 22 products.
4. Open `Biscuits Z'animo chocolat lait Cadbury` and verify detail name and
   price.
5. Use browser Back and verify that the same product grid and breadcrumb return.
6. Use breadcrumb segments to return successively to level 3, level 2 and root.
7. Disable networking and complete another branch through product detail and
   back navigation with zero fetch/XHR.
8. Repeat the launch in the same persistent profile and verify that it reaches
   the root catalog again without hanging on synchronization.
9. Validate at 390 x 844 and 1280 x 900 with no horizontal overflow, overlapping
   controls or stacked inactive stages.

## Campaign isolation

- Do not inspect or call the legacy RetailStore project while authoring Run5.
- Do not inspect prior RetailStoreFlow projects or reuse their frontend source.
- The agent receives this specification, fixture manifest and Flow guides only.
- All backend, FullSync and frontend authoring uses Convertigo Flow MCP tools.
