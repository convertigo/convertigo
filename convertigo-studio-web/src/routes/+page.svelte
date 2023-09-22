<script>
	import {
		AppRail,
		AppRailAnchor,
		ProgressRadial,
		LightSwitch,
		AppBar,
		AppShell,
		TabGroup,
		Tab
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { callService } from '$lib/convertigo';
	import { modeCurrent } from '@skeletonlabs/skeleton';

	// @ts-ignore
	import IconCloud from '~icons/mdi/cloud-outline';
	// @ts-ignore
	import IconFileTree from '~icons/mdi/file-tree';
	// @ts-ignore
	import IconEditor from '~icons/mdi/application-edit-outline';
	// @ts-ignore
	import IconProperties from '~icons/mdi/list-box';
	// @ts-ignore
	import IconLogout from '~icons/mdi/logout';
	import Monaco from '$lib/Monaco.svelte';
	import C8oTree from '$lib/C8oTree.svelte';
	import { properties } from '$lib/propertiesStore';

	let currentTile = 0;

	let treeWidth = 0;
	let propertiesWidth = 0;
	let editorTab = 0;
	let treeSelected = true;
	let propertiesSelected = true;
	let editorSelected = true;
	let authenticated = false;
	/**
	 * @type {HTMLImageElement}
	 */
	let img;
	onMount(() => {
		img = document.createElement('img');
		img.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
		// @ts-ignore
		treeWidth = 1 * (localStorage.getItem('treeWidth') ?? '100');
		// @ts-ignore
		propertiesWidth = 1 * (localStorage.getItem('propertiesWidth') ?? '100');

		callService('engine.CheckAuthentication').then((res) => {
			authenticated = res.admin.authenticated;
			if (!authenticated) {
				if (!location.href.includes('/studio')) {
					sessionStorage.setItem('studioWebDev', 'true');
					location.href = '/convertigo/admin/login.html'
				} else {
					sessionStorage.setItem('studioWebDev', 'false');
					location.href = location.href.replace(/\/studio\/.*/, '/admin/login.html');
				}
			}
		});
	});

	/**
	 * @param {{ layerX: number; x: number; target: { parentElement: { offsetLeft: number; }; }; }} e
	 */
	function treeWidthDrag(e) {
		if (e.layerX > 0) {
			treeWidth = e.x - e.target.parentElement.offsetLeft;
			localStorage.setItem('treeWidth', `${treeWidth}`);
		}
	}

	/**
	 * @param {{ layerX: number; x: number; target: { parentElement: { offsetLeft: number; }; }; }} e
	 */
	function propertiesWidthDrag(e) {
		if (e.layerX > 0) {
			propertiesWidth = e.x - e.target.parentElement.offsetLeft;
			localStorage.setItem('propertiesWidth', `${propertiesWidth}`);
		}
	}

	function withTransition(node, { duration }) {
		return {
			duration,
			css: (t) => {
				let l = Math.round(linear(t) * node.clientWidth);
				return `
					width: ${l}px;
					min-width: ${l}px;
					opacity: ${t};
				`;
			}
		};
	}

	// @ts-ignore
	function noDragImage(e) {
		e.target.parentElement.parentElement.classList.remove('widthTransition');
		e.dataTransfer.setDragImage(img, 0, 0);
	}

	function changeTheme(e) {
		document.body.setAttribute('data-theme', e.target.value);
	}

	let treeNodes = [
		{
			content: 'Please update'
		}
	];

	async function update() {
		let json = await callService('engine.Authenticate', {
			authType: 'login',
			authUserName: 'admin',
			authPassword: 'admin'
		});
		json = await callService('tree.Get');
		console.log('json: ' + JSON.stringify(json));
		treeNodes = json.children.map((p) => {
			return {
				content: p.label,
				children: p.children
			};
		});
	}

	const themes = [
		{
			name: 'skeleton',
			enhancements: true
		},
		{
			name: 'wintry',
			enhancements: true
		},
		{
			name: 'modern',
			enhancements: true
		},
		{
			name: 'hamlindigo',
			enhancements: true
		},
		{
			name: 'rocket',
			enhancements: true
		},
		{
			name: 'sahara',
			enhancements: true
		},
		{
			name: 'gold-nouveau',
			enhancements: true
		},
		{
			name: 'vintage',
			enhancements: true
		},
		{
			name: 'seafoam',
			enhancements: true
		},
		{
			name: 'crimson',
			enhancements: true
		}
	];
</script>

<AppShell>
	<svelte:fragment slot="header"
		><AppBar
			gridColumns="grid-cols-3"
			slotDefault="place-self-center"
			slotTrail="place-content-end"
		>
			<svelte:fragment slot="lead"><IconCloud style="margin-left:10px" /></svelte:fragment>
			Low Code Studio
			<svelte:fragment slot="trail"
				><select on:change={changeTheme} class="select">
					{#each themes as theme}
						<option>{theme.name}</option>
					{/each}
				</select>
				<LightSwitch /></svelte:fragment
			>
		</AppBar></svelte:fragment
	>
	<svelte:fragment slot="sidebarLeft"
		><AppRail>
			<svelte:fragment slot="lead">
				<AppRailAnchor selected={treeSelected} on:click={() => (treeSelected = !treeSelected)}
					><IconFileTree /></AppRailAnchor
				>
				<AppRailAnchor
					selected={propertiesSelected}
					on:click={() => (propertiesSelected = !propertiesSelected)}
					><IconProperties /></AppRailAnchor
				>
				<AppRailAnchor selected={editorSelected} on:click={() => (editorSelected = !editorSelected)}
					><IconEditor /></AppRailAnchor
				>
				<AppRailAnchor
					selected={propertiesSelected}
					on:click={async () => {
						console.log(await callService('projects.List'));
					}}><IconProperties /></AppRailAnchor
				>
			</svelte:fragment>
			<svelte:fragment slot="trail">
				<AppRailAnchor href='/convertigo/admin' title="Admin"><IconLogout /></AppRailAnchor>
			</svelte:fragment>
		</AppRail></svelte:fragment
	>
	<!-- (sidebarRight) -->
	<!-- (pageHeader) -->
	<!-- Router Slot -->
	<div class="flex flex-row items-stretch h-full">
		{#if treeSelected}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<div
				class="card m-1 variant-soft-primary overflow-hidden widthTransition"
				style:width="{treeWidth}px"
				style:min-width="100px"
				on:drag={treeWidthDrag}
				on:dragstart={noDragImage}
				transition:withTransition={{ duration: 250 }}
			>
				<div class="flex flex-row items-stretch h-full">
					<div
						class="flex-col flex items-stretch grow scroll-smooth overflow-y-auto snap-y scroll-px-4 snap-mandatory"
					>
						{#if authenticated}
							<C8oTree />
						{:else}
							<ProgressRadial
								...
								stroke={100}
								meter="stroke-primary-500"
								track="stroke-primary-500/30"
							/>
						{/if}
					</div>
					<span class="draggable divider-vertical h-full border-2" draggable="true" />
				</div>
			</div>
		{/if}
		{#if propertiesSelected}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<div
				class="card m-1 variant-soft-primary overflow-hidden widthTransition"
				style:width="{propertiesWidth}px"
				style:min-width="100px"
				on:drag={propertiesWidthDrag}
				on:dragstart={noDragImage}
				transition:withTransition={{ duration: 250 }}
			>
				<div class="flex flex-row items-stretch h-full">
					<!-- Responsive Container (recommended) -->
					<div class="table-container">
						<!-- Native Table Element -->
						<table class="table table-hover table-compact">
							<thead>
								<tr>
									<th>Name</th>
									<th>Value</th>
								</tr>
							</thead>
							<tbody>
								{#each Object.entries($properties) as entry}
									<tr>
										<td>{entry[0]}</td>
										<td
											><input
												class="input"
												type="text"
												placeholder={entry[0]}
												value={entry[1]}
											/></td
										>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
					<span class="draggable divider-vertical h-full border-2" draggable="true" />
				</div>
			</div>
		{/if}
		{#if editorSelected}
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
	.draggable {
		cursor: grab;
	}
	.card:active {
		cursor: grabbing;
	}
	.widthTransition {
		transition-property: min-width;
		transition-duration: 250ms;
	}

	:global(#page-content) {
		overflow-y: hidden;
	}

	:global(.input) {
		padding: 0px 10px;
	}
</style>
