<script>
	import {
		AppRail,
		AppRailAnchor,
		LightSwitch,
		AppBar,
		AppShell,
		TabGroup,
		Tab
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { call } from '$lib/utils/service';
	import { modeCurrent } from '@skeletonlabs/skeleton';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { loading, authenticated } from '$lib/utils/loadingStore';
	import { Toast, Modal, initializeStores } from '@skeletonlabs/skeleton';

	// @ts-ignore
	import IconCloud from '~icons/mdi/cloud-outline';
	// @ts-ignore
	import IconFileTree from '~icons/mdi/file-tree';
	// @ts-ignore
	import IconEditor from '~icons/mdi/application-edit-outline';
	// @ts-ignore
	import IconProperties from '~icons/mdi/list-box';
	// @ts-ignore
	import IconPalette from '~icons/mdi/palette-outline';
	// @ts-ignore
	import IconLogout from '~icons/mdi/logout';
	// @ts-ignore
	import IconEye from '~icons/mdi/eye';

	import SizableCard from '$lib/shell/SizableCard.svelte';
	import Monaco from '$lib/editor/Editor.svelte';
	import C8oTree from '$lib/treeview/Treeview.svelte';
	import Palette from '$lib/palette/Palette.svelte';
	import themes from '$lib/resources/themes.json';
	import Viewer from '$lib/viewer/Viewer.svelte';
	import NgxBuilder from '$lib/viewer/NgxBuilder.svelte';
	import Properties from '$lib/properties/Properties.svelte';

	initializeStores();

	let theme = localStorageStore('studio.theme', 'skeleton');
	let editorTab = 0;
	let treeSelected = localStorageStore('studio.treeSelected', false);
	let propertiesSelected = localStorageStore('studio.propertiesSelected', false);
	let paletteSelected = localStorageStore('studio.paletteSelected', false);
	let editorSelected = localStorageStore('studio.editorSelected', false);
	let viewerSelected = localStorageStore('studio.viewerSelected', false);
	let ngxbuilderSelected = localStorageStore('studio.ngxbuilderSelected', false);

	onMount(() => {
		changeTheme($theme);

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
	 * @param {any} e
	 */
	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

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
</script>

<Toast />
<Modal />

<AppShell>
	<svelte:fragment slot="header"
		><AppBar
			gridColumns="grid-cols-3"
			slotDefault="place-self-center"
			slotTrail="place-content-end"
		>
			<svelte:fragment slot="lead"
				><div class={$loading > 0 ? 'rotate' : ''} style="margin-left:10px">
					<IconCloud />
				</div></svelte:fragment
			>
			Low Code Studio
			<svelte:fragment slot="trail"
				><select on:change={changeTheme} class="select" value={$theme}>
					{#each themes as theme}
						<option>{theme}</option>
					{/each}
				</select>
				<LightSwitch /></svelte:fragment
			>
		</AppBar></svelte:fragment
	>
	<svelte:fragment slot="sidebarLeft"
		><AppRail>
			<svelte:fragment slot="lead">
				<AppRailAnchor selected={$treeSelected} on:click={() => ($treeSelected = !$treeSelected)}
					><IconFileTree /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$propertiesSelected}
					on:click={() => ($propertiesSelected = !$propertiesSelected)}
					><IconProperties /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$paletteSelected}
					on:click={() => ($paletteSelected = !$paletteSelected)}><IconPalette /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$viewerSelected}
					on:click={() => ($viewerSelected = !$viewerSelected)}><IconEye /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$ngxbuilderSelected}
					on:click={() => ($ngxbuilderSelected = !$ngxbuilderSelected)}><IconEye /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$editorSelected}
					on:click={() => ($editorSelected = !$editorSelected)}><IconEditor /></AppRailAnchor
				>
			</svelte:fragment>
			<svelte:fragment slot="trail">
				<AppRailAnchor rel="external" href="/convertigo/admin/" title="Admin"
					><IconLogout /></AppRailAnchor
				>
			</svelte:fragment>
		</AppRail></svelte:fragment
	>
	<!-- (sidebarRight) -->
	<!-- (pageHeader) -->
	<!-- Router Slot -->
	<div class="flex flex-row items-stretch h-full">
		{#if $treeSelected}
			<SizableCard name="tree">
				<C8oTree />
			</SizableCard>
		{/if}
		{#if $propertiesSelected}
			<SizableCard name="properties">
				<Properties />
			</SizableCard>
		{/if}
		{#if $paletteSelected}
			<SizableCard name="palette">
				<Palette />
			</SizableCard>
		{/if}
		{#if $viewerSelected}
			<SizableCard name="viewer">
				<Viewer />
			</SizableCard>
		{/if}
		{#if $ngxbuilderSelected}
			<SizableCard name="ngxbuilder">
				<NgxBuilder />
			</SizableCard>
		{/if}
		{#if $editorSelected}
			<div
				class="grow card m-1 variant-soft-success h-full"
				style="height: calc(100% - 8px);"
				transition:withTransition={{ duration: 250 }}
			>
				<TabGroup class="w-full h-full flex flex-col" regionPanel="grow">
					<Tab bind:group={editorTab} name="file1" value={0}>file1.txt</Tab>
					<Tab bind:group={editorTab} name="file2" value={1}>file2.txt</Tab>
					<svelte:fragment slot="panel">
						<div class="w-full h-full grow flex">
							<Monaco
								content="console.log('hello');"
								readOnly={false}
								language="typescript"
								theme={$modeCurrent ? 'vs' : 'vs-dark'}
							/>
						</div>
					</svelte:fragment>
				</TabGroup>
			</div>
		{/if}
	</div>
	<!-- ---- / ---- -->
	<!-- (pageFooter) -->
	<!-- (footer) -->
</AppShell>

<style>
	@keyframes rotate {
		from {
			transform: rotateY(0deg);
		}
		to {
			transform: rotateY(360deg);
		}
	}

	.rotate {
		animation: rotate 0.5s infinite linear;
	}

	:global(#page-content) {
		overflow-y: hidden;
	}

	:global(.input) {
		padding: 0px 10px;
	}
</style>
