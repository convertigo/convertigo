<script>
	import { authenticated } from '$lib/utils/loadingStore';
	import { call } from '$lib/utils/service';
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';

	initializeStores();

	let theme = localStorageStore('studio.theme', 'skeleton');

	let authUserName = '';
	let authPassword = '';

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
	});

	const handleSubmit = async () => {
		try {
			const response = await call('engine.Authenticate', { 
                authUserName: authUserName ,
                authPassword: authPassword,
                authType: 'login' 
            });
			if (!$authenticated) {
				goto('/convertigo/admin/');
			} else {
				console.error('Fail to authenticate');
			}
		} catch (error) {
			console.error('failure');
		}
	};

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}
</script>

<div class="h-full bg-surface-700 flex flex-col items-center">
	<h1 class="text-white text-center mt- text-4xl mt-20 margin leading-[40px] font-light">
		Welcome to Convertigo <br /> Administration Console
	</h1>

	<div class="flex flex-col w-[600px] bg-surface h-80 mt-40 rounded-xl p-4 items-center">
		<input
			class="bg-white rounded-xl border-none mt-5 text-center text-surface-900 font-light placeholder:font-light"
			placeholder="username"
			type="text"
		/>

		<input
			class="bg-white rounded-xl border-none mt-5 text-center text-surface-900 font-light placeholder:font-light"
			placeholder="password"
			type="password"
		/>

		<button on:click={handleSubmit} class="bg-surface-400 mt-10 font-light p-1 p rounded button"> Enter </button>
	</div>
</div>

<style>
	.button {
		padding-left: 16px;
		padding-right: 16px;
	}
</style>
