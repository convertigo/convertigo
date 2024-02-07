<script>
	import { TabGroup, Tab } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { call } from '$lib/utils/service';
	import { modeCurrent } from '@skeletonlabs/skeleton';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { authenticated } from '$lib/utils/loadingStore';

	import SizableCard from '$lib/shell/SizableCardCpy.svelte';
	import Monaco from '$lib/editor/Editor.svelte';
	import C8oTree from '$lib/treeview/TreeviewCpy.svelte';

	import Viewer from '$lib/viewer/Viewer.svelte';
	import NgxBuilder from '$lib/viewer/NgxBuilder.svelte';
	import Properties from '$lib/properties/Properties.svelte';
	import PaletteCpy from '$lib/palette/PaletteCpy.svelte';

	let editorTab = 0;
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

<div class="flex flex-row items-stretch h-full dark:bg-surface-900 bg-surface-50">
	{#if $treeSelected}
		<SizableCard name="tree">
			<C8oTree />
		</SizableCard>
	{/if}

	{#if $viewerSelected}
		<SizableCard name="viewer">
			<Viewer />
		</SizableCard>
	{/if}

	{#if $paletteSelected}
		<SizableCard name="palette">
			<PaletteCpy />
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

	{#if $propertiesSelected}
		<SizableCard name="properties">
			<Properties />
		</SizableCard>
	{/if}
</div>

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
