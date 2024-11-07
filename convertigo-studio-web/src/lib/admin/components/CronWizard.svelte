<script>
	import { ListBox, ListBoxItem } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';
	import Container from '$lib/common/components/Container.svelte';
	/** @type {{cronExpression?: string}} */
	let { cronExpression = $bindable('0 0 0 * * ?') } = $props();

	function createRange(aRange) {
		const newRange = [];
		for (let i = 0; i < aRange.length; ) {
			let deb = aRange[i];
			let fin = aRange[i];
			let inc = 1;
			while (i + inc < aRange.length && +aRange[i + inc] === +aRange[i] + inc) {
				fin = +aRange[i + inc];
				inc++;
			}
			newRange.push(deb === fin ? deb : `${deb}-${fin}`);
			i += inc;
		}
		return newRange.toString();
	}

	function parseRange(rangeStr) {
		const parts = rangeStr.split(',');
		const numbers = [];

		for (let part of parts) {
			if (part.includes('-')) {
				const [start, end] = part.split('-').map(Number);
				for (let i = start; i <= end; i++) {
					numbers.push('' + i);
				}
			} else {
				numbers.push(part);
			}
		}
		return numbers;
	}

	function makeArray(prefix, lenght, inc) {
		return [
			prefix,
			...Array(lenght)
				.fill(0)
				.map((_, index) => '' + (index + inc))
		];
	}

	const def = [
		{
			title: 'Minutes',
			values: makeArray('*', 60, 0),
			labels: makeArray('every', 60, 0)
		},
		{
			title: 'Hours',
			values: makeArray('*', 24, 0),
			labels: makeArray('every', 24, 0)
		},
		{
			title: 'Day of month',
			values: makeArray('*', 31, 1),
			labels: makeArray('all', 31, 1)
		},
		{
			title: 'Month',
			values: makeArray('*', 12, 1),
			labels: [
				'all',
				'January',
				'February',
				'March',
				'April',
				'May',
				'June',
				'July',
				'August',
				'September',
				'October',
				'November',
				'December'
			]
		},
		{
			title: 'Day of week',
			values: makeArray('?', 7, 1),
			labels: ['any', 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
		}
	];

	const binds = new Array(def.length).fill(null);

	let selection = $derived(
		writable(
			cronExpression
				.split(' ')
				.slice(1, 6)
				.map((v) => parseRange(v))
		)
	);

	function changed(event, index) {
		$selection[index].sort((a, b) => (isNaN(a) ? -1 : +a) - (isNaN(b) ? -1 : +b));
		let exp = '0';
		for (let sel of $selection) {
			exp += ` ${createRange(sel)}`;
		}
		cronExpression = exp;
	}
</script>

<div class="flex flex-row flex-wrap">
	{#each def as { title, values, labels }, i}
		<Container flex flexCol gap="5">
			<p class="font-bold text-start pr-5">{title}</p>
			<ListBox rounded="rounded" class="h-52 overflow-y-auto p-1" multiple={true}>
				{#each values as value, j}
					<ListBoxItem
						class="font-extralight"
						active="bg-tertiary-100-800"
						{value}
						name={labels[j]}
						bind:group={$selection[i]}
						on:change={(e) => changed(e, i)}>{labels[j]}</ListBoxItem
					>
				{/each}
			</ListBox>
		</Container>
	{/each}
</div>
