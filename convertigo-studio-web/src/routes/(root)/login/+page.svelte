<script>
	import { ProgressRing } from '@skeletonlabs/skeleton-svelte';
	import { goto } from '$app/navigation';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{data: import('./$types').PageData}} */
	let { data } = $props();

	/** @type {string|null} */
	let error = $state(null);

	let doAuthenticate = $state(false);

	async function handleSubmit(e) {
		try {
			doAuthenticate = true;
			await Authentication.authenticate(e);
			if (Authentication.authenticated) {
				goto(`${data.redirect ?? '../admin'}`);
			} else {
				error = Authentication.error;
			}
		} catch (error) {
			error = '' + error;
		} finally {
			doAuthenticate = false;
		}
	}
</script>

{#if doAuthenticate}
	<ProgressRing value={null} size="size-72">
		<span class="animate-pulse">Authenticating …</span>
	</ProgressRing>
{:else}
	<Card class="preset-glass-surface" bg="">
		<form onsubmit={handleSubmit} class="layout-y-m-center">
			<Ico icon="convertigo:logo" class="-m-5 text-primary-500" size="32" />
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

			<button class="button-primary w-full" type="submit">Enter</button>
		</form>
	</Card>
{/if}
