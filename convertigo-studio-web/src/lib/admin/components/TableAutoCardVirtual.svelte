<script>
	import { onMount } from 'svelte';
	import VirtualList from 'svelte-tiny-virtual-list';
	import {
		refreshConfigurations,
		configurations,
		updateConfigurations
	} from '$lib/admin/stores/configurationStore';
	import { logsList } from '../stores/logsStore';

	let virtualList;
	let rowHeights = [];
	let rowData = [];
	let logsCategory = null;

	let scrollToIndex;
	let scrollToAlignment = 'start';
	let scrollToBehaviour = 'instant';

	onMount(async () => {
		await refreshConfigurations();
		await logsList();
	});

	$: {
		const config = $configurations;
		if (config?.admin?.category) {
			logsCategory = config.admin.category.find((cat) => cat['@_name'] === 'Logs');
		}
	}
</script>

<div class="actions">
	<input type="number" placeholder="Scroll to index..." class="input" bind:value={scrollToIndex} />
	<div class="select">
		<label for="alignment">Alignment</label>
		<span>
			{scrollToAlignment}
			<select id="alignment" bind:value={scrollToAlignment}>
				<option value="start">start</option>
				<option value="center">center</option>
				<option value="end">end</option>
				<option value="auto">auto</option>
			</select>
		</span>
	</div>
	<div class="select">
		<label for="behaviour">Behaviour</label>
		<span>
			{scrollToBehaviour}
			<select id="behaviour" bind:value={scrollToBehaviour}>
				<option value="auto">auto</option>
				<option value="smooth">smooth</option>
				<option value="instant">instant</option>
			</select>
		</span>
	</div>
</div>

<div class="list">
	<VirtualList
		bind:this={virtualList}
		height={500}
		width="auto"
		itemCount={rowData.length}
		itemSize={(index) => rowHeights[index]}
		{scrollToIndex}
	>
		<div
			slot="item"
			let:index
			let:style
			{style}
			class="row"
			class:highlighted={index == scrollToIndex}
		>
			<div class="cell">{rowData[index]?.category}</div>
			<div class="cell">{rowData[index]?.time}</div>
			<div class="cell">{rowData[index]?.thread}</div>
			<div class="cell">{rowData[index]?.message}</div>
			<div class="cell">{rowData[index]?.extra}</div>
		</div>
	</VirtualList>
</div>

<style>
	:global(body),
	:global(html) {
		height: 100%;
		margin: 0;
		background-color: rgb(249, 249, 249);
	}

	:global(.virtual-list-wrapper) {
		margin: 20px;
		background: #fff;
		border-radius: 2px;
		box-shadow:
			0 2px 2px 0 rgba(0, 0, 0, 0.14),
			0 3px 1px -2px rgba(0, 0, 0, 0.2),
			0 1px 5px 0 rgba(0, 0, 0, 0.12);
		background: #fafafa;
		font-family: -apple-system, BlinkMacSystemFont, Helvetica, Arial, sans-serif;
		color: #333;
		-webkit-font-smoothing: antialiased;
	}

	.row {
		display: flex;
		padding: 0 15px;
		border-bottom: 1px solid #eee;
		box-sizing: border-box;
		line-height: 50px;
		font-weight: 500;
		background: #fff;
	}

	.cell {
		flex: 1;
		padding: 0 10px;
		border-right: 1px solid #ddd;
		box-sizing: border-box;
		text-align: center;
	}

	.cell:last-child {
		border-right: none;
	}

	.row.highlighted {
		background: #efefef;
	}

	.actions {
		display: flex;
		flex-wrap: wrap;
		padding: 0 20px;
		padding-top: 15px;
		justify-content: space-between;
	}

	.actions label {
		padding: 10px 0;
		font-size: 18px;
		color: #999;
		font-family: -apple-system, BlinkMacSystemFont, Helvetica, Arial, sans-serif;
	}

	.input {
		color: #333 !important;
		font-size: 18px;
		appearance: none;
		border: none;
		border-bottom: 1px solid #ddd;
		background-color: transparent;
		padding: 10px 0;
		margin-bottom: 10px;
		font-family: -apple-system, BlinkMacSystemFont, Helvetica, Arial, sans-serif;
		outline: none;
		width: 100%;
	}

	.input:focus {
		border-bottom: 2px solid #008cff;
		margin-bottom: 9px;
	}

	.select {
		flex-shrink: 0;
		white-space: nowrap;
		height: 40px;
		margin-bottom: 10px;
		line-height: 40px;
		background: #fff;
		border: 1px solid #ddd;
		border-radius: 3px;
		box-shadow:
			0 2px 2px 0 rgba(0, 0, 0, 0.1),
			0 3px 1px -2px rgba(0, 0, 0, 0.1),
			0 1px 5px 0 rgba(0, 0, 0, 0.06);
		font-family: -apple-system, BlinkMacSystemFont, Helvetica, Arial, sans-serif;
		color: #333;
	}

	.select label {
		display: inline-block;
		height: 100%;
		padding: 0 10px;
		border-right: 1px solid #eee;
		background: #fafafa;
		font-weight: 400;
		color: #999;
	}

	.select span {
		position: relative;
		display: inline-block;
		min-width: 100px;
		padding: 0 15px;
		padding-right: 20px;
		text-transform: capitalize;
		font-weight: 300;
	}

	.select span:after {
		position: absolute;
		top: 50%;
		right: 0.5em;
		transform: translateY(-50%);
		width: 0;
		height: 0;
		padding: 0;
		content: '';
		border-left: 0.25em solid transparent;
		border-right: 0.25em solid transparent;
		border-top: 0.375em solid #ddd;
		pointer-events: none;
	}

	.select select {
		position: absolute;
		top: 0;
		left: 0;
		display: block;
		width: 100%;
		height: 100%;
		opacity: 0;
		cursor: pointer;
	}

	:global(input::-moz-focus-inner),
	:global(input::-moz-focus-outer) {
		border: 0;
	}
</style>
