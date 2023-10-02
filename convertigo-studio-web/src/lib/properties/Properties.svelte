<script>
	import { properties } from './propertiesStore';
</script>

<div class="table-container">
	<!-- Native Table Element -->
	<table class="table table-hover table-compact">
		<thead>
			<tr>
				<th>Name</th>
				<th>Value</th>
			</tr>
		</thead>
		<tbody>
			{#each Object.entries($properties) as entry}
				<tr>
					<td>{entry[0]}</td>
					<td><input class="input" type="text" placeholder={entry[0]} value={entry[1]} /></td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
