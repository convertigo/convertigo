<script>
	import CacheForm from '$lib/admin-console/admin-components/CacheForm.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import {
		cacheClear,
		cacheConfig,
		cacheProperties,
		showCacheProperties,
		cacheType
	} from '$lib/admin-console/stores/cacheStore';
	import {
		Accordion,
		AccordionItem,
		initializeStores,
		localStorageStore
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	initializeStores();

	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		showCacheProperties();
		cacheConfig();
		cacheClear();

		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
	});

	export function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	function updateCacheType(newType) {
		cacheType.set(newType);
	}
</script>

<div class="p-10">
	<h1 class="mb-5 pb-2 border-1 border-b border-surface-100">Cache</h1>

	<Card>
		<h2>Cache type</h2>
		<p class="mt-5">Choose the desired cache type :</p>
		<div class="flex mt-5">
			<div class="flex items-center">
				<input
					type="radio"
					id="cacheTypeFile"
					on:change={() => updateCacheType('file')}
					value="file"
					checked={$cacheType === 'file'}
				/>
				<label for="cacheTypeFile" class="text-[14px] ml-2">file</label>
			</div>

			<div class="flex ml-10 items-center">
				<input
					id="cacheTypeDatabase"
					type="radio"
					value="database"
					on:change={() => updateCacheType('database')}
					checked={$cacheType === 'database'}
				/>
				<label for="cacheTypeDatabase" class="text-[14px] ml-2">database</label>
			</div>

			<button class="ml-10 p-0 bg-surface-100 pl-4 pr-4 btn variant-filled">Apply</button>
		</div>
	</Card>

	<Accordion width="w-[100%] mt-10 bg-surface-700">
		<AccordionItem open={$cacheType === 'database'}>
			<svelte:fragment slot="summary">Configurations</svelte:fragment>
			<svelte:fragment slot="content">
				<CacheForm />
			</svelte:fragment>
		</AccordionItem>
	</Accordion>
</div>
