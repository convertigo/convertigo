<script>
	import TreeView from '$lib/common/components/TreeView.svelte';
	import VirtualList from 'svelte-tiny-virtual-list';

	let data = $state([
		['A', '1'],
		['B', '2'],
		['C', '3'],
		['D', '4']
	]);

	let filter = $state('');

	let lines = $derived(data.filter((item) => item[0].toLowerCase().includes(filter.toLowerCase())));

	let bug = $state(true);

	function itemSize(index) {
		return 50;
	}

	function addNode() {
		const newNode = { id: `node_${Date.now()}`, name: `New Node ${Date.now()}` };
		rootNode.children[1].children.push(newNode);
	}

	async function loadChildren({ node }) {
		console.log('Loading children for', node);
		return new Array(node.childrenCount)
			.fill({})
			.map((_, i) => ({ id: `node_${Date.now()}_${i}`, name: `New Node ${Date.now()}_${i}` }));
	}

	function onExpandedChange({ expandedNodes }) {
		for (let node of expandedNodes) {
			node.children
				?.filter((n) => n.childrenCount)
				.forEach((n) => {
					console.log('Expanding node:', n);
					delete n.childrenCount; // Remove childrenCount if it exists
					n.children = [
						{ id: `${n.id}/exp`, name: 'exp', childrenCount: 1 },
						{ id: `${n.id}/leaf`, name: 'leaf' }
					];
				});
		}
		console.log('Expanded changed:', JSON.stringify(expandedNodes));
	}

	let rootNode = $state({
		id: 'ROOT',
		name: '',
		children: [
			{
				id: 'node_modules',
				name: 'node_modules',
				children: [
					{ id: 'node_modules/zag-js', name: 'zag-js' },
					{ id: 'node_modules/pandacss', name: 'panda' },
					{
						id: 'node_modules/@types',
						name: '@types',
						children: [
							{ id: 'node_modules/@types/react', name: 'react' },
							{ id: 'node_modules/@types/react-dom', name: 'react-dom' }
						]
					}
				]
			},
			{
				id: 'src',
				name: 'src',
				children: [
					{ id: 'src/app.tsx', name: 'app.tsx' },
					{ id: 'src/index.ts', name: 'index.ts' }
				]
			},
			{
				id: 'public',
				name: 'public',
				childrenCount: 4
			},
			{
				id: 'exp',
				name: 'exp',
				children: [
					{ id: 'exp/exp', name: 'exp', childrenCount: 1 },
					{ id: 'exp/leaf', name: 'leaf' }
				]
			},
			{
				id: 'src/routes',
				name: 'routes',
				children: [
					{ id: 'src/routes/%2Bpage.svelte', name: '+page.svelte' },
					{ id: 'src/routes/%2Blayout.svelte', name: '+layout.svelte' },
					{ id: 'src/routes/sandbox/%2Bpage.svelte', name: 'sandbox/+page.svelte' },
					{ id: 'src/routes/sandbox/tv/TreeView.svelte', name: 'tv/TreeView.svelte' },
					{ id: 'src/routes/sandbox/tv/TreeNode.svelte', name: 'tv/TreeNode.svelte' }
				]
			},
			{ id: 'panda.config', name: 'panda.config.ts' },
			{ id: 'package.json', name: 'package.json' },
			{ id: 'renovate.json', name: 'renovate.json' },
			{ id: 'readme.md', name: 'README.md' }
		]
	});
</script>

<div>Bug: <input type="checkbox" bind:checked={bug} /></div>
<div>Filter: <input type="text" placeholder="filter" bind:value={filter} /></div>
<!-- <VirtualList height={400} width="auto" itemCount={lines.length} itemSize={50}>
	{#snippet children({ style, index })}
		{@const line = lines[index]}
		<div {style}>
			{#if line || bug}
				{line[0]} : {line[1]}
			{:else}
				<span>Loading...</span>
			{/if}
		</div>
	{/snippet}
{#snippet header()}
head
{/snippet}
{#snippet footer()}
foot
{/snippet}
</VirtualList> -->

<!-- <VirtualList height={400} width="auto" itemCount={lines.length} itemSize={50}>
	<div slot="item" let:index let:style {style}>
		{@const line = lines[index]}
		{line[0]} : {line[1]}
	</div>
</VirtualList> -->
<button onclick={addNode}>Add</button>
<TreeView {rootNode} {onExpandedChange}></TreeView>
