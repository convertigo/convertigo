<script>
	import { ico } from '$lib/utils/Ico.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray } from '$lib/utils/service';
	import { draw } from 'svelte/transition';

	const data = Object.keys(ico).reduce((acc, key) => {
		const [pkg, name] = key.split(':');
		acc[pkg] = checkArray(acc[pkg]);
		acc[pkg].push(name);
		return acc;
	}, {});
</script>

<div class="h-full overflow-y-auto">
	<div
		class="grid grid-cols-4 md:grid-cols-6 lg:grid-cols-8 xl:grid-cols-10 2xl:grid-cols-12 gap-4 p-4"
	>
		{#each Object.keys(data) as pkg}
			<div class="card p-2 col-span-full">{pkg}</div>
			{#each data[pkg] as name}
				<div class="card p-2 flex flex-col items-center gap-2">
					<Ico icon="{pkg}:{name}" size="10" />
					<Ico icon="{pkg}:{name}" animate={{ duration: 1000 }} repeat={true} size="10" />
					<span>{name}</span>
				</div>
			{/each}
		{/each}
	</div>
</div>
