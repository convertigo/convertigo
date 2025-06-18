<script>
	import { goto } from '$app/navigation';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import Ico from '$lib/utils/Ico.svelte';

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

<div class="layout-y h-full justify-center">
	<Ico icon="convertigo:logo" class="fixed z-0 text-primary-100-900" size="fit" />
	<Card class="preset-glass-surface z-1" bg="">
		<form onsubmit={handleSubmit} class="layout-y-m-stretch">
			<h1 class="text-center text-3xl">
				<p>Welcome to Convertigo</p>
				<p>Administration Console</p>
			</h1>
			<input type="hidden" name="authType" value="login" />
			<PropertyType name="authUserName" placeholder="username" autocomplete="username" />

			<PropertyType
				name="authPassword"
				placeholder="password"
				type="password"
				autocomplete="current-password"
			/>

			{#if error}
				<p class="rounded-sm preset-filled-error-500 p-low">{error}</p>
			{/if}

			<Button label="Enter" class="button-primary" type="submit" />
		</form>
	</Card>
</div>
