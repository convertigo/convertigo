<script>
	import QrCode from '#lib/common/components/QrCode.svelte';
	import Ico from '#lib/utils/Ico.svelte';
	import { isLocalAddress, publicFrontendUrl } from './frontendShare.js';

	/**
	 * The QR code of the application shown, to open it on a phone: the development server through the gateway follows
	 * the edits of the Studio (hot reload), the production application is the built one.
	 * @type {{ resolveUrl: () => string, available?: boolean, buttonClass?: string, title?: string }}
	 */
	let {
		resolveUrl,
		available = true,
		buttonClass = '',
		title = 'QR code of the application, to open it on a phone'
	} = $props();
	let open = $state(false);
	let url = $state('');
	let copied = $state(false);
	let root = $state();

	function toggle() {
		if (!open) {
			url = publicFrontendUrl(resolveUrl());
			copied = false;
		}
		open = !open && Boolean(url);
	}

	async function copy() {
		try {
			await navigator.clipboard.writeText(url);
			copied = true;
		} catch {
			copied = false;
		}
	}

	/** the popup closes on Escape and on a click outside of it, as a menu */
	function closeOutside(event) {
		if (!open) return;
		if (event.type === 'keydown' ? event.key === 'Escape' : !root?.contains(event.target)) {
			open = false;
		}
	}
</script>

<svelte:window onkeydown={closeOutside} onpointerdown={closeOutside} />

<span class="share-qr" bind:this={root}>
	<button
		type="button"
		class={buttonClass}
		{title}
		aria-label="QR code"
		aria-expanded={open}
		disabled={!available}
		onclick={toggle}
	>
		<Ico icon="mdi:qrcode" size={4} />
	</button>
	{#if open}
		<div class="share-qr__popup" role="dialog" aria-label="QR code of the application">
			<QrCode href={url} link={false} class="share-qr__image" alt={url} />
			<a class="share-qr__url" href={url} target="_blank" rel="noopener">{url}</a>
			{#if isLocalAddress(url)}
				<p class="share-qr__warning">
					This address only reaches this machine: open the Studio with the address of the server to
					share it.
				</p>
			{/if}
			<button type="button" class="button-secondary" onclick={copy}>
				<Ico icon={copied ? 'mdi:check' : 'mdi:content-copy'} size={4} />
				{copied ? 'Copied' : 'Copy the address'}
			</button>
		</div>
	{/if}
</span>

<style>
	.share-qr {
		position: relative;
		display: inline-flex;
	}
	.share-qr__popup {
		position: absolute;
		top: calc(100% + 0.25rem);
		right: 0;
		z-index: 50;
		display: grid;
		gap: 0.5rem;
		justify-items: center;
		width: 15rem;
		padding: 0.75rem;
		border-radius: 0.5rem;
		background: var(--color-surface-50-950);
		box-shadow: 0 0.5rem 1.5rem rgb(0 0 0 / 0.2);
	}
	.share-qr__popup :global(.share-qr__image) {
		width: 12rem;
		height: 12rem;
		image-rendering: pixelated;
	}
	.share-qr__url {
		max-width: 100%;
		font-size: 0.7rem;
		overflow-wrap: anywhere;
		text-align: center;
	}
	.share-qr__warning {
		margin: 0;
		font-size: 0.7rem;
		color: var(--color-warning-600-400);
		text-align: center;
	}
</style>
