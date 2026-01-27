<script>
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';

	const palette = [
		{
			name: 'Primary',
			class: 'bg-primary-500 text-primary-contrast-500'
		},
		{
			name: 'Secondary',
			class: 'bg-secondary-500 text-secondary-contrast-500'
		},
		{
			name: 'Tertiary',
			class: 'bg-tertiary-500 text-tertiary-contrast-500'
		},
		{
			name: 'Success',
			class: 'bg-success-500 text-success-contrast-500'
		},
		{
			name: 'Danger',
			class: 'bg-error-500 text-error-contrast-500'
		}
	];

	const sections = [
		{
			id: 'yx',
			title: 'Surface y-x (light → dark)',
			container: 'bg-surface-50-950 text-surface-900-100',
			border: 'border-surface-200-800',
			card: 'bg-surface-100-900',
			muted: 'text-surface-600-400',
			surfaceChip: 'bg-surface-100-900 text-surface-900-100 border-surface-200-800',
			active: 'bg-primary-500 text-primary-contrast-500'
		},
		{
			id: 'xy',
			title: 'Surface x-y (dark → light)',
			container: 'bg-surface-950-50 text-surface-100-900',
			border: 'border-surface-800-200',
			card: 'bg-surface-900-100',
			muted: 'text-surface-400-600',
			surfaceChip: 'bg-surface-900-100 text-surface-100-900 border-surface-800-200',
			active: 'bg-primary-500 text-primary-contrast-500'
		}
	];

	let inputA = $state('Sample input');
	let inputB = $state('Sample input');
	let switchA = $state('true');
	let switchB = $state('false');
</script>

<LightSwitch />

<main class="layout-y-stretch gap-8 p-6">
	<header class="layout-y-low">
		<h2 class="text-xl font-semibold">Theme showcase</h2>
		<p class="text-sm text-muted">
			Deux sections identiques pour valider les paires -y-x et -x-y. En changeant de thème, les
			sections s’inversent.
		</p>
	</header>

	<div class="grid gap-8 lg:grid-cols-2">
		{#each sections as section (section.id)}
			<section class="rounded-container border p-6 shadow-sm {section.container} {section.border}">
				<div class="layout-x-between items-center">
					<h3 class="text-base font-semibold">{section.title}</h3>
					<span class="text-xs {section.muted}">{section.container}</span>
				</div>

				<div class="mt-4 layout-y-stretch gap-4">
					<div class="layout-x-wrap gap-2">
						{#each palette as item (item.name)}
							<span class="rounded-base px-3 py-1 text-xs font-medium {item.class}">
								{item.name}
							</span>
						{/each}
						<span class="rounded-base border px-3 py-1 text-xs font-medium {section.surfaceChip}">
							Surface
						</span>
						<span class="rounded-base px-3 py-1 text-xs font-medium {section.active}">
							Active
						</span>
					</div>

					<div class="rounded-base border p-4 {section.card} {section.border}">
						<div class="layout-x-between items-center">
							<div>
								<p class="text-sm font-semibold">Card / Window</p>
								<p class="text-xs {section.muted}">Secondary text & labels</p>
							</div>
							<div class="layout-x gap-2">
								<button class="button-primary h-8 px-3 text-xs">Primary</button>
								<button class="button-secondary h-8 px-3 text-xs">Secondary</button>
							</div>
						</div>

						<div class="mt-3 layout-x-wrap gap-2">
							<button class="button-tertiary h-8 px-3 text-xs">Tertiary</button>
							<button class="button-success h-8 px-3 text-xs">Success</button>
							<button class="button-error h-8 px-3 text-xs">Danger</button>
						</div>
					</div>

					<div class="grid gap-3 sm:grid-cols-2">
						<div class="rounded-base border p-3 {section.card} {section.border}">
							<p class="text-xs font-medium {section.muted}">Input</p>
							{#if section.id == 'yx'}
								<PropertyType name="inputA" bind:value={inputA} />
							{:else}
								<PropertyType name="inputB" bind:value={inputB} />
							{/if}
						</div>
						<div class="rounded-base border p-3 {section.card} {section.border}">
							<p class="text-xs font-medium {section.muted}">Switches</p>
							<div class="layout-y-low">
								{#if section.id == 'yx'}
									<CheckState name="switch-a" values={['false', 'true']} bind:value={switchA}>
										Enabled
									</CheckState>
								{:else}
									<CheckState name="switch-b" values={['false', 'true']} bind:value={switchB}>
										Disabled
									</CheckState>
								{/if}
							</div>
						</div>
					</div>
				</div>
			</section>
		{/each}
	</div>
</main>
