<script>
	import { asset } from '$app/paths';
	import Button from '$lib/admin/components/Button.svelte';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import Ico from '$lib/utils/Ico.svelte';
	import { getFrontendUrl } from '$lib/utils/service';
	import StudioDevicePanel from './StudioDevicePanel.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';

	const familyDefinitions = [
		{
			id: 'responsive',
			title: 'Responsive',
			match: (device) => device.id === 'none'
		},
		{
			id: 'phone',
			title: 'Phones',
			match: (device) => device.type === 'phone'
		},
		{
			id: 'tablet',
			title: 'Tablets',
			match: (device) => device.type === 'tablet'
		},
		{
			id: 'desktop',
			title: 'Desktop',
			match: (device) => device.type === 'desktop' && device.id !== 'none'
		}
	];
	const ZOOM_STEP = 0.15;
	const MIN_ZOOM = 0.4;
	const MAX_ZOOM = 2.5;
	const FIT_PADDING = 24;
	const iconButtonClasses = 'button-ico-secondary h-8! w-8! justify-center p-0!';

	/** @type {{ projectName?: string, previewUrlOverride?: string, previewMode?: 'production' | 'development', selectedDeviceId?: string, landscape?: boolean, showDeviceSelector?: boolean, showDeviceDrawer?: boolean }} */
	let {
		projectName = '',
		previewUrlOverride = '',
		previewMode = 'production',
		selectedDeviceId = $bindable('none'),
		landscape = $bindable(false),
		showDeviceSelector = true,
		showDeviceDrawer = false
	} = $props();

	/** @type {HTMLIFrameElement | undefined} */
	let iframe = $state();
	let clientHeight = $state(0);
	let clientWidth = $state(0);
	let addressOverride = $state({ base: '', value: '' });
	let iframeOverride = $state({ base: '', value: '' });
	let zoomOverride = $state({ base: '', value: 1 });
	let zoomModeOverride = $state({ base: '', value: 'fit' });
	let deviceDrawerOpen = $state(false);
	let previewUrl = $derived(previewUrlOverride || (projectName ? getFrontendUrl(projectName) : ''));
	let addressBar = $derived(
		addressOverride.base === previewUrl ? addressOverride.value : previewUrl
	);
	let iframeUrl = $derived(iframeOverride.base === previewUrl ? iframeOverride.value : previewUrl);
	let zoom = $derived(zoomOverride.base === previewUrl ? zoomOverride.value : 1);
	let zoomMode = $derived(zoomModeOverride.base === previewUrl ? zoomModeOverride.value : 'fit');
	let trimmedAddress = $derived(addressBar.trim());
	let deviceGroups = $derived.by(buildDeviceGroups);
	let selectedDevice = $derived(Bezels[selectedDeviceId] ?? Bezels.none);
	let isResponsivePreview = $derived(selectedDevice.id === 'none');
	let deviceViewportWidth = $derived(getDeviceMetric(selectedDevice.iframe?.width, 1280));
	let deviceViewportHeight = $derived(getDeviceMetric(selectedDevice.iframe?.height, 800));
	let viewportWidth = $derived(
		isResponsivePreview ? 0 : landscape ? deviceViewportHeight : deviceViewportWidth
	);
	let viewportHeight = $derived(
		isResponsivePreview ? 0 : landscape ? deviceViewportWidth : deviceViewportHeight
	);
	let isFramedDevice = $derived(!isResponsivePreview && !landscape);
	let frameWidth = $derived(
		isFramedDevice ? getDeviceMetric(selectedDevice.bezel?.width, viewportWidth) : viewportWidth
	);
	let frameHeight = $derived(
		isFramedDevice ? getDeviceMetric(selectedDevice.bezel?.height, viewportHeight) : viewportHeight
	);
	let fitScale = $derived.by(() => {
		if (isResponsivePreview) {
			return 1;
		}
		if (!clientWidth || !clientHeight || !frameWidth || !frameHeight) {
			return 0.45;
		}
		const availableWidth = Math.max(1, clientWidth - FIT_PADDING);
		const availableHeight = Math.max(1, clientHeight - FIT_PADDING);
		return Math.max(0.08, Math.min(1, availableWidth / frameWidth, availableHeight / frameHeight));
	});
	let appliedScale = $derived(zoomMode === 'fit' ? fitScale : fitScale * zoom);
	let zoomLabel = $derived(zoomMode === 'fit' ? 'Fit' : `${Math.round(zoom * 100)}%`);
	let viewportLabel = $derived(
		isResponsivePreview
			? 'Responsive'
			: `${selectedDevice.title}${landscape ? ' landscape' : ''} - ${viewportWidth}x${viewportHeight}`
	);
	let fitButtonClasses = $derived(
		[iconButtonClasses, zoomMode === 'fit' && 'studio-preview__icon-button--active']
			.filter(Boolean)
			.join(' ')
	);
	let deviceButtonClasses = $derived(
		[iconButtonClasses, deviceDrawerOpen && 'studio-preview__icon-button--active']
			.filter(Boolean)
			.join(' ')
	);
	let previewStyle = $derived(
		[
			`--studio-preview-scale:${appliedScale}`,
			frameWidth ? `--studio-preview-width:${frameWidth}px` : '',
			frameHeight ? `--studio-preview-height:${frameHeight}px` : '',
			frameWidth ? `--studio-preview-render-width:${Math.round(frameWidth * appliedScale)}px` : '',
			frameHeight
				? `--studio-preview-render-height:${Math.round(frameHeight * appliedScale)}px`
				: '',
			viewportWidth ? `--studio-preview-frame-width:${viewportWidth}px` : '',
			viewportHeight ? `--studio-preview-frame-height:${viewportHeight}px` : '',
			isFramedDevice
				? `--studio-preview-frame-top:${getDeviceMetric(selectedDevice.iframe?.marginTop, 0)}px`
				: '',
			isFramedDevice
				? `--studio-preview-frame-left:${getDeviceMetric(selectedDevice.iframe?.marginLeft, 0)}px`
				: '',
			isFramedDevice
				? `--studio-preview-frame-radius:${getDeviceMetric(selectedDevice.iframe?.borderRadius, 0)}px`
				: ''
		]
			.filter(Boolean)
			.join(';')
	);
	function applyAddressBar() {
		if (!trimmedAddress || trimmedAddress === '#') {
			return;
		}
		iframeOverride = { base: previewUrl, value: trimmedAddress };
		addressOverride = { base: previewUrl, value: trimmedAddress };
	}

	function reloadIframe() {
		try {
			iframe?.contentWindow?.location?.reload();
		} catch (error) {
			console.warn('Unable to reload iframe', error);
		}
	}

	function navigateBack() {
		try {
			iframe?.contentWindow?.history?.back();
		} catch (error) {
			console.warn('Unable to navigate iframe back', error);
		}
	}

	function navigateForward() {
		try {
			iframe?.contentWindow?.history?.forward();
		} catch (error) {
			console.warn('Unable to navigate iframe forward', error);
		}
	}

	/**
	 * @param {number} direction
	 */
	function adjustZoom(direction) {
		const currentZoom = zoomMode === 'fit' ? 1 : zoom;
		setZoom(
			Math.max(
				MIN_ZOOM,
				Math.min(MAX_ZOOM, Math.round((currentZoom + direction * ZOOM_STEP) * 100) / 100)
			)
		);
	}

	/**
	 * @param {number} value
	 */
	function setZoom(value) {
		zoomModeOverride = { base: previewUrl, value: 'manual' };
		zoomOverride = { base: previewUrl, value };
	}

	function fitPreview() {
		zoomModeOverride = { base: previewUrl, value: 'fit' };
		zoomOverride = { base: previewUrl, value: 1 };
	}

	/**
	 * @param {Event} event
	 */
	function selectDevice(event) {
		const select = /** @type {HTMLSelectElement | null} */ (event.currentTarget);
		selectedDeviceId = select?.value || 'none';
		fitPreview();
		if (selectedDeviceId === 'none') {
			landscape = false;
		}
	}

	function toggleLandscape() {
		if (isResponsivePreview) {
			return;
		}
		landscape = !landscape;
		fitPreview();
	}

	function toggleDeviceDrawer() {
		deviceDrawerOpen = !deviceDrawerOpen;
	}

	function closeDeviceDrawer() {
		deviceDrawerOpen = false;
	}

	function handleWindowKeydown(event) {
		if (event.key === 'Escape' && deviceDrawerOpen) {
			closeDeviceDrawer();
		}
	}

	/**
	 * @param {Event} event
	 */
	function updateAddressBar(event) {
		const input = /** @type {HTMLInputElement | null} */ (event.currentTarget);
		addressOverride = { base: previewUrl, value: input?.value ?? '' };
	}

	/**
	 * @param {HTMLIFrameElement} node
	 */
	function registerIframe(node) {
		iframe = node;
		return () => {
			if (iframe === node) {
				iframe = undefined;
			}
		};
	}

	/**
	 * @param {unknown} value
	 * @param {number} fallback
	 * @returns {number}
	 */
	function getDeviceMetric(value, fallback) {
		return Number.isFinite(value) ? Number(value) : fallback;
	}

	function buildDeviceGroups() {
		const devices = Object.values(Bezels)
			.filter(Boolean)
			.sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
		const remaining = [...devices];
		/** @type {{ id: string, title: string, devices: any[] }[]} */
		const groups = [];
		for (const family of familyDefinitions) {
			const familyDevices = [];
			for (let index = remaining.length - 1; index >= 0; index -= 1) {
				const device = remaining[index];
				if (!family.match(device)) {
					continue;
				}
				familyDevices.push(device);
				remaining.splice(index, 1);
			}
			if (familyDevices.length) {
				familyDevices.sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
				groups.push({ id: family.id, title: family.title, devices: familyDevices });
			}
		}
		return groups;
	}
