<script>
	import { goto } from '$app/navigation';
	import { asset } from '$app/paths';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import SelectionHighlight from '$lib/common/components/SelectionHighlight.svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import { getFrontendUrl } from '$lib/utils/service';
	import { onDestroy, onMount, tick } from 'svelte';
	import { Spring } from 'svelte/motion';
	import RightPart from '../../../../admin/RightPart.svelte';
	import Last from '../Last.svelte';

	const familyDefinitions = [
		{
			id: 'apple-iphone',
			title: 'Apple — iPhone',
			match: (device) => device.id?.startsWith('iPhone-')
		},
		{ id: 'apple-ipad', title: 'Apple — iPad', match: (device) => device.id?.startsWith('iPad-') },
		{
			id: 'apple-mac',
			title: 'Apple — Mac',
			match: (device) => device.id?.startsWith('MacBook-') || device.id?.startsWith('iMac-')
		},
		{
			id: 'google-pixel',
			title: 'Google Pixel',
			match: (device) => device.id?.startsWith('Google-Pixel-')
		},
		{
			id: 'samsung-galaxy',
			title: 'Samsung Galaxy',
			match: (device) => device.id?.startsWith('Galaxy-')
		},
		{ id: 'dell', title: 'Dell', match: (device) => device.id?.startsWith('Dell-') },
		{ id: 'no-frame', title: 'Preview Without Frame', match: (device) => device.id === 'none' },
		{ id: 'other', title: 'Other Devices', match: () => true }
	];

	let orientation = $derived.by(() =>
		page.params.model?.split('_')[1] == 'h' ? 'horizontal' : 'vertical'
	);
	let selectedDevice = $derived.by(
		() => Bezels[page.params.model?.split('_')[0]] ?? Object.values(Bezels)[0]
	);
	let selectedIndex = $derived.by(() => selectedDevice.index ?? -1);
	const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
	const ZOOM_STEP = 0.15;
	const MIN_ZOOM = 0.4;
	const MAX_ZOOM = 3;
	let zoomMode = $state('fit');
	let zoom = $state(1);
	let fitScale = $state(1);
	let rotationSteps = $state(0);
	let showStatusBar = $state(false);
	const iconButtonClasses =
		'grid h-[2.25rem]! w-[2.25rem]! place-items-center rounded-full border border-surface-200-800 bg-surface-100-900 text-surface-700-300 transition hover:bg-surface-200-800/70 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500 disabled:cursor-not-allowed disabled:opacity-45';
	const iconButtonActiveClasses = 'bg-primary-100-900 text-primary-600-400';
	let groupedDevices = $derived.by(() => {
		const devices = Object.values(Bezels)
			.filter(Boolean)
			.sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
		const remaining = new Map(devices.map((device) => [device.id, device]));
		const orderedGroups = [];
		for (const family of familyDefinitions) {
			const bucket = [];
			for (const device of devices) {
				if (!remaining.has(device.id)) continue;
				if (!family.match(device)) continue;
				bucket.push(device);
				remaining.delete(device.id);
			}
			if (bucket.length) {
				orderedGroups.push({ id: family.id, title: family.title, devices: bucket });
			}
		}
		return orderedGroups;
	});
	let deviceGroupMap = $derived.by(() => {
		const map = new Map();
		for (const group of groupedDevices) {
			for (const device of group.devices) {
				map.set(device.id, group.id);
			}
		}
		return map;
	});
	let openGroup = $state([]);
	let lastDeviceId = $state();

	$effect(() => {
		const targetGroup = deviceGroupMap.get(selectedDevice.id);
		if (!targetGroup) return;

		if (selectedDevice.id !== lastDeviceId) {
			openGroup = [targetGroup];
			lastDeviceId = selectedDevice.id;
			return;
		}

		if (openGroup.length === 0) {
			openGroup = [targetGroup];
		}
	});

	$effect(() => {
		selectedDevice.id;
		zoomMode = 'fit';
		zoom = 1;
		rotationSteps = 0;
	});
	let selectedIndexLast = $state(-1);
	let projectUrl = $derived.by(() =>
		(page.params.project ?? '_') == '_' ? '#' : getFrontendUrl(page.params.project)
	);
	let iframeUrl = $state('');
	let addressBar = $state('');
	let lastProjectUrl = $state('');
	const trimmedAddress = $derived.by(() => addressBar?.trim?.() ?? '');
	let angle = Spring.of(() => (orientation == 'horizontal' ? 1 : 0));

	const adjustZoom = (direction) => {
		const delta = direction * ZOOM_STEP;
		if (zoomMode === 'fit') {
			zoomMode = 'manual';
			zoom = clamp(1 + delta, MIN_ZOOM, MAX_ZOOM);
			return;
		}
		zoom = clamp(zoom + delta, MIN_ZOOM, MAX_ZOOM);
	};

	const resetZoomToFit = () => {
		zoomMode = 'fit';
		zoom = 1;
	};

	const rotateViewer = () => {
		if (selectedDevice.type === 'phone') {
			const next = orientation === 'horizontal' ? 'vertical' : 'horizontal';
			goto(`../${selectedDevice.id}_${next.substring(0, 1)}/`);
			return;
		}
		rotationSteps = (rotationSteps + 1) % 4;
	};

	$effect(() => {
		const currentUrl = projectUrl;
		if (currentUrl !== lastProjectUrl) {
			iframeUrl = currentUrl;
			addressBar = currentUrl;
			lastProjectUrl = currentUrl;
		}
	});

	$effect(() => {
		const currentUrl = projectUrl;
		if (currentUrl !== lastProjectUrl) {
			iframeUrl = currentUrl;
			addressBar = currentUrl;
			lastProjectUrl = currentUrl;
		}
	});

	const toggleStatusBar = () => {
		showStatusBar = !showStatusBar;
	};

	const openInNewTab = (url = iframeUrl) => {
		const target = url?.trim?.() ?? '';
		if (!target || target === '#') return;
		if (typeof window !== 'undefined') {
			window.open(target, '_blank', 'noopener');
		}
	};

	const navigateBack = () => {
		try {
			if (iframe?.contentWindow?.history) {
				iframe.contentWindow.history.back();
				return;
			}
		} catch (error) {
			console.warn('Unable to control iframe history (back)', error);
		}
		if (typeof history !== 'undefined') history.back();
	};

	const navigateForward = () => {
		try {
			if (iframe?.contentWindow?.history) {
				iframe.contentWindow.history.forward();
				return;
			}
		} catch (error) {
			console.warn('Unable to control iframe history (forward)', error);
		}
		if (typeof history !== 'undefined') history.forward();
	};

	$effect(() => {
		Last.model = selectedDevice.id;
		Last.orientation = orientation.substring(0, 1);
		angle.target = orientation == 'horizontal' ? 1 : 0;
	});

	$effect(() => {
		selectedIndex;
		tick().then(() => {
			selectedIndexLast = selectedIndex;
		});
	});

	let iframe = $state();
	let bezel = $state();

	$effect(() => {
		if (!bezel || !iframe) return;
		// Ensure the frameless preview fills the available viewport.
		if (selectedDevice.id === 'none' && clientWidth && clientHeight) {
			const autoSize = `${clientWidth}px`;
			const autoHeight = `${clientHeight}px`;
			bezel.setAttribute('style', '');
			iframe.setAttribute('style', '');
			bezel.style.position = 'relative';
			iframe.style.position = 'relative';
			for (const [prop, value] of [
				['width', autoSize],
				['height', autoHeight],
				['minWidth', autoSize],
				['minHeight', autoHeight]
			]) {
				bezel.style[prop] = value;
				iframe.style[prop] = value;
			}
			return;
		}
		const previousScale = bezel.style.scale;
		for (const elt of [iframe, bezel]) {
			elt.attributeStyleMap.clear();
			for (let [key, value] of Object.entries(
				elt == iframe ? selectedDevice.iframe : selectedDevice.bezel
			)) {
				if (Number.isFinite(value)) {
					value = `${value}px`;
				}
				if (elt == bezel && (key == 'height' || key == 'width')) {
					elt.style[`min-${key}`] = value;
				}
				elt.style[key] = value;
			}
		}
		const bezelTransforms = [];
		const iframeTransforms = [];
		iframe.style.transformOrigin = '';

		if (selectedDevice.type == 'phone') {
			bezelTransforms.push(`rotate(${angle.current * -90}deg)`);
			iframeTransforms.push(`rotate(${angle.target * 90}deg)`);
			if (angle.target == 1) {
				const w = iframe.style.height;
				iframe.style.height = iframe.style.width;
				iframe.style.width = w;
				iframe.style.transformOrigin = '0 0';
				iframeTransforms.push('translate(0, -100%)');
			}
		}

		if (rotationSteps) {
			bezelTransforms.push(`rotate(${rotationSteps * 90}deg)`);
		}

		bezel.style.transform = bezelTransforms.join(' ');
		iframe.style.transform = iframeTransforms.join(' ');
		bezel.style.scale = previousScale;
	});

	let clientHeight = $state(0);
	let clientWidth = $state(0);

	$effect(() => {
		if (!bezel || !iframe) return;
		let nextFit = 1;
		if (selectedDevice.id != 'none') {
			const { height, width } = selectedDevice.bezel ?? {};
			const referenceHeight = angle.target == 1 ? width : height;
			const referenceWidth = angle.target == 1 ? height : width;
			const scaleHeight = referenceHeight ? clientHeight / referenceHeight : 1;
			const scaleWidth = referenceWidth ? clientWidth / referenceWidth : 1;
			nextFit = Math.max(0.01, Math.min(scaleHeight, scaleWidth));
		}
		fitScale = nextFit;
		const appliedScale = zoomMode === 'fit' ? fitScale : fitScale * zoom;
		bezel.style.scale = appliedScale;
	});

	const applyAddressBar = () => {
		if (!trimmedAddress || trimmedAddress === '#') return;
		iframeUrl = trimmedAddress;
		addressBar = trimmedAddress;
	};

	const reloadIframe = () => {
		try {
			iframe?.contentWindow?.location?.reload();
		} catch (error) {
			console.warn('Unable to reload iframe', error);
		}
	};

	$effect(() => {
		iframeUrl = projectUrl;
		addressBar = projectUrl;
	});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
	});
