<script>
	import { writable } from 'svelte/store';
	export let cronExpression = '0 0 0 ? * ?';

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
			values: makeArray('?', 31, 1),
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

	$: selection = writable(
		cronExpression
			.split(' ')
			.slice(1, 6)
			.map((v) => parseRange(v))
	);

	function changed(event, index) {
		$selection[index] = [...event.target.options].filter((o) => o.selected).map((o) => o.value);
		let exp = '0';
		for (let sel of $selection) {
			exp += ` ${createRange(sel)}`;
		}
		cronExpression = exp;
	}
</script>

<div class="flex flex-row flex-wrap">
	{#each def as { title, values, labels }, i}
		<div>
			<p class="font-bold text-start p-5">{title}</p>
			<select
				class="select h-[40vh] rounded-token"
				multiple={true}
				on:change={(e) => changed(e, i)}
			>
				{#each values as value, j}
					<option class="font-extralight" {value} selected={$selection[i].includes(value)}
						>{labels[j]}</option
					>
				{/each}
			</select>
			<!--<p>{createRange($selection[i])}</p>-->
		</div>
	{/each}
</div>
