<script>
	import { asset } from '$app/paths';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import Ico from '$lib/utils/Ico.svelte';
	import {
		addCustomDevice,
		deviceById,
		nativeOsOf,
		previewDevices,
		removeCustomDevice,
		selectDeviceOs
	} from './previewDevices.svelte.js';

	const familyDefinitions = [
		{
			id: 'apple-iphone',
			title: 'Apple iPhone',
			match: (device) => device.id?.startsWith('iPhone-')
		},
		{ id: 'apple-ipad', title: 'Apple iPad', match: (device) => device.id?.startsWith('iPad-') },
		{
			id: 'apple-mac',
			title: 'Apple Mac',
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
		{ id: 'responsive', title: 'Responsive', match: (device) => device.id === 'none' },
		{ id: 'other', title: 'Other devices', match: () => true }
	];

	let {
		selectedDeviceId = $bindable('none'),
		landscape = $bindable(false),
		onSelect = () => {}
	} = $props();

	let selectedDevice = $derived(deviceById(selectedDeviceId));
	let selectedDeviceType = $derived(selectedDevice?.type ?? '');
	let selectedDeviceTitle = $derived(
		selectedDevice?.id === 'none' ? 'Responsive' : (selectedDevice?.title ?? 'Responsive')
	);
	let deviceGroups = $derived.by(buildDeviceGroups);
	let deviceGroupMap = $derived.by(() => {
		/** @type {Record<string, string>} */
		const map = {};
		for (const group of deviceGroups) {
			for (const device of group.devices) {
				map[device.id] = group.id;
			}
		}
		return map;
	});
	let openGroups = $state(['responsive']);

	/**
	 * @param {string} id
	 */
	function selectDevice(id) {
		selectedDeviceId = id;
		openGroups = [deviceGroupMap[id] ?? (id.startsWith('custom-') ? 'custom' : 'responsive')];
		if (id === 'none') {
			landscape = false;
		}
		onSelect(id);
	}

	/**
	 * @param {boolean} nextLandscape
	 */
	function setLandscape(nextLandscape) {
		if (
			selectedDeviceType === 'phone' ||
			selectedDeviceType === 'tablet' ||
			selectedDeviceType === 'custom'
		) {
			landscape = nextLandscape;
		}
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
		groups.push({ id: 'custom', title: 'Custom devices', devices: previewDevices.custom });
		return groups;
	}

	function addDevice() {
		const id = addCustomDevice();
		if (id) {
			selectDevice(id);
		}
	}

	/**
	 * @param {string} id
	 */
	function removeDevice(id) {
		if (removeCustomDevice(id) && selectedDeviceId === id) {
			selectDevice('none');
		}
	}
</script>

<section class="studio-device-panel layout-y-stretch-low" aria-label="Frontend devices">
	<header class="studio-device-panel__summary layout-x-between-low studio-surface">
		<div class="studio-device-panel__summary-text layout-y-none">
			<span class="studio-label">Current device</span>
			<strong class="studio-ellipsis">{selectedDeviceTitle}</strong>
		</div>
		{#if selectedDevice.id === 'none' || !selectedDevice.bezel}
			<Ico icon="mdi:devices" size={6} />
		{:else}
			<img
				class="studio-device-panel__summary-thumb"
				src={asset(`/bezels/thumbnails/${selectedDevice.id}.webp`)}
				alt=""
				loading="lazy"
			/>
		{/if}
	</header>

	<div class="studio-device-panel__orientation studio-surface" aria-label="Preview orientation">
		<button
			type="button"
			class="layout-x-center-low"
			class:studio-device-panel__orientation-button--active={!landscape}
			disabled={selectedDeviceType !== 'phone' && selectedDeviceType !== 'tablet'}
			aria-label="Portrait orientation"
			onclick={() => setLandscape(false)}
		>
			<Ico icon="mdi:smartphone-link" size={4} />
			<span>Portrait</span>
		</button>
		<button
			type="button"
			class="layout-x-center-low"
			class:studio-device-panel__orientation-button--active={landscape}
			disabled={selectedDeviceType !== 'phone' && selectedDeviceType !== 'tablet'}
			aria-label="Landscape orientation"
			onclick={() => setLandscape(true)}
		>
			<Ico icon="mdi:camera-rotate-outline" size={4} />
			<span>Landscape</span>
		</button>
	</div>

	<div class="studio-device-panel__orientation studio-surface" aria-label="Device OS">
		{#each [{ os: 'auto', label: `Auto (${nativeOsOf(selectedDevice) === 'ios' ? 'iOS' : 'Android'})` }, { os: 'android', label: 'Android' }, { os: 'ios', label: 'iOS' }] as choice (choice.os)}
			<button
				type="button"
				class="layout-x-center-low"
				class:studio-device-panel__orientation-button--active={previewDevices.os === choice.os}
				title="The OS the NGX application shows, as its Ionic mode"
				onclick={() => selectDeviceOs(/** @type {'auto' | 'android' | 'ios'} */ (choice.os))}
			>
				<span>{choice.label}</span>
			</button>
		{/each}
	</div>

	<AccordionGroup bind:value={openGroups} collapsible class="studio-device-panel__groups">
		{#each deviceGroups as group (group.id)}
			<AccordionSection
				value={group.id}
				class="studio-device-panel__group studio-surface"
				triggerClass="studio-device-panel__group-trigger"
				panelClass="studio-device-panel__group-panel"
				title={group.title}
				titleClass="studio-device-panel__group-title"
				count={group.devices.length}
				countVariant="number"
			>
				{#snippet panel()}
					<div class="studio-device-panel__device-list layout-y-none">
						{#each group.devices as device (device.id)}
							{@const isResponsive = device.id === 'none'}
							{@const isSelected = selectedDeviceId === device.id}
							{@const isCustom = group.id === 'custom'}
							<button
								type="button"
								class="studio-device-panel__device layout-x-low"
								class:studio-device-panel__device--active={isSelected}
								aria-pressed={isSelected}
								aria-label={`Select device ${isResponsive ? 'Responsive' : device.title}`}
								onclick={() => selectDevice(device.id)}
							>
								{#if isResponsive || isCustom}
									<span class="studio-device-panel__responsive-thumb" aria-hidden="true">
										<Ico icon="mdi:devices" size={5} />
									</span>
								{:else}
									<img
										class="studio-device-panel__device-thumb"
										src={asset(`/bezels/thumbnails/${device.id}.webp`)}
										alt=""
										loading="lazy"
									/>
								{/if}
								<span class="studio-device-panel__device-text layout-y-none">
									<strong class="studio-ellipsis"
										>{isResponsive ? 'Responsive' : device.title}</strong
									>
									<small class="studio-ellipsis"
										>{device.iframe?.width ?? '-'} x {device.iframe?.height ?? '-'}</small
									>
								</span>
								{#if isCustom}
									<span
										class="studio-device-panel__remove"
										role="button"
										tabindex="0"
										title="Remove the device"
										aria-label={`Remove device ${device.title}`}
										onclick={(event) => {
											event.stopPropagation();
											removeDevice(device.id);
										}}
										onkeydown={(event) => {
											if (event.key === 'Enter' || event.key === ' ') {
												event.preventDefault();
												event.stopPropagation();
												removeDevice(device.id);
											}
										}}><Ico icon="mdi:close" size={4} /></span
									>
								{/if}
							</button>
						{/each}
						{#if group.id === 'custom'}
							<button
								type="button"
								class="studio-device-panel__device layout-x-low"
								onclick={addDevice}
							>
								<span class="studio-device-panel__responsive-thumb" aria-hidden="true">
									<Ico icon="mdi:plus" size={5} />
								</span>
								<span class="studio-device-panel__device-text layout-y-none">
									<strong class="studio-ellipsis">Add a custom device</strong>
									<small class="studio-ellipsis">Its name and its screen size</small>
								</span>
							</button>
						{/if}
					</div>
				{/snippet}
			</AccordionSection>
		{/each}
	</AccordionGroup>
</section>

<style>
	.studio-device-panel__remove {
		display: inline-grid;
		margin-inline-start: auto;
		place-items: center;
		border-radius: 0.25rem;
		color: var(--color-surface-600-400);
		padding: 0.15rem;
	}

	.studio-device-panel__remove:hover {
		background: color-mix(in oklab, var(--color-error-500) 15%, transparent);
		color: var(--color-error-500);
	}

	.studio-device-panel {
		height: 100%;
		min-width: 0;
		min-height: 0;
		overflow: hidden;
		padding: 0.55rem;
	}

	.studio-device-panel__summary {
		min-width: 0;
		background: color-mix(in oklab, var(--color-surface-100-900) 66%, transparent);
		color: var(--color-surface-700-300);
		padding: 0.65rem;
	}

	.studio-device-panel__summary-text {
		min-width: 0;
	}

	.studio-device-panel__summary strong {
		color: var(--color-surface-950-50);
		font-size: 0.88rem;
		line-height: 1.2;
	}

	.studio-device-panel__summary-thumb {
		width: 2.9rem;
		height: 2.9rem;
		flex: 0 0 auto;
		object-fit: contain;
	}

	.studio-device-panel__orientation {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.25rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 70%, transparent);
		padding: 0.25rem;
	}

	.studio-device-panel__orientation button {
		min-width: 0;
		height: 2rem;
		border: 1px solid transparent;
		border-radius: 0.32rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0 0.35rem;
		font-size: 0.72rem;
		font-weight: 720;
	}

	.studio-device-panel__orientation button:not(:disabled):hover,
	.studio-device-panel__orientation-button--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-device-panel__orientation button:disabled {
		cursor: not-allowed;
		opacity: 0.45;
	}

	:global(.studio-device-panel__groups) {
		flex: 1 1 auto;
		min-height: 0;
		overflow: auto;
	}

	:global(.studio-device-panel__group) {
		overflow: hidden;
	}

	:global(.studio-device-panel__group + .studio-device-panel__group) {
		margin-top: 0.45rem;
	}

	:global(.studio-device-panel__group-trigger) {
		min-height: 2.35rem;
		border-bottom-color: var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 72%, transparent);
		padding: 0.45rem 0.65rem;
	}

	:global(.studio-device-panel__group-title) {
		font-size: 0.78rem;
		font-weight: 760;
	}

	:global(.studio-device-panel__group-panel) {
		padding: 0;
	}

	.studio-device-panel__device-list {
		min-width: 0;
	}

	.studio-device-panel__device {
		min-width: 0;
		border: 0;
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-200-800) 72%, transparent);
		background: transparent;
		color: var(--color-surface-850-150);
		padding: 0.55rem 0.65rem;
		text-align: left;
	}

	.studio-device-panel__device:hover,
	.studio-device-panel__device--active {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-700-300);
	}

	.studio-device-panel__device:last-child {
		border-bottom: 0;
	}

	.studio-device-panel__device-thumb,
	.studio-device-panel__responsive-thumb {
		display: grid;
		width: 2.65rem;
		height: 2.65rem;
		flex: 0 0 auto;
		place-items: center;
		border-radius: 0.25rem;
		object-fit: contain;
	}

	.studio-device-panel__responsive-thumb {
		border: 1px dashed var(--color-surface-300-700);
		color: var(--color-surface-600-400);
	}

	.studio-device-panel__device-text {
		min-width: 0;
	}

	.studio-device-panel__device-text strong {
		font-size: 0.78rem;
		line-height: 1.15;
	}

	.studio-device-panel__device-text small {
		color: var(--color-surface-600-400);
		font-size: 0.68rem;
		font-weight: 650;
	}
</style>
