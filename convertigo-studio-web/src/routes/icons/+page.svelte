<script>
	import Ico, { ico } from '$lib/utils/Ico.svelte';
	import { checkArray } from '$lib/utils/service';

	// import { clipboard } from '@skeletonlabs/skeleton';
	// import { draw } from 'svelte/transition';

	const data = Object.keys(ico).reduce((acc, key) => {
		const [pkg, name] = key.split(':');
		acc[pkg] = checkArray(acc[pkg]);
		acc[pkg].push(name);
		return acc;
	}, {});
</script>

<div class="h-full overflow-y-auto">
	<div
		class="grid grid-cols-4 gap-4 p-4 md:grid-cols-6 lg:grid-cols-8 xl:grid-cols-10 2xl:grid-cols-12"
	>
		{#each Object.keys(data) as pkg}
			<div
				class="col-span-full card border-[1px] border-surface-300-700 preset-filled-surface-200-800 p-2"
			>
				{pkg}
			</div>
			{#each data[pkg] as name}
				<div
					class="flex flex-col items-center gap-2 card border-[1px] border-surface-300-700 preset-filled-surface-200-800 p-2"
				>
					<Ico icon="{pkg}:{name}" size="10" />
					<!-- <Ico icon="{pkg}:{name}" animate={{ duration: 1000 }} repeat={true} size="10" /> -->
					<!-- <button use:clipboard={`${pkg}:${name}`}>{name}</button> -->
					<span>{name}</span>
				</div>
			{/each}
		{/each}
	</div>
</div>
