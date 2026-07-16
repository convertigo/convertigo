# RetailStore original vs Flow Run4

Observed on beta at 390 x 844 on 2026-07-16. The original reference is
`sampleMobileRetailStore`; the Flow application is
`sample_RetailStoreFlowRun4`. Captures are under `legacy-ux/` and `run4-ux/`.

## Page-by-page comparison

| Stage | Original | Flow Run4 | Result |
| --- | --- | --- | --- |
| Initial document | Opens `/Sync` with `SYNCHRONIZATION` and `Sync the database on the client.` | The root document is initially blank. | Missing first-launch state. |
| FullSync pull | Shows a dedicated synchronization page while the client database is populated. | Redirects to `/store/`; after 250 ms only `RAYONS` and `Store` are visible. | The current `Status` is not associated with `syncCatalog` and exposes no FullSync progress. |
| First local view | Opens `/Optimize` with `OPTIMIZATION` and `Optimizing the database for the first time.` | The first local view runs invisibly in the Store `OnMount`. | Missing index-preparation state. |
| Store root | Persistent cart mark and `STORE` app bar; 14 category cards in two columns. | 14 cards in two columns, but no app bar and a low-contrast `RAYONS` heading on a black background. | Data path works; shell and contrast do not match. |
| Level 2 | Same `/Store` route; `RAYONS` breadcrumb button and seven children. | Same `/store/` route and seven children, but no breadcrumb. | Grid works; navigation state is invisible. |
| Level 3 | Breadcrumb becomes `RAYONS / EPICERIE SUCREE`; nine children. | Nine children with only the static `RAYONS` heading. | Missing ancestor state and segment actions. |
| Products | Breadcrumb becomes `RAYONS / EPICERIE SUCREE / GOUTERS & BISCUITS`; fixture contract contains 22 products. | No breadcrumb and only 15 products. Prices are raw values such as `5.4000`. | Functional data bug plus formatting gap. |
| Product detail | Distinct `/Detail` route, `PRODUCT`, visible back command, image, `5.4 €`, slider and computed `x 1= 5.40 €`, then add-to-cart. | Distinct `/detail/` route and browser history work. The minus, plus and add-to-cart buttons have no actions; total is the unchanged raw unit price. | Route/get are valid; quantity workflow is not implemented. |
| Browser Back | Restores the product grid and all three breadcrumb segments. | Restores the product grid, but there is no breadcrumb to restore. | Result data survives; navigation context is not rendered. |
| Breadcrumb Back | Each segment restores its level without adding category history entries. | No equivalent control. | Missing workflow. |

## Measured navigation

- Original category changes stay on `/Store`; history remains at 4. Product
  detail raises it to 5.
- Run4 category changes stay on `/store/`; history remains at 3. Product detail
  raises it to 4.
- Run4 therefore already has the correct history boundary. Breadcrumb state
  should be client state, not additional routes.

## Product-count defect

The fixture has 22 product records containing parent id `48196`. Run4 returns
15 rows for that parent. `normalizeFixture` correctly preserves
`levelIdFather` as an array, but the current `itemsByParent` map emits
`String(doc.levelIdFather)` for products. Products belonging to several
categories therefore receive a comma-joined key and disappear from each
individual category. The design view must emit one row for every parent id,
as it already does for categories.

## Required generic Flow capabilities

1. **Action-aware progress**: a Status/Progress block must select an action by
   stable id, expose meaningful loading/error/success labels and render
   FullSync `current`, `total` and status values. The first local view needs a
   separate optimization label.
2. **Structured client list state**: breadcrumb history needs a small generic
   list-state action (`set`, `append`, `truncate`, `clear`) whose values come
   from picker bindings. Category navigation must not use hard-coded paths.
3. **Bindable button labels**: Button needs the same structured `source`
   contract as Text so a breadcrumb iterator can render category names.
4. **Numeric input state**: the detail workflow needs a Range/Stepper block
   and numeric state binding so quantity and computed total are real, not
   decorative buttons.
5. **Formatting**: visible numeric bindings need currency/decimal formatting
   instead of raw CouchDB numbers.

## Recommended correction order

1. Fix the multi-parent FullSync view and migrate server/client markers; prove
   exactly 22 products.
2. Add action-aware progress and display Sync then Optimize before the grid.
3. Add generic list state plus bindable Button labels, then implement and test
   the complete breadcrumb path including offline use.
4. Add numeric input/state and price formatting for the product page.
5. Align the app shell, contrast and card density after the workflows pass.

Do not hard-code the acceptance branch in Run4. Every new capability above is
generic and should be exposed through the Flow palette and structured picker.