</script>

<svelte:window onkeydown={handleWindowKeydown} />

<div class="studio-preview" style={previewStyle}>
	{#if previewUrl}
		<form
			class="studio-preview__bar"
			onsubmit={(event) => {
				event.preventDefault();
				applyAddressBar();
			}}
		>
			<div class="studio-preview__nav layout-x-none">
				<Button
					full={false}
					icon="mdi:arrow-left"
					class={iconButtonClasses}
					title="Go back"
					ariaLabel="Go back"
					onclick={navigateBack}
				/>
				<Button
					full={false}
					icon="mdi:arrow-right"
					class={iconButtonClasses}
					title="Go forward"
					ariaLabel="Go forward"
					onclick={navigateForward}
				/>
				<Button
					full={false}
					icon="mdi:reload"
					class={iconButtonClasses}
					title="Reload"
					ariaLabel="Reload"
					onclick={reloadIframe}
				/>
				{#if showDeviceDrawer}
					<Button
						full={false}
						icon="mdi:devices"
						class={deviceButtonClasses}
						title={deviceDrawerOpen ? 'Close device drawer' : 'Choose preview device'}
						ariaLabel={deviceDrawerOpen ? 'Close device drawer' : 'Choose preview device'}
						aria-expanded={deviceDrawerOpen}
						aria-controls="studio-preview-device-drawer"
						onclick={toggleDeviceDrawer}
					/>
				{/if}
			</div>

			<input
				type="text"
				value={addressBar}
				oninput={updateAddressBar}
				placeholder="URL"
				class="studio-preview__address"
			/>

			<div class="studio-preview__actions layout-x-end-none">
				<span
					class="studio-preview__mode"
					class:studio-preview__mode--development={previewMode === 'development'}
				>
					{previewMode === 'development' ? 'Dev' : 'Prod'}
				</span>
				<Button
					full={false}
					label="Go"
					class="button-primary h-8! w-fit! px-3!"
					disabled={!trimmedAddress || trimmedAddress === '#'}
					onclick={applyAddressBar}
				/>
				{#if showDeviceSelector}
					<label class="studio-preview__device-select layout-x-low">
						{#if isResponsivePreview}
							<Ico icon="mdi:devices" size={4} />
						{:else}
							<img
								class="studio-preview__device-thumb"
								src={asset(`/bezels/thumbnails/${selectedDevice.id}.webp`)}
								alt=""
								loading="lazy"
							/>
						{/if}
						<select value={selectedDeviceId} aria-label="Preview device" onchange={selectDevice}>
							{#each deviceGroups as group (group.id)}
								<optgroup label={group.title}>
									{#each group.devices as device (device.id)}
										<option value={device.id}
											>{device.id === 'none' ? 'Responsive' : device.title}</option
										>
									{/each}
								</optgroup>
							{/each}
						</select>
					</label>
				{/if}
				<span class="studio-preview__size studio-ellipsis">{viewportLabel}</span>
				<Button
					full={false}
					icon="mdi:magnify-minus-outline"
					class={iconButtonClasses}
					title="Zoom out"
					ariaLabel="Zoom out"
					onclick={() => adjustZoom(-1)}
				/>
				<Button
					full={false}
					icon="mdi:fit-to-page-outline"
					class={fitButtonClasses}
					title="Fit to screen"
					ariaLabel="Fit to screen"
					onclick={fitPreview}
				/>
				<Button
					full={false}
					icon="mdi:magnify-plus-outline"
					class={iconButtonClasses}
					title="Zoom in"
					ariaLabel="Zoom in"
					onclick={() => adjustZoom(1)}
				/>
				<span class="studio-preview__zoom">{zoomLabel}</span>
				<Button
					full={false}
					icon="mdi:camera-rotate-outline"
					class={iconButtonClasses}
					title="Rotate viewport"
					ariaLabel="Rotate viewport"
					disabled={isResponsivePreview}
					onclick={toggleLandscape}
				/>
				<Button
					full={false}
					href={trimmedAddress || previewUrl}
					target="_blank"
					rel="noreferrer"
					icon="mdi:open-in-new-variant"
					class={iconButtonClasses}
					title="Open frontend"
					ariaLabel="Open frontend"
				/>
			</div>
		</form>

		{#if showDeviceDrawer}
			{#if deviceDrawerOpen}
				<button
					type="button"
					class="studio-preview__drawer-backdrop"
					aria-label="Close device drawer"
					onclick={closeDeviceDrawer}
				></button>
			{/if}
			<aside
				id="studio-preview-device-drawer"
				class="studio-preview__device-drawer"
				class:studio-preview__device-drawer--open={deviceDrawerOpen}
				aria-label="Preview devices"
				aria-hidden={!deviceDrawerOpen}
				inert={!deviceDrawerOpen}
			>
				<StudioDevicePanel bind:selectedDeviceId bind:landscape onSelect={closeDeviceDrawer} />
			</aside>
		{/if}

		<MaxRectangle bind:clientHeight bind:clientWidth class="studio-preview__viewport">
			<div class="studio-preview__center">
				<div
					class={[
						'studio-preview__scaled-box',
						isResponsivePreview && 'studio-preview__scaled-box--fluid'
					]}
				>
					<div
						class={[
							'studio-preview__stage',
							isResponsivePreview && 'studio-preview__stage--responsive',
							isFramedDevice && 'studio-preview__stage--framed',
							!isResponsivePreview && !isFramedDevice && 'studio-preview__stage--viewport'
						]}
					>
						<iframe
							{@attach registerIframe}
							class={['studio-preview__frame', isFramedDevice && 'studio-preview__frame--framed']}
							title={`${projectName} frontend`}
							src={iframeUrl}
						></iframe>
						{#if isFramedDevice}
							<picture class="studio-preview__bezel" aria-hidden="true">
								<source srcset={asset(`/bezels/${selectedDevice.id}.webp`)} type="image/webp" />
								<img src={asset(`/bezels/${selectedDevice.id}.webp`)} alt="" loading="lazy" />
							</picture>
						{/if}
					</div>
				</div>
			</div>
		</MaxRectangle>
	{:else}
		<StudioEmptyState message="No project selected" class="studio-preview__empty" />
	{/if}
</div>

<style>
	.studio-preview {
		position: relative;
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		background: var(--color-surface-100-900);
	}

	.studio-preview__drawer-backdrop {
		position: absolute;
		z-index: 18;
		inset: 2.8rem 0 0;
		border: 0;
		background: color-mix(in oklab, var(--color-surface-950-50) 18%, transparent);
		cursor: default;
	}

	.studio-preview__device-drawer {
		position: absolute;
		z-index: 20;
		top: 2.8rem;
		bottom: 0;
		left: 0;
		width: min(22rem, calc(100% - 2rem));
		overflow: hidden;
		border-right: 1px solid var(--color-surface-200-800);
		background: var(--color-surface-50-950);
		box-shadow: var(--shadow-follow);
		transform: translateX(-102%);
		transition: transform 180ms ease;
	}

	.studio-preview__device-drawer--open {
		transform: translateX(0);
	}

	@media (prefers-reduced-motion: reduce) {
		.studio-preview__device-drawer {
			transition: none;
		}
	}

	.studio-preview__bar {
		display: grid;
		grid-template-columns: auto minmax(12rem, 1fr) auto;
		align-items: center;
		gap: 0.35rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 88%, transparent);
		padding: 0.38rem;
	}

	.studio-preview__nav,
	.studio-preview__actions {
		min-width: 0;
	}

	.studio-preview__device-select {
		min-width: 10rem;
		height: 2rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 78%, transparent);
		color: var(--color-surface-700-300);
		padding: 0 0.45rem;
	}

	.studio-preview__device-select select {
		min-width: 0;
		max-width: 12rem;
		border: 0;
		background: transparent;
		color: var(--color-surface-950-50);
		font-size: 0.74rem;
		font-weight: 650;
		outline: none;
	}

	.studio-preview__device-thumb {
		width: 1.4rem;
		height: 1.4rem;
		object-fit: contain;
	}

	:global(.studio-preview__icon-button--active) {
		border-color: color-mix(in oklab, var(--color-primary-500) 44%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-preview__address {
		min-width: 0;
		height: 2rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-950-50);
		padding: 0 0.55rem;
		font-size: 0.76rem;
	}

	.studio-preview__zoom {
		min-width: 2.3rem;
		color: var(--color-surface-600-400);
		font-size: 0.72rem;
		font-weight: 700;
		text-align: center;
	}

	.studio-preview__mode {
		border: 1px solid var(--color-surface-300-700);
		border-radius: 999px;
		background: color-mix(in oklab, var(--color-surface-300-700) 14%, transparent);
		color: var(--color-surface-600-400);
		padding: 0.08rem 0.42rem;
		font-size: 0.64rem;
		font-weight: 780;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.studio-preview__mode--development {
		border-color: color-mix(in oklab, var(--color-success-500) 55%, transparent);
		background: color-mix(in oklab, var(--color-success-500) 14%, transparent);
		color: var(--color-success-700-300);
	}

	.studio-preview__size {
		max-width: 15rem;
		min-width: 5rem;
		color: var(--color-surface-600-400);
		font-size: 0.7rem;
		font-weight: 700;
		text-align: center;
	}

	:global(.studio-preview__viewport) {
		min-width: 0;
		min-height: 0;
		overflow: auto;
		background:
			linear-gradient(
				45deg,
				color-mix(in oklab, var(--color-surface-200-800) 42%, transparent) 25%,
				transparent 25%
			),
			linear-gradient(
				-45deg,
				color-mix(in oklab, var(--color-surface-200-800) 42%, transparent) 25%,
				transparent 25%
			),
			var(--color-surface-100-900);
		background-position:
			0 0,
			0 0;
		background-size: 1rem 1rem;
		padding: 0.75rem;
	}

	.studio-preview__center {
		display: grid;
		min-width: 100%;
		min-height: 100%;
		place-items: center;
	}

	.studio-preview__scaled-box {
		width: var(--studio-preview-render-width, 100%);
		height: var(--studio-preview-render-height, 100%);
		min-width: 0;
		min-height: 0;
	}

	.studio-preview__scaled-box--fluid {
		width: 100%;
		height: 100%;
	}

	.studio-preview__stage {
		width: var(--studio-preview-width, calc(100% / var(--studio-preview-scale)));
		height: var(--studio-preview-height, calc(100% / var(--studio-preview-scale)));
		min-width: 24rem;
		min-height: 20rem;
		overflow: hidden;
		border: 1px solid var(--color-surface-300-700);
		border-radius: 0.35rem;
		background: white;
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, var(--color-surface-950) 10%, transparent);
		transform: scale(var(--studio-preview-scale));
		transform-origin: top left;
	}

	.studio-preview__stage--framed,
	.studio-preview__stage--viewport {
		position: relative;
		min-width: 0;
		min-height: 0;
	}

	.studio-preview__stage--framed {
		overflow: visible;
		border: 0;
		background: transparent;
		box-shadow: none;
	}

	.studio-preview__stage--viewport {
		width: var(--studio-preview-frame-width);
		height: var(--studio-preview-frame-height);
		border-style: dashed;
	}

	.studio-preview__frame {
		width: 100%;
		height: 100%;
		min-height: 0;
		border: 0;
		background: white;
	}

	.studio-preview__frame--framed {
		position: absolute;
		z-index: 1;
		top: var(--studio-preview-frame-top);
		left: var(--studio-preview-frame-left);
		width: var(--studio-preview-frame-width);
		height: var(--studio-preview-frame-height);
		overflow: hidden;
		border-radius: var(--studio-preview-frame-radius);
	}

	.studio-preview__bezel {
		position: absolute;
		z-index: 2;
		inset: 0;
		pointer-events: none;
		user-select: none;
	}

	.studio-preview__bezel img {
		display: block;
		width: 100%;
		height: 100%;
	}

	.studio-preview :global(.studio-preview__empty) {
		min-height: 16rem;
	}

	@media (max-width: 900px) {
		.studio-preview__bar {
			grid-template-columns: minmax(0, 1fr);
		}

		.studio-preview__actions {
			justify-content: flex-start;
			overflow-x: auto;
		}

		.studio-preview__device-select select {
			max-width: 10rem;
		}
	}
</style>
