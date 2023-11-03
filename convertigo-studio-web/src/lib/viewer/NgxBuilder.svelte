<script>
	import { selectedId } from '$lib/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';
	import { onMount } from 'svelte';

	let project = '';
    let message = '';
    /** @type {WebSocket|null} */
    let ws = null;

	$: project = $selectedId.replace(
		/(.*?)[\.:].*/,
		'$1'
	);

    $: {
        if (project != '' && ws?.readyState == WebSocket.OPEN) {
            ws.send('changed to ' + project);
        }
    }

    onMount(() => {
        ws = new WebSocket(`${getUrl().replace(/^http/, 'ws')}studio.ngxbuilder.WsBuilder?__xsrfToken=${localStorage.getItem('x-xsrf-token')}`);
        ws.onopen = () => {
            ws?.send("hello " + project);
        };
        ws.onmessage = (msg) => {
            message = message + `${msg.data}\n`;
        };
        return () => {
            ws?.close();
            ws = null;
        };
    });
</script>

{#if project == ''}
    <h1>No project selected!</h1>
{:else if message != ''}
    <h1>Building project <u>{project}</u>!</h1>
{:else}
    <h1>Checking for project <u>{project}</u>!</h1>
{/if}
{#if message != ''}
    <textarea readonly={true} class="bg-white dark:bg-gray-700 h-full">{message}</textarea>
{/if}