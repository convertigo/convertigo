<script>
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Palette from '$lib/studio/palette/Palette.svelte';
	import Properties from '$lib/studio/properties/Properties.svelte';
	import StudioPanel from '$lib/studio/shell/StudioPanel.svelte';
	import Treeview from '$lib/studio/treeview/Treeview.svelte';
</script>

<section class="studio-shell min-h-0 grow gap-low">
	<StudioPanel title="Projects" icon="mdi:folder-outline" contentClass="p-low">
		<Treeview />
	</StudioPanel>

	<StudioPanel
		title="Properties"
		icon="mdi:tune-vertical-variant"
		contentClass="p-none!"
		scroll={false}
	>
		<Properties />
	</StudioPanel>

	<StudioPanel title="Palette" icon="mdi:palette-outline" contentClass="p-none!" scroll={false}>
		<Palette />
	</StudioPanel>

	<StudioPanel title="Editor" icon="mdi:code-tags" contentClass="p-none!" scroll={false}>
		<Editor />
	</StudioPanel>
</section>

<style lang="postcss">
	.studio-shell {
		display: grid;
		grid-template-columns: 24% 22% 22% 32%;
		grid-template-rows: minmax(0, 1fr);
	}

	@media (max-width: 1280px) {
		.studio-shell {
			grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
			grid-template-rows: repeat(2, minmax(0, 1fr));
		}
		:global(.studio-shell > :nth-child(4)) {
			grid-column: 1 / -1;
		}
	}
</style>
