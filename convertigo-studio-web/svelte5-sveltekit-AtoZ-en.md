# Svelte 5 + SvelteKit 2 — **A→Z Guide for LLMs/Codegen (Codex‑ready)**

> **Goal**: Make a codegen model (e.g. “Codex”) **reliable** on Svelte 5 and SvelteKit 2.  
> **Scope**: Full reference (+ patterns & anti‑patterns) for the **runes** model, **snippets + {@render}**, modern **events**, and SvelteKit 2 data‑flow (load, actions, remote functions, streaming).  
> **Mode**: This document assumes **Svelte 5 runes** (not legacy). It includes migration notes where useful.
> **Skeleton**: Pair this guide with [Skeleton’s “LLMs + Svelte” reference](https://www.skeleton.dev/llms-svelte.txt). That file defines the composed-part APIs, data/state attributes and event names used throughout Skeleton 4.

---

## 0) _Do‑not‑hallucinate_ contract (hard rules for LLMs)

1. **Only** use APIs present in the official docs. If something isn’t in the docs, **don’t invent it** — leave a `// TODO: confirm in docs` comment.
2. **Events**: use **DOM attributes** like `onclick`, `oninput`, etc. **Do not** use `on:` in runes mode. Handle modifiers (e.g. preventDefault) **by hand** in the handler.
3. **Children/content**: use **snippets** — declare with `{#snippet ...}` and **render** with `{@render ...}`. **Never** put a snippet **inside an attribute**.
4. Prefer **snippets** over slots (slots are **deprecated** in Svelte 5).
5. **Props** come from `let { ... } = $props()`. Avoid mutating non‑bindable props. For two‑way binding, declare a prop with **`$bindable`**.
6. **Reactivity**: use `$state` for state, `$derived` for _pure_ computed values, `$effect` for side‑effects.
7. **Externalize reusable logic** to `.svelte.js` / `.svelte.ts` modules — runes are valid there, too.
8. **SvelteKit**:
   - Page/layout data via `load` in `+page.ts`/`+page.server.ts`.
   - **Form actions** live in `+page.server.ts`; client enhancement via `use:enhance`.
   - Prefer **Remote Functions** for typed client↔server calls when available.
9. **Type discipline**: when destructuring `...rest` from `$props()`, treat it as **`Record<string, any>`** (JSDoc) or `Record<string, any>` (TS). Don’t let the model infer narrow or incorrect shapes.
10. When using another **component**, **read its source** to discover accepted props (`$props` destructuring). If unavailable, **don’t invent props** — pass only documented ones and standard HTML attributes.

> Official docs pages backing these rules are listed at the end of this file (see **References**).

---

## 1) Runes: explicit reactivity (Svelte 5)

### 1.1 `$state` — reactive state

```svelte
<script>
	let count = $state(0);

	function inc() {
		count++;
	}
</script>

<button onclick={inc}>Count: {count}</button>
```

**Notes**

- `$state` creates a reactive proxy for scalars/objects. Mutating `$state` objects (e.g. `user.likes++`) triggers updates.
- `$state.raw(value)` injects a non‑proxied value; `state.snapshot()` yields a plain serializable object.

### 1.2 `$derived` — pure derived values

```svelte
<script>
	let count = $state(2);
	let double = $derived(count * 2);
</script>

<div>{double}</div>
```

**Constraints**: no side‑effects inside `$derived(...)`. The expression should be pure.

### 1.3 `$effect` — side‑effects tied to dependencies

```svelte
<script>
	let query = $state('');

	$effect(() => {
		// Runs after DOM update, re‑runs when any accessed state changes
		console.log('search:', query);
		return () => console.log('cleanup'); // before next run or unmount
	});
</script>
```

**SSR**: `$effect` **does not run** during server‑side rendering.

### 1.4 `$props` — component inputs

```svelte
<script lang="ts">
	// Renaming + fallbacks + rest
	let {
		class: klass = '',
		title,
		...rest
	}: { class?: string; title: string } & Record<string, any> = $props();
</script>

<button class={klass} {...rest}>{title}</button>
```

- Prefer destructuring.
- **Mutations**: don’t mutate non-bindable props. Temporary reassignment is allowed, but avoid mutating values you **don’t own**.
- Skip useless defaults: don’t write `value = undefined`. Omit the initializer and let absence propagate naturally.
- Optional snippets can be rendered inline with `{@render snippet?.()}` to avoid boilerplate `{#if}` wrappers.

**JSDoc variant** (with `...rest` typed to **any**)

```svelte
<script>
	/** @type {{ class?: string; title: string } & Record<string, any>} */
	let { class: klass = '', title, ...rest } = $props();
</script>
```

### 1.5 `$bindable` — two‑way binding prop

```svelte
<!-- Child.svelte -->
<script>
	let { value = $bindable('') } = $props();
</script>

<input bind:value />

<!-- Parent.svelte -->
<Child bind:value={name} />
```

Use sparingly; prefer one‑way data flow + callbacks unless two‑way semantics are truly desired.

### 1.6 `$inspect`, `$host`

- `$inspect` — development‑time introspection/debug.
- `$host` — advanced host element access / custom elements interop.

### 1.7 Using runes outside components

You can place runes in **`.svelte.js` / `.svelte.ts`** modules to share reactive state/logic across components:

```ts
// Anywhere
import { count } from '$lib/counter.svelte';

// counter.svelte.ts
export const count = $state(0);

count++; // reactive wherever consumed
```

---

## 2) Template syntax essentials

### 2.1 Control blocks

```svelte
{#if ok}<p>OK</p>{:else}<p>NO</p>{/if}

<ul>
	{#each items as item (item.id)}<li>{item.name}</li>{/each}
</ul>

{#key someChangingId}
	<ExpensiveSubtree />
{/key}

{#await promise}
	<p>Loading…</p>
{:then data}
	<pre>{JSON.stringify(data)}</pre>
{:catch e}
	<p>Error: {e.message}</p>
{/await}
```

### 2.2 **Snippets** + `{@render}` (replace most slot use‑cases)

**Declare** a snippet and **render** it:

```svelte
{#snippet Sum(a, b)}<p>{a} + {b} = {a + b}</p>{/snippet}

{@render Sum(1, 2)}
{@render Sum(3, 4)}
```

**Pass** snippets to components:

```svelte
<!-- Table.svelte -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	let { header }: { header: Snippet<[string]> } = $props();
</script>

<!-- Parent -->
{#snippet header(title)}<h2>{title}</h2>{/snippet}
<Table {header} />

<table>
	<thead>{@render header('My title')}</thead>
	<tbody>{@render row?.(anyRow)}</tbody>
</table>
```

**Implicit `children` snippet**:

```svelte
<!-- Button.svelte -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	let { children }: { children?: Snippet } = $props();
</script>

<!-- Parent -->
<Button>OK</Button>

<button>
	{@render children?.()}
	{#if !children}<span>Default</span>{/if}
</button>
```

**Important**

- Never put `{#snippet ...}` **inside an attribute** (e.g. `header="..."`).
- Don’t invent placeholder snippets like `"emptySnippet"`. Use `?.()` or an `{#if}` fallback in markup.
- Slots exist as **legacy**; favor snippets in modern code.

### 2.3 Events (runes mode)

```svelte
<button
	onclick={(e) => {
		e.preventDefault();
		doThing();
	}}
>
	Submit
</button>
```

- Use `onclick`, `oninput`, `onchange`, … (DOM attribute names).
- For component‑level events, accept **callback props** (e.g. `onclick`), and call them from the child.
- Don’t use `on:` syntax or its pipe modifiers in runes mode.

### 2.4 Other directives

- `bind:` — element/property bindings and component prop bindings.
- `use:` — actions (e.g., `use:enhance` with SvelteKit forms).
- `transition:`, `in:`/`out:`, `animate:` — motion/animation.
- `class:` and `class={expr}`; `style:` bindings.
- `{@html}` (unsafe HTML; sanitize first), `{@const}`, `{@debug}`, `{@attach}`.

### 2.5 Skeleton 4 components (quick rules)

- Components are **composed**: import the `Root` and its parts (`Dialog.Trigger`, `Tabs.List`, `Popover.Content`, …). Don’t rely on removed v3 props.
- Events dispatched by these parts are lowercase DOM attributes (`onopenchange`, `onvaluechange`, `ondismiss`). Subscribe using the DOM name; the payload lives in `event.detail`.
- When you must customise the inner markup, use the provided `element` snippet and spread the attributes (`{@render element?.({ attributes })}`) so `data-state` toggles, focus and animations keep working.
- Keep the supplied hidden inputs (`SegmentedControl.ItemHiddenInput`, etc.) for form controls — they synchronise the Zag.js machine with native form submission.
- Toasts, dialogs, popovers and other overlays require their wrapper parts (`<Toast.Group>`, `<Portal>`, `<Dialog.Backdrop>`). Omitting them breaks z-index/focus management.

---

## 3) Special elements

### `<svelte:boundary>` — pending & error isolation

```svelte
<svelte:boundary onerror={(err, reset) => console.error(err)}>
	<Content />

	{#snippet pending()}<p>Loading…</p>{/snippet}
	{#snippet failed(error, reset)}
		<p>Oops: {error.message}</p>
		<button onclick={reset}>Retry</button>
	{/snippet}
</svelte:boundary>
```

Use boundaries to wall‑off parts of the tree, show _pending_ UI for inner `{#await}` blocks, and display local error UI with a reset function.

Other specials: `<svelte:window>`, `<svelte:document>`, `<svelte:body>`, `<svelte:head>`, `<svelte:element>`, `<svelte:options>`.

---

## 4) Styling

- `<style>` blocks are **scoped** to the component.
- Use `:global(...)` to opt out.
- Nested `<style>` blocks per component are supported.

---

## 5) Runtime interop

### 5.1 Stores

Stores still work and can coexist with runes. Prefer `$state` for local component state, and stores for shared/persistent/app‑level state.

### 5.2 Context & lifecycle

- `setContext` / `getContext` unchanged.
- `onMount`, `beforeUpdate`, `afterUpdate`, `onDestroy` remain valid (with runes).

---

## 6) SvelteKit 2 — essentials

### 6.1 File‑system routing (quick map)

```
src/routes/
  +layout.svelte             # root layout (must render children)
  +layout.ts                 # universal load (optional)
  +layout.server.ts          # server-only load (optional)
  +page.svelte               # page component
  +page.ts                   # universal load
  +page.server.ts            # server-only load + form actions
  +server.ts                 # endpoint (RequestHandler)
```

- `+layout.svelte` must render children via `{@render children()}`.
- Layouts nest and propagate down the route tree.

### 6.2 Data loading (`load`)

- **Universal load** (`+page.ts`/`+layout.ts`) runs on SSR initial render and on client navigations.
- **Server load** (`+page.server.ts`/`+layout.server.ts`) runs only on the server (can access env vars, DB, cookies).
- You can **stream** promises from server loads for progressive rendering.

**Example (streaming)**

```ts
// +page.server.ts
export const load = async ({ params }) => {
	return {
		post: await db.getPost(params.slug), // awaited (blocks)
		comments: db.getComments(params.slug) // Promise (streams)
	};
};
```

```svelte
<!-- +page.svelte -->
<script lang="ts">
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<h1>{data.post.title}</h1>
{#await data.comments then list}
	{#each list as c}<Comment {c} />{/each}
{/await}
```

### 6.3 Form actions + `use:enhance`

- Define actions in `+page.server.ts`:
  ```ts
  export const actions = {
  	default: async ({ request }) => {
  		const form = await request.formData();
  		// validate, process…
  		return { success: true };
  	}
  };
  ```
- On the client, add `use:enhance` to progressively enhance a `<form method="POST">` that targets a page action:

  ```svelte
  <script lang="ts">
  	import { enhance } from '$app/forms';
  	import type { PageProps } from './$types';

  	let { form }: PageProps = $props();
  </script>

  <form method="POST" use:enhance>
  	<!-- fields -->
  </form>
  ```

- You can customize enhancement hooks (`cancel`, redirects, reset, etc.).

### 6.4 Remote Functions (type‑safe RPC, SvelteKit 2.27+)

- Place functions in `*.remote.ts` (`query`, `query.batch`, `form`, `command`, `prerender`).
- They **always run on the server**; on the client, they compile down to fetch wrappers.
- Combine with Svelte’s `{@await}` (and optional experimental `await` in components) for inline data.

```ts
// data.remote.ts
export async function query() {
	return await db.listPosts();
}
```

```svelte
<script>
	import { query as getPosts } from './data.remote';
</script>

<ul>
	{#each await getPosts() as post}
		<li>{post.title}</li>
	{/each}
</ul>
```

### 6.5 Page options (per page/layout)

- `export const prerender = true | false | 'auto'`
- `export const ssr = true | false`
- `export const csr = true | false`
- `export const trailingSlash = 'never' | 'always' | 'ignore'`

### 6.6 State management (server/client crossing)

- **Never** share mutable module‑level state on the server between requests/users.
- Avoid side‑effects inside `load` — keep it pure; return data.
- Preserve app/page state across navigations carefully; use URL (query/params) or snapshots when appropriate.

---

## 7) TypeScript & JSDoc: _bullet‑proof_ prop typing

### 7.1 Minimal component skeletons

**TypeScript**

```svelte
<script lang="ts">
	// 1) Props with rest as any
	type Rest = Record<string, any>;
	let { id, class: klass = '', ...rest }: { id?: string; class?: string } & Rest = $props();

	// 2) Local state
	let open = $state(false);

	// 3) Derived values
	let label = $derived(open ? 'Hide' : 'Show');

	// 4) Events (callbacks)
	let { onclick }: { onclick?: (e: MouseEvent) => void } = $props();

	function toggle(e: MouseEvent) {
		e.preventDefault();
		open = !open;
		onclick?.(e);
	}
</script>

<button {id} class={klass} {...rest} onclick={toggle}>
	{label}
</button>

<style>
	button {
		all: unset;
		cursor: pointer;
	}
</style>
```

**JSDoc**

```svelte
<script>
	/** @typedef {{ id?: string; class?: string }} KnownProps */
	/** @type {KnownProps & Record<string, any>} */
	let { id, class: klass = '', ...rest } = $props();

	let open = $state(false);
	let label = $derived(open ? 'Hide' : 'Show');

	/** @type {(e: MouseEvent) => void | undefined} */
	let onclick;

	function toggle(e) {
		e.preventDefault();
		open = !open;
		onclick?.(e);
	}
</script>

<button {id} class={klass} {...rest} onclick={toggle}>{label}</button>
```

> **Why `Record<string, any>` for `rest`?**  
> It prevents incorrect inference (e.g. LLM invents fields and TypeScript complains). It also mirrors the fact that **any** extra prop may be forwarded to the underlying element/component.

### 7.2 Snippet typing

**Explicit prop snippet**

```svelte
<script lang="ts">
	import type { Snippet } from 'svelte';

	let { header }: { header: Snippet<[string]> } = $props();
</script>

<header>{@render header('Title')}</header>
```

**Implicit `children` snippet optional**

```svelte
<script lang="ts">
	import type { Snippet } from 'svelte';

	let { children }: { children?: Snippet } = $props();
</script>

<div>
	{@render children?.()}{#if !children}<em>fallback</em>{/if}
</div>
```

**Generic snippet**

```svelte
<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';

	let { items, row }: { items: T[]; row: Snippet<[T]> } = $props();
</script>

<ul>
	{#each items as it (it?.id ?? it)}<li>{@render row(it)}</li>{/each}
</ul>
```

### 7.3 Child component discovery: **don’t invent props**

When using another component (`<Card ... />`), **open the component source** and inspect its `$props` destructuring to know the accepted inputs. If that’s not accessible:

- Pass only **documented props** and valid **HTML attributes**.
- If customization is needed, pass **snippets** rather than made‑up attributes.
- If you must forward arbitrary attributes, use `{...rest}` with `Record<string, any>` types as shown above.

---

## 8) Patterns you can copy‑paste safely

### 8.1 Two‑way binding via `$bindable`

```svelte
<!-- Child.svelte -->
<script lang="ts">
	let {
		value = $bindable(''),
		oncommit
	}: {
		value?: string;
		oncommit?: (v: string) => void;
	} = $props();

	$effect(() => oncommit?.(value));
</script>

<input bind:value />
```

```svelte
<!-- Parent.svelte -->
<Child bind:value={name} oncommit={(v) => save(v)} />
```

### 8.2 Controlled binding with a getter/setter pair

### 8.3 Wrapper components with default headers

When you wrap Skeleton components (AccordionSection, Card, etc.), keep the wrapper thin.
Expose optional props (title, count, trailingText…) but fall back to the documented
Skeleton markup when nothing is set. That way all consumers share the same structure
and styling.

```svelte
<Accordion.ItemTrigger class={triggerClass}>
	{#if control}
		{@render control()}
	{:else if title || typeof count === 'number'}
		<div class="flex items-center justify-between">
			<div class="min-w-0">
				{#if title}<span class="text-sm font-semibold">{title}</span>{/if}
				{#if subtitle}<span class="text-xs text-neutral-500">{subtitle}</span>{/if}
			</div>
			{#if typeof count === 'number'}
				<span class="badge">{countLabel(count)}</span>
			{/if}
		</div>
	{:else}
		{@render defaultHeader()}
	{/if}
</Accordion.ItemTrigger>
```

Guidelines:

- One shared wrapper; no per-page forks.
- Styles come from Skeleton docs (`px-low`, `py-low`, etc.).
- Snippets stay optional via `{@render snippet?.()}`.
- Éviter la multiplication des headers recopiés.

### 8.3 Wrapper components with default headers

When you wrap Skeleton components (AccordionSection, Card, etc.), keep the wrapper thin.
Expose optional props (title, count, trailingText…) but fall back to the documented
Skeleton markup when nothing is set. That way all consumers share the same structure
and styling.

```svelte
<Accordion.ItemTrigger class={triggerClass}>
	{#if control}
		{@render control()}
	{:else if title || typeof count === 'number'}
		<div class="flex items-center justify-between">
			<div class="min-w-0">
				{#if title}<span class="text-sm font-semibold">{title}</span>{/if}
				{#if subtitle}<span class="text-xs text-neutral-500">{subtitle}</span>{/if}
			</div>
			{#if typeof count === 'number'}
				<span class="badge">{countLabel(count)}</span>
			{/if}
		</div>
	{:else}
		{@render defaultHeader()}
	{/if}
</Accordion.ItemTrigger>
```

Guidelines:

- One shared wrapper; no per-page forks.
- Styles come from Skeleton docs (`px-low`, `py-low`, etc.).
- Snippets stay optional via `{@render snippet?.()}`.
- Éviter la multiplication des headers recopiés.

Svelte lets you treat any binding as a controlled value by passing a pair of functions instead of a single reference. The first function is the **getter** (called to read the current value) and the second is the **setter** (called whenever the child emits a change). This is perfect when the source of truth lives outside `$state()` — e.g. a persisted singleton, a store, or a component that expects an array even in single‑select mode.

```svelte
<script>
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';

	// persist the flag globally; anything truthy means "open"
	let settings = persistedState('ui.advanced.open', false);
</script>

<AccordionGroup
	collapsible
	bind:value={
		() => (settings.current ? ['advanced'] : []), // getter
		(v) => (settings.current = v.length > 0)
	}
>
	<!-- Accordion sections ... -->
</AccordionGroup>
```

Guidelines:

- **Always** return/accept the shape expected by the child. Skeleton’s accordion, for example, deals in `string[]`, even in single-select mode.
- Keep the getter **pure**; it must not mutate anything.
- Use the setter to project the child output into your domain (toggle a boolean, write to `persistedState`, dispatch an event, etc.).

### 8.3 List with customizable row snippet

```svelte
<!-- List.svelte -->
<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';

	let { items = [], row }: { items?: T[]; row: Snippet<[T]> } = $props();
</script>

<ul>
	{#each items as item (item?.id ?? item)}
		<li>{@render row(item)}</li>
	{/each}
</ul>
```

### 8.4 Error/pending isolation

```svelte
<svelte:boundary onerror={(e, reset) => (console.error(e), /* show toast */)}>
  <DataWidget/>

  {#snippet pending()}<Spinner />{/snippet}
  {#snippet failed(err, reset)}
    <ErrorPane {err} onretry={reset}/>
  {/snippet}
</svelte:boundary>
```

### 8.5 SvelteKit form with `use:enhance`

```svelte
<!-- +page.svelte -->
<script lang="ts">
	import { enhance } from '$app/forms';
	import type { PageProps } from './$types';

	let { form }: PageProps = $props();
</script>

<form method="POST" use:enhance>
	<input name="email" type="email" required />
	<button>Save</button>
</form>
```

```ts
// +page.server.ts
export const actions = {
	default: async ({ request }) => {
		const data = await request.formData();
		// validate/process
		return { success: true };
	}
};
```

### 8.6 Remote Function + inline await

```ts
// src/routes/blog/data.remote.ts
export async function query() {
	return await db.listPosts();
}
```

```svelte
<script>
	import { query as getPosts } from './data.remote';
</script>

<ul>
	{#each await getPosts() as post}
		<li><a href={'/blog/' + post.slug}>{post.title}</a></li>
	{/each}
</ul>
```

### 8.7 Reusable state in `.svelte.ts`

```ts
// theme.svelte.ts
export const theme = $state<'light' | 'dark'>('light');
export const toggle = () => (theme = theme === 'light' ? 'dark' : 'light');
```

```svelte
<script>
	import { theme, toggle } from '$lib/theme.svelte';
</script>

<button onclick={toggle}>Theme: {theme}</button>
```

---

## 9) Migration cheat‑sheet (Svelte 4 → 5)

| Svelte 4 (legacy)          | Svelte 5 (runes)                            |
| -------------------------- | ------------------------------------------- |
| `let count = 0` (reactive) | `let count = $state(0)`                     |
| `$: double = count * 2`    | `let double = $derived(count * 2)`          |
| `$: { /* effect */ }`      | `$effect(() => { /* effect */ })`           |
| `export let foo`           | `let { foo } = $props()`                    |
| `on:click`                 | `onclick={...}`                             |
| Slots + `let:`             | **Snippets** `{#snippet}` + `{@render}`     |
| `createEventDispatcher()`  | Prefer **callback props** (`onclick`, etc.) |

> Legacy APIs exist for compatibility, but prefer **runes** + **snippets** in new code.

---

## 10) Anti‑patterns & how to fix them

- **React‑isms**: `children` as prop, `className`, `onClick`, `style={{…}}`, React hooks — **ban** these. Use Svelte’s `children` snippet, `class`, `onclick`, standard style bindings, and runes (`$effect`/`$derived`).
- **Snippets in attributes**: Don’t write `header="{#snippet ...}"`. Declare snippets in markup and render with `{@render}`.
- **Fake empty snippets**: Don’t invent `emptySnippet`. Treat snippets as optional (`?.()`) and supply **fallback markup**.
- **Mutating props**: Don’t mutate props unless they’re **bindable**. Prefer callback props or `$bindable` sparingly.
- **Invented component props**: Always inspect the child’s `$props` destructuring. If unknown, **don’t guess**.
- **`on:` / modifier pipes** in runes mode: use `onclick` and handle `preventDefault`/`stopPropagation` in code.
- **Server shared state**: don’t keep request‑scoped mutable state in module variables on the server.

---

## 11) LLM self‑checklist (run before finalizing code)

1. No occurrences of `on:` / `$:` / `export let` / `<slot>` / `className` / React hooks.
2. All **events** are DOM attributes (e.g. `onclick`) with explicit handler logic.
3. All **props** come from `$props()`; any `...rest` is typed as `Record<string, any>`.
4. **Snippets** are declared with `{#snippet}` and rendered via `{@render}` — never inside attributes; optional snippets use `?.()` or `{#if}` with fallback.
5. No mutation of non‑bindable props; two‑way use `$bindable` or callbacks.
6. If using another **component**, the props match its actual `$props` signature.
7. In SvelteKit, forms target page **actions** and use `use:enhance` with `method="POST"`.
8. No server‑side shared mutable state; no side‑effects inside `load` functions.

---

## 12) References (official docs)

- **Runes**: `$state`, `$derived`, `$effect`, `$props`, `$bindable`, “What are runes?”
  - https://svelte.dev/docs/svelte/%24state
  - https://svelte.dev/docs/svelte/%24derived
  - https://svelte.dev/docs/svelte/%24effect
  - https://svelte.dev/docs/svelte/%24props
  - https://svelte.dev/docs/svelte/%24bindable
  - https://svelte.dev/docs/svelte/what-are-runes
- **Events**: runes mode vs legacy `on:`
  - https://svelte.dev/docs/svelte/legacy-on
  - Overview example uses `onclick`: https://svelte.dev/docs/svelte
- **Snippets & render**
  - https://svelte.dev/docs/svelte/snippet
  - https://svelte.dev/docs/svelte/%40render
- **Boundary**
  - https://svelte.dev/docs/svelte/svelte-boundary
- **SvelteKit**
  - Routing: https://svelte.dev/docs/kit/routing
  - Loading data: https://svelte.dev/docs/kit/load
  - Form actions & `use:enhance`: https://svelte.dev/docs/kit/form-actions
  - Remote Functions (2.27+): https://svelte.dev/docs/kit/remote-functions
  - State management: https://svelte.dev/docs/kit/state-management
  - Page options: https://svelte.dev/docs/kit/page-options
- **LLMs** (feed these to your toolchain)
  - https://svelte.dev/docs/llms
  - https://svelte.dev/docs/svelte/llms.txt
  - https://svelte.dev/docs/kit/remote-functions/llms.txt

---

### Appendix A — “LLM‑Starter Prompt” (drop‑in)

> **You are generating Svelte 5 + SvelteKit 2 code in runes mode.**  
> Follow these rules strictly:  
> – Use `$state`, `$derived`, `$effect`, `$props`, `$bindable`. No `$:` or `export let`.  
> – Use **snippets** `{#snippet}` + `{@render}`. Never embed snippets inside attributes. Treat optional snippets via `?.()` or `{#if}` fallback; do **not** invent `emptySnippet`.  
> – Use **DOM event attributes** (`onclick`/`oninput`/…); don’t use `on:`. Handle modifiers manually.  
> – When destructuring `$props()`, if there’s `...rest` it must be typed **`Record<string, any>`** (TS) or `Record<string, any>` in JSDoc.  
> – Two‑way bindings only through `$bindable` or `bind:`; otherwise prefer callbacks.  
> – In SvelteKit: data via `load`; forms via page **actions** + `use:enhance`; remote calls via **Remote Functions** if possible.  
> – If referencing another component, open its source and mirror its `$props` signature; never invent props.  
> – If unsure, output a `// TODO: verify in docs` comment — **don’t hallucinate**.
