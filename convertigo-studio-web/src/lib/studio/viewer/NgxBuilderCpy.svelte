<script>
	import { run } from 'svelte/legacy';

	import { selectedId } from '$lib/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';
	import Icon from '@iconify/svelte';
	import { AppBar, AppRail, AppRailAnchor, AppShell, ProgressRadial } from '@skeletonlabs/skeleton';
	import { afterUpdate, onMount } from 'svelte';

	let project = $state('');
	let message = $state('');
	/** @type {WebSocket|null} */
	let ws = $state(null);

	/** @type {HTMLIFrameElement} */
	let iframe = $state();

	let iframeUrl = $state(null);
	let progress = $state(0);
	let output = $state('');

	onMount(() => {
		ws = new WebSocket(
			`${getUrl().replace(
				/^http/,
				'ws'
			)}studio.ngxbuilder.WsBuilder?__xsrfToken=${localStorage.getItem('x-xsrf')}`
		);
		ws.onopen = () => {
			ws = ws;
		};
		ws.onmessage = (msg) => {
			try {
				const { type, value } = JSON.parse(msg.data);
				switch (type) {
					case 'output':
						output = value;
					case 'log':
						message = message + `${value}\n`;
						break;
					case 'progress':
						progress = Number.parseFloat(value);
						break;
					case 'load':
						iframeUrl = value;
						break;
				}
			} catch (e) {
				message = message + `Exception: ${e}\n`;
			}
		};
		return () => {
			ws?.close();
			ws = null;
		};
	});

	let textarea = $state();
	let startY, startHeight;

	/** @param {MouseEvent} e*/
	function onDrag(e) {
		startY = e.clientY;
		startHeight = parseInt(document.defaultView?.getComputedStyle(textarea)?.height ?? '', 10);
		document.documentElement.addEventListener('mousemove', doDrag, false);
		document.documentElement.addEventListener('mouseup', stopDrag, false);
	}

	/** @param {MouseEvent} e*/
	function doDrag(e) {
		textarea.style.height = startHeight - (e.clientY - startY) + 'px';
	}

	function stopDrag() {
		document.documentElement.removeEventListener('mousemove', doDrag, false);
		document.documentElement.removeEventListener('mouseup', stopDrag, false);
	}

	afterUpdate(() => {
		if (textarea) {
			textarea.scrollTop = textarea.scrollHeight;
		}
	});

	function runAction(action) {
		progress = 0;
		iframeUrl = null;
		output = 'Not running';
		ws?.send(
			JSON.stringify({
				action,
				project
			})
		);
	}
	run(() => {
		project = $selectedId.replace(/(.*?)[\.:].*/, '$1');
	});
	run(() => {
		if (project != '' && ws?.readyState == WebSocket.OPEN) {
			message = '';
			runAction('attach');
		}
	});
</script>

{#if project == ''}
	<h1>No project selected!</h1>
{:else if message == ''}
	<h1>Checking for project <u>{project}</u>!</h1>
{/if}
{#if project != '' && message != ''}
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<AppShell>
		<svelte:fragment slot="header">
			<AppBar padding="p-1 pl-4" background="dark:bg-surface-500 bg-surface-50">
				<svelte:fragment slot="lead">
					<p>NGX Builder for {project}</p>
				</svelte:fragment>

				<svelte:fragment slot="header"></svelte:fragment>

				<svelte:fragment slot="trail">
					<button
						onclick={() => runAction('build_dev')}
						type="button"
						class="rounded-xl bg-surface-700 p-2">Run</button
					>
					<button
						onclick={() => runAction('kill')}
						type="button"
						class="rounded-xl bg-surface-700 p-2">Kill</button
					>
				</svelte:fragment>
			</AppBar>
		</svelte:fragment>

		<!--
		<svelte:fragment slot="sidebarLeft">
			<AppRail width="auto" class="p-2" background="bg-surface-900" >

				<AppRailAnchor on:click={() => runAction('build_dev')}>
					<button type="button" class="p-3 rounded-xl bg-surface-700">Run</button>
				</AppRailAnchor>
				<AppRailAnchor on:click={() => runAction('kill')}>
					<button type="button" class="p-3 rounded-xl bg-surface-700">Kill</button>
				</AppRailAnchor>
			</AppRail>
		</svelte:fragment> -->
		{#if iframeUrl == null}
			<div class="flex h-full w-full flex-col items-center justify-around">
				<ProgressRadial value={progress}>{progress}%</ProgressRadial>
				<div class="preset-ghost w-3/4 card py-1 text-center">{output}</div>
			</div>
		{:else}
			<iframe bind:this={iframe} class="h-full w-full" title="test" src={iframeUrl} />
		{/if}
		<svelte:fragment slot="footer">
			<div class="draggable w-full border-4" onmousedown={onDrag} />
			<textarea
				bind:this={textarea}
				readonly={true}
				class="h-full w-full resize-none bg-white dark:bg-gray-100">{message}</textarea
			>
		</svelte:fragment>
	</AppShell>
{/if}

<style lang="postcss">
	.draggable {
		cursor: grab;
	}
</style>
