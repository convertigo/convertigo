<script>
	import Icon from '@iconify/svelte';
	import { AppRail } from '@skeletonlabs/skeleton';
	import parts from './PagesRail.json';
	import { page } from '$app/stores';

	$: isRoot = $page.route.id == '/admin-console';
</script>

<AppRail
	width="w-auto"
	height="h-[100%]"
	background="dark:bg-surface-800 bg-white"
	class="border-r-[0.5px] dark:border-surface-500 border-surface-50 p-4"
	active="dark:bg-surface-900"
	hover="hover:bg-surface-900"
>
	{#each parts as tiles, i}
		{#each tiles as tile}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a
				href={`${isRoot ? '' : '../'}${url}`}
				class="flex p-2 items-center dark:hover:bg-surface-500 hover:bg-surface-50 rounded-xl {(
					url == '' ? isRoot : $page.url.pathname.endsWith(url)
				)
					? 'bg'
					: ''}"
			>
				<Icon icon={tile.icon} width={25} height={25} />
				<span
					class="font-extralight font-normal ml-3 text-[14px] dark:text-surface-200 text-surface-800"
					>{tile.title}</span
				>
			</a>
		{/each}
		{#if i < parts.length - 1}
			<div class="col-span-2 flex justify-center my-5">
				<separator class="border-[1px] flex w-[70%] border-surface-100" />
			</div>
		{/if}
	{/each}
</AppRail>

<style lang="postcss">
	.bg {
		@apply bg-surface-50 dark:bg-surface-500;
	}
</style>
