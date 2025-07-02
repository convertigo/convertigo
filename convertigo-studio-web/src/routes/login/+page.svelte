<script>
	import { goto } from '$app/navigation';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{data: import('./$types').PageData}} */
	let { data } = $props();

	/** @type {string|null} */
	let error = $state(null);

	async function handleSubmit(e) {
		try {
			await Authentication.authenticate(e);
			if (Authentication.authenticated) {
				goto(`${data.redirect ?? '../admin'}`);
			} else {
				error = Authentication.error;
			}
		} catch (error) {
			error = '' + error;
		}
	}
</script>

<span class="fixed top-0 right-0 p-low"><LightSwitch></LightSwitch></span>
<div class="bg layout-y h-full justify-center">
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
</div>

<style>
	@property --angle {
		syntax: '<angle>';
		inherits: false;
		initial-value: 150deg;
	}
	@keyframes gradient {
		0% {
			background-position: 0% 50%;
			--angle: 150deg;
		}
		50% {
			background-position: 100% 50%;
			--angle: 250deg;
		}
		100% {
			background-position: 0% 50%;
			--angle: 150deg;
		}
	}
	.bg {
		background-image: linear-gradient(
			var(--angle),
			var(--color-tertiary-50),
			var(--color-secondary-50),
			var(--color-primary-100),
			var(--color-surface-50) 90%
		);
		background-size: 250% 250%;
		animation: gradient 20s ease infinite;
	}
	:global(.dark) .bg {
		background-image: linear-gradient(
			var(--angle),
			var(--color-tertiary-950),
			var(--color-secondary-950),
			var(--color-primary-900),
			var(--color-surface-950) 90%
		);
	}
</style>