</script>

{#snippet rightPart()}
	<nav
		class="h-full border-r-[0.5px] border-color preset-filled-surface-100-900 max-md:layout-grid-[100px]"
	>
		<AccordionGroup bind:value={openGroup} collapsible>
			{#each groupedDevices as { id, title, devices } (id)}
				<AccordionSection
					value={id}
					class="border-b border-surface-200-800 last:border-none"
					triggerClass="px-low py-2 text-sm font-semibold uppercase tracking-wide text-surface-600-400 hover:text-surface-900-100"
					panelClass="px-0 pb-2"
					{title}
					titleClass="text-sm font-semibold uppercase tracking-wide"
					count={devices.length}
					countVariant="number"
				>
					{#snippet panel()}
						<div class="layout-y-stretch gap-1 pt-1">
							{#each devices as device (device.id)}
								{@const {
									id,
									title,
									type,
									iframe: { height, width },
									index: deviceIndex
								} = device}
								{@const href =
									type == 'phone' ? `../${id}_${orientation.substring(0, 1)}/` : `../${id}/`}
								{@const isSelected = selectedIndex == deviceIndex}
								<a
									{href}
									aria-current={isSelected ? 'true' : undefined}
									class="relative layout-x-p-low min-w-36 items-center! gap! rounded-sm py-2 shadow-surface-900-100 hover:bg-surface-200-800 hover:shadow-md/10"
								>
									{#if isSelected}
										<SelectionHighlight delta={selectedIndexLast - selectedIndex} />
									{/if}
									{#if id != 'none'}
										<picture
											class="z-10 layout-x-none h-16 w-16 shrink-0 justify-center overflow-hidden rounded-sm"
											aria-hidden="true"
										>
											<source srcset={asset(`/bezels/thumbnails/${id}.webp`)} type="image/webp" />
											<img
												src={asset(`/bezels/thumbnails/${id}.webp`)}
												alt=""
												class="max-h-full max-w-full"
												loading="lazy"
											/>
										</picture>
									{:else}
										<div
											class="z-10 layout-x-none h-16 w-16 shrink-0 justify-center rounded-sm border border-dashed border-surface-200-800 text-[10px] tracking-wide text-muted uppercase"
											aria-hidden="true"
										>
											No frame
										</div>
									{/if}
									<span class="z-10 text-[13px] font-{isSelected ? 'medium' : 'normal'}"
										>{title}<br /><small>{width} x {height}</small></span
									>
								</a>
							{/each}
						</div>
					{/snippet}
				</AccordionSection>
			{/each}
		</AccordionGroup>
	</nav>
{/snippet}
<InputGroup
	class="sticky top-[60px] z-10 -mt-low rounded-container !border-surface-200-800 !bg-surface-100-900 p-low shadow-follow backdrop-blur-sm"
	bind:value={addressBar}
	onsubmit={(event) => {
		event.preventDefault();
		applyAddressBar();
	}}
>
	{#snippet leading()}
		<div class="gap-xs layout-x">
			<Button
				icon="mdi:arrow-left"
				title="Go back"
				onclick={navigateBack}
				cls={iconButtonClasses}
			/>
			<Button
				icon="mdi:arrow-right"
				title="Go forward"
				onclick={navigateForward}
				cls={iconButtonClasses}
			/>
			<Button icon="mdi:reload" title="Reload" onclick={reloadIframe} cls={iconButtonClasses} />
		</div>
	{/snippet}
	{#snippet actions()}
		<Button
			label="Go"
			title="Load URL"
			cls="button-primary w-fit!"
			disabled={!trimmedAddress || trimmedAddress === '#'}
			onclick={applyAddressBar}
		/>
		<Button
			icon="mdi:magnify-minus-outline"
			title="Zoom out"
			onclick={() => adjustZoom(-1)}
			cls={iconButtonClasses}
		/>
		<Button
			icon="mdi:fit-to-page-outline"
			title="Fit to screen"
			onclick={resetZoomToFit}
			cls={`${iconButtonClasses} ${zoomMode === 'fit' ? iconButtonActiveClasses : ''}`}
		/>
		<Button
			icon="mdi:magnify-plus-outline"
			title="Zoom in"
			onclick={() => adjustZoom(1)}
			cls={iconButtonClasses}
		/>
		<span class="text-xs font-semibold text-muted"
			>{Math.round((zoomMode === 'fit' ? 1 : zoom) * 100)}%</span
		>
		<Button
			icon="mdi:camera-rotate-outline"
			title="Rotate"
			onclick={rotateViewer}
			cls={iconButtonClasses}
		/>
		<Button
			icon={showStatusBar ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline'}
			title="Toggle status bar"
			onclick={toggleStatusBar}
			cls={`${iconButtonClasses} ${showStatusBar ? iconButtonActiveClasses : ''}`}
		/>
		<Button
			icon="mdi:open-in-new-variant"
			title="Open in new tab"
			onclick={() => openInNewTab(trimmedAddress)}
			disabled={!trimmedAddress || trimmedAddress === '#'}
			cls={iconButtonClasses}
		/>
	{/snippet}
</InputGroup>
<MaxRectangle bind:clientHeight bind:clientWidth>
	<div
		class="layout-x justify-center"
		style="max-height: {clientHeight}px; max-width: {clientWidth}px; height: {clientHeight}px; width: {clientWidth}px;"
	>
		<div class="relative inline-flex overflow-visible">
			<div bind:this={bezel} class="relative">
				<iframe
					bind:this={iframe}
					src={iframeUrl}
					title={`${selectedDevice.title} Preview`}
					class="absolute overflow-hidden"
					class:hidden={!iframe}
				></iframe>
				{#if showStatusBar && selectedDevice.type === 'phone' && orientation === 'vertical'}
					<div
						class="absolute top-[min(18px,5%)] left-1/2 z-10 layout-x-between w-[min(260px,70%)] -translate-x-1/2 items-center rounded-full bg-gradient-to-b from-surface-900/80 to-surface-900/55 px-3 py-[4px] text-[10px] font-semibold tracking-[0.08em] text-white/90 shadow-md/40 shadow-surface-900-100 backdrop-blur-lg"
						aria-hidden="true"
					>
						<span class="[font-variant-numeric:tabular-nums]">9:41</span>
						<div class="layout-x-low">
							<span class="inline-block h-[5px] w-[34px] rounded-full bg-white/75"></span>
							<span class="inline-block h-[5px] w-[5px] rounded-full bg-white/75"></span>
							<span
								class="clip-path-[polygon(0%_100%,15%_35%,45%_60%,65%_20%,100%_100%)] inline-block h-[9px] w-[10px] bg-white/75"
							></span>
						</div>
					</div>
				{/if}
				{#if selectedDevice.id != 'none'}
					{#key `${selectedDevice.id}-${orientation}`}
						<picture class="pointer-events-none absolute inset-0 select-none" aria-hidden="true">
							<source srcset={asset(`/bezels/${selectedDevice.id}.webp`)} type="image/webp" />
							<img
								src={asset(`/bezels/${selectedDevice.id}.webp`)}
								alt=""
								class="min-h-full min-w-full"
								loading="lazy"
							/>
						</picture>
					{/key}
				{/if}
			</div>
		</div>
	</div>
	<!-- </div>
	</div> -->
</MaxRectangle>
