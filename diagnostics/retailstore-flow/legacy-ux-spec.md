# RetailStore Flow black-box UX specification

This specification records observable behavior only. It intentionally omits
the legacy component tree, FullSync design-document names, view keys and
implementation code.

## Application shell

- Use a compact mobile-first application shell with a persistent store mark and
  a short uppercase page title.
- The catalog is the primary experience. Do not expose provisioning controls,
  seed diagnostics, raw JSON, transaction status cards or implementation help
  in the normal browsing UI.
- Catalog cards form a two-column grid at 390 px and expand to more columns on
  wider screens. Images and labels are the primary card content.

## First launch

1. A new browser profile opens a synchronization page automatically.
2. Synchronization starts without a user click and displays meaningful progress.
3. A first-run optimization page follows automatically while local query indexes
   are prepared.
4. The application then replaces the working view with the catalog page.
5. A returning profile may skip completed first-run work, but must still reach
   the catalog without a provisioning button.

## Catalog navigation

- Root catalog: show the 14 top-level departments.
- Selecting a department keeps the user on the catalog route and replaces the
  grid with its children.
- Selecting a child category repeats that behavior until products are reached.
- Above every non-root grid, show a horizontal breadcrumb made of actionable
  segments. The first segment is `RAYONS`; following segments are selected
  ancestors.
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
  `AJOUTER AU PANIER` command.
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
8. Validate at 390 x 844 and 1280 x 900 with no horizontal overflow, overlapping
   controls or stacked inactive stages.

## Campaign isolation

- Do not inspect or call the legacy RetailStore project while authoring Run4.
- Do not inspect prior RetailStoreFlow projects or reuse their frontend source.
- The agent receives this specification, fixture manifest and Flow guides only.
- All backend, FullSync and frontend authoring uses Convertigo Flow MCP tools.
