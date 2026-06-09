<script>
	import { asset } from '$app/paths';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import Bezels from '$lib/dashboard/Bezels';
	import Ico from '$lib/utils/Ico.svelte';

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

	let { selectedDeviceId = $bindable('none'), landscape = $bindable(false) } = $props();

	let selectedDevice = $derived(Bezels[selectedDeviceId] ?? Bezels.none);
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
		openGroups = [deviceGroupMap[id] ?? 'responsive'];
		if (id === 'none') {
			landscape = false;
		}
	}

	/**
	 * @param {boolean} nextLandscape
	 */
	function setLandscape(nextLandscape) {
		if (selectedDeviceType === 'phone' || selectedDeviceType === 'tablet') {
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
		return groups;
	}
</script>

<section class="studio-device-panel" aria-label="Frontend devices">
	<header class="studio-device-panel__summary">
		<div>
			<span>Current device</span>
			<strong>{selectedDeviceTitle}</strong>
		</div>
		{#if selectedDevice.id === 'none'}
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

	<div class="studio-device-panel__orientation" aria-label="Preview orientation">
		<button
			type="button"
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
			class:studio-device-panel__orientation-button--active={landscape}
			disabled={selectedDeviceType !== 'phone' && selectedDeviceType !== 'tablet'}
			aria-label="Landscape orientation"
			onclick={() => setLandscape(true)}
		>
			<Ico icon="mdi:camera-rotate-outline" size={4} />
			<span>Landscape</span>
		</button>
	</div>

	<AccordionGroup bind:value={openGroups} collapsible class="studio-device-panel__groups">
		{#each deviceGroups as group (group.id)}
			<AccordionSection
				value={group.id}
				class="studio-device-panel__group"
				triggerClass="studio-device-panel__group-trigger"
				panelClass="studio-device-panel__group-panel"
				title={group.title}
				titleClass="studio-device-panel__group-title"
				count={group.devices.length}
				countVariant="number"
			>
				{#snippet panel()}
					<div class="studio-device-panel__device-list">
						{#each group.devices as device (device.id)}
							{@const isResponsive = device.id === 'none'}
							{@const isSelected = selectedDeviceId === device.id}
							<button
								type="button"
								class="studio-device-panel__device"
								class:studio-device-panel__device--active={isSelected}
								aria-pressed={isSelected}
								aria-label={`Select device ${isResponsive ? 'Responsive' : device.title}`}
								onclick={() => selectDevice(device.id)}
							>
								{#if isResponsive}
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
								<span class="studio-device-panel__device-text">
									<strong>{isResponsive ? 'Responsive' : device.title}</strong>
									<small>{device.iframe?.width ?? '-'} x {device.iframe?.height ?? '-'}</small>
								</span>
							</button>
						{/each}
					</div>
				{/snippet}
			</AccordionSection>
		{/each}
	</AccordionGroup>
</section>

<style>
	.studio-device-panel {
		display: grid;
		height: 100%;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto auto minmax(0, 1fr);
		gap: 0.5rem;
		overflow: hidden;
		padding: 0.55rem;
	}

	.studio-device-panel__summary {
		display: flex;
		min-width: 0;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 66%, transparent);
		color: var(--color-surface-700-300);
		padding: 0.65rem;
	}

	.studio-device-panel__summary div {
		display: grid;
		min-width: 0;
		gap: 0.18rem;
	}

	.studio-device-panel__summary span {
		color: var(--color-surface-600-400);
		font-size: 0.66rem;
		font-weight: 780;
		text-transform: uppercase;
	}

	.studio-device-panel__summary strong {
		overflow: hidden;
		color: var(--color-surface-950-50);
		font-size: 0.88rem;
		line-height: 1.2;
		text-overflow: ellipsis;
		white-space: nowrap;
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
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 70%, transparent);
		padding: 0.25rem;
	}

	.studio-device-panel__orientation button {
		display: flex;
		min-width: 0;
		height: 2rem;
		align-items: center;
		justify-content: center;
		gap: 0.32rem;
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
		min-height: 0;
		overflow: auto;
	}

	:global(.studio-device-panel__group) {
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 55%, transparent);
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
		display: grid;
		min-width: 0;
	}

	.studio-device-panel__device {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.55rem;
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
		display: grid;
		min-width: 0;
		gap: 0.12rem;
	}

	.studio-device-panel__device-text strong,
	.studio-device-panel__device-text small {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
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
