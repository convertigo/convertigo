<script>
	import { goto } from '$app/navigation';
	import Button from '$lib/admin/components/Button.svelte';
	import Authentication from '$lib/common/Authentication.svelte';

	/** @type {{data: import('./$types').PageData}} */
	let { data } = $props();

	/** @type {string|null} */
	let error = $state(null);

	async function handleSubmit(e) {
		try {
			await Authentication.authenticate(e);
			if (Authentication.authenticated) {
				goto(`${data.redirect ?? '/admin'}`);
			} else {
				error = Authentication.error;
			}
		} catch (error) {
			error = '' + error;
		}
	}
</script>

<div class="layout-y-m h-full justify-center gap-5!">
	<h1 class="text-center text-4xl">
		<p>Welcome to Convertigo</p>
		<p>Administration Console</p>
	</h1>
	<form onsubmit={handleSubmit} class="layout-y-m-stretch w-[300px] max-w-full gap-5!">
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
			<p class="rounded-sm preset-filled-error p-low">{error}</p>
		{/if}

		<Button label="Enter" class="basic-button" type="submit" />
	</form>
</div>
