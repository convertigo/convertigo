<script>
	import PropertyType from './PropertyType.svelte';

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

	function onchange(index) {
		selection[index].sort((a, b) => (isNaN(a) ? -1 : +a) - (isNaN(b) ? -1 : +b));
		let exp = '0';
		for (let sel of selection) {
			exp += ` ${createRange(sel)}`;
		}
		cronExpression = exp;
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

	let selection = $state(Array(def.length).fill([]));
	$effect(() => {
		selection = cronExpression
			.split(' ')
			.slice(1, 6)
			.map((v) => parseRange(v));
	});
</script>

<div class="layout-x-wrap-low">
	{#each def as { title, values, labels }, i}
		<div class="w-fit">
			<PropertyType
				type="combo"
				description={title}
				size="8"
				bind:value={selection[i]}
				onchange={(e) => onchange(i)}
				multiple
				item={values.map((value, j) => ({ value, text: labels[j] }))}
			/>
		</div>
	{/each}
</div>
