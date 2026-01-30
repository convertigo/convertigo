<script>
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Instances from '$lib/admin/Instances.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import Time from '$lib/common/Time.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { onMount } from 'svelte';
	import PagesRailToggle from './PagesRailToggle.svelte';

	let {
		showLeft = $bindable(),
		showDrawer = $bindable(),
		variant = 'app',
		title = undefined
	} = $props();

	const computedTitle = $derived(
		title ??
			(variant === 'studio'
				? 'Convertigo Studio'
				: `Convertigo ${page.route.id?.includes('dashboard') ? 'Dashboard' : 'Admin Console'}`)
	);

	onMount(() => {
		if (variant === 'studio') {
			return;
		}
		Instances.refresh();
	});

	const instanceOptions = $derived(Instances.instances.map(({ instanceId }) => instanceId));
</script>

<header
	class="sticky top-0 z-20 layout-x-between border-b border-color bg-surface-100-900 shadow-sm/10 shadow-surface-900-100 backdrop-blur-md"
>
	{#if variant === 'studio'}
		<section class="layout-x pl-5">
			<Ico icon="logo.png" alt="logo convertigo" size={7} />
			<h1 class="font-medium">{computedTitle}</h1>
		</section>

		<section class="layout-x-p py-low!">
			<a class="button-primary h-fit! py-none px-low text-sm" href="../admin/">Admin console</a>
			{#if Time.isSameTime}
				<span class="monitor-time">{Time.browserTime}</span>
			{:else}
				<span class="monitor-time max-md:hidden">you {Time.browserTime}</span>
				<span class="monitor-time">server {Time.serverTime} {Time.serverTimezone}</span>
			{/if}
			<LightSwitch />
		</section>
	{:else}
		<section class="layout-x pl-5">
			<PagesRailToggle class="md:hidden" bind:state={showDrawer} />
			<PagesRailToggle class="max-md:hidden" bind:state={showLeft} />
		</section>

		<section class="layout-x">
			<Ico icon="logo.png" alt="logo convertigo" size={7} />
			<h2 class="font-medium max-md:hidden">{computedTitle}</h2>
		</section>

		<section class="layout-x-p py-low!">
			<a
				href="https://github.com/convertigo/convertigo"
				target="_blank"
				class="layout-x-low max-md:hidden"
			>
				<p class="font-medium">Star us on</p>
				<Ico icon="mdi:github" size={8} />
			</a>
			{#if Instances.storeMode === 'redis' && ((Instances.current ?? '').trim() || Instances.instances.length)}
				<div class="layout-x-low max-md:hidden">
					<span class="text-xs font-medium opacity-70">Instance</span>
					<PropertyType
						type="combo"
						multiple={false}
						fit
						item={instanceOptions}
						disabled={Instances.loading}
						class="select h-fit! input-common w-100 px-2 py-none text-sm"
						bind:value={Instances.current}
					/>
					<Button
						icon="mdi:refresh"
						full={false}
						disabled={Instances.loading}
						cls="button-ico-secondary h-fit! px-2 py-none"
						title="Refresh instances"
						ariaLabel="Refresh instances"
						onclick={Instances.refresh}
					/>
				</div>
			{/if}
			{#if Time.isSameTime}
				<span class="monitor-time">{Time.browserTime}</span>
			{:else}
				<span class="monitor-time max-md:hidden">you {Time.browserTime}</span>
				<span class="monitor-time">server {Time.serverTime} {Time.serverTimezone}</span>
			{/if}
			<LightSwitch />
		</section>
	{/if}
</header>

<style lang="postcss">
	@reference "../../../app.css";

	.monitor-time {
		@apply p-1 font-mono text-xs text-nowrap;
	}
</style>
