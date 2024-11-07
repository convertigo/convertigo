<script>
	import { goto } from '$app/navigation';
	import { base } from '$app/paths';
	import Authentication from '$lib/common/Authentication.svelte';

	/** @type {{data: import('./$types').PageData}} */
	let { data } = $props();

	/** @type {string|null} */
	let error = $state(null);

	async function handleSubmit(/** @type {SubmitEvent} */ e) {
		e.preventDefault();
		try {
			// @ts-ignore
			await Authentication.authenticate(new FormData(e.target));
			if (Authentication.authenticated) {
				goto(`${base}${data.redirect ?? '/admin'}`);
			} else {
				error = Authentication.error;
			}
		} catch (error) {
			error = '' + error;
		}
	}
</script>

<div class="h-full layout-y-m !gap-5 justify-center">
	<h1 class="text-4xl text-center">
		<p>Welcome to Convertigo</p>
		<p>Administration Console</p>
	</h1>
	<form
		onsubmit={handleSubmit}
		class="layout-y-m !gap-5 w-[300px] max-w-full !items-stretch"
	>
		<input type="hidden" name="authType" value="login" />
		<input
			name="authUserName"
			class="input"
			placeholder="username"
			type="text"
			autocomplete="username"
		/>

		<input
			name="authPassword"
			class="input"
			placeholder="password"
			type="password"
			autocomplete="current-password"
		/>

		{#if error}
			<p class="preset-filled-error p-low rounded">{error}</p>
		{/if}

		<button type="submit" class="basic-button"
			>Enter</button
		>
	</form>
</div>
