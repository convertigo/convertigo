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

<div class="h-full bg-surface-700 flex flex-col items-center">
	<h1 class="text-white text-center mt- text-4xl mt-20 margin leading-[40px] font-light">
		<p>Welcome to Convertigo</p>
		<p>Administration Console</p>
	</h1>
	<form
		onsubmit={handleSubmit}
		class="flex flex-col w-[600px] bg-surface h-80 mt-40 rounded-xl p-4 items-center"
	>
		<input type="hidden" name="authType" value="login" />
		<input
			name="authUserName"
			class="bg-white rounded-xl border-none mt-5 text-center text-surface-900 font-light placeholder:font-light"
			placeholder="username"
			type="text"
			autocomplete="username"
		/>

		<input
			name="authPassword"
			class="bg-white rounded-xl border-none mt-5 text-center text-surface-900 font-light placeholder:font-light"
			placeholder="password"
			type="password"
			autocomplete="current-password"
		/>

		{#if error}
			<div class="class variant-filled-error rounded-xl mt-5 py-1 px-4">{error}</div>
		{/if}

		<button type="submit" class="bg-surface-400 mt-10 font-light py-1 px-16 p rounded button"
			>Enter</button
		>
	</form>
</div>
