<script>
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailAnchor, AppShell, localStorageStore } from '@skeletonlabs/skeleton';
	// @ts-ignore
	import IconLogout from '~icons/mdi/logout';
	import { authenticated } from '$lib/utils/loadingStore';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';

	let treeSelected = localStorageStore('studio.treeSelected', false);
	let propertiesSelected = localStorageStore('studio.propertiesSelected', false);
	let paletteSelected = localStorageStore('studio.paletteSelected', false);
	let editorSelected = localStorageStore('studio.editorSelected', false);
	let viewerSelected = localStorageStore('studio.viewerSelected', false);
	let ngxbuilderSelected = localStorageStore('studio.ngxbuilderSelected', false);

	onMount(() => {
		call('engine.CheckAuthentication').then((res) => {
			$authenticated = res.admin.authenticated;
			if (!$authenticated) {
				if (!location.href.includes('/studio')) {
					sessionStorage.setItem('studioWebDev', 'true');
					location.href = '/convertigo/admin/login.html';
				} else {
					sessionStorage.setItem('studioWebDev', 'false');
					location.href = location.href.replace(/\/studio\/.*/, '/admin/login.html');
				}
			}
		});
	});

	/**
	 * @param {HTMLDivElement} node
	 */
	function withTransition(node, { duration }) {
		return {
			duration,
			css: (/** @type {any} */ t) => {
				let l = Math.round(linear(t) * node.clientWidth);
				return `
				width: ${l}px;
				min-width: ${l}px;
				opacity: ${t};
			`;
			}
		};
	}

	let value = 0;
</script>

<AppShell>
	<svelte:fragment slot="sidebarLeft"
		><AppRail
			width="w-[35px]"
			background="dark:bg-surface-900 bg-surface-200"
			class="border-1 border-r border-surface-300 dark:border-surface-500"
			gap="gap-10"
			spacing="mb-"
		>
			<AppRailAnchor
				selected={$treeSelected}
				active="dark:bg-surface-700 bg-surface-300"
				on:click={() => ($treeSelected = !$treeSelected)}
				><Icon
					icon="material-symbols-light:account-tree-outline"
					class="h-6 w-6 pl-1"
				/></AppRailAnchor
			>
			<AppRailAnchor
				selected={$propertiesSelected}
				active="dark:bg-surface-700 bg-surface-300"
				on:click={() => ($propertiesSelected = !$propertiesSelected)}
				><Icon
					icon="material-symbols-light:event-list-outline-sharp"
					class="h-6 w-6 pl-1"
				/></AppRailAnchor
			>
			<AppRailAnchor
				active="dark:bg-surface-700 bg-surface-300"
				selected={$paletteSelected}
				on:click={() => ($paletteSelected = !$paletteSelected)}
			>
				<Icon icon="material-symbols-light:add" class="h-8 w-8 pl-1" /></AppRailAnchor
			>
			<AppRailAnchor
				active="dark:bg-surface-700 bg-surface-300"
				selected={$viewerSelected}
				on:click={() => ($viewerSelected = !$viewerSelected)}
				><Icon
					icon="material-symbols-light:blur-medium-rounded"
					class="h-8 w-8 pl-1"
				/></AppRailAnchor
			>
			<AppRailAnchor
				active="dark:bg-surface-700 bg-surface-300"
				selected={$ngxbuilderSelected}
				on:click={() => ($ngxbuilderSelected = !$ngxbuilderSelected)}
				><Icon
					icon="material-symbols-light:blur-circular-outline"
					class="h-8 w-8 pl-1"
				/></AppRailAnchor
			>
			<AppRailAnchor
				active="dark:bg-surface-700 bg-surface-300"
				selected={$editorSelected}
				on:click={() => ($editorSelected = !$editorSelected)}
				><Icon icon="material-symbols-light:brush-outline" class="h-8 w-8 pl-1" /></AppRailAnchor
			>
			<svelte:fragment slot="trail">
				<AppRailAnchor rel="external" href="/auth/" title="Admin"
					><IconLogout class="h-8 w-8" /></AppRailAnchor
				>
			</svelte:fragment>
		</AppRail></svelte:fragment
	>
</AppShell>
