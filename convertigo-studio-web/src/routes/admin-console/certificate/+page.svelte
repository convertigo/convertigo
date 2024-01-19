<script>
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import Cache from '$lib/admin-console/config-comp/Cache.svelte';
</script>

<div class="p-10">
	<h1 class="mb-10">Certificates</h1>

	<Card>
		<h1 class="border-b p-2">Certificates Configuration</h1>

		<table class="mt-5 w-full bg-surface-700">
			<thead>
				<tr class="bg-surface-900">
					<th class="px-4 py-2">Certificate / Store</th>
					<th class="px-4 py-2">Type</th>
					<th class="px-4 py-2">Password</th>
					<th class="px-4 py-2">Group</th>
					<th class="px-4 py-2">Delete</th>
					<th class="px-4 py-2">Update</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="border px-4 py-2"></td>
				</tr>
			</tbody>
		</table>
	</Card>

	<div class="mt-10">
		<Card>
			<h1 class="border-b p-2">Mapping Configuration</h1>

			<table class="mt-5 w-full bg-surface-700">
				<thead>
					<tr class="bg-surface-900">
						<th class="px-4 py-2">Project Name</th>
						<th class="px-4 py-2">Certificate / Store</th>
						<th class="px-4 py-2">Delete</th>
						<th class="px-4 py-2">Update</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="border px-4 py-2"></td>
					</tr>
				</tbody>
			</table>

			<table class="mt-5 w-full bg-surface-700">
				<thead>
					<tr class="bg-surface-900">
						<th class="px-4 py-2">Project Name</th>
						<th class="px-4 py-2">Virtual server</th>
						<th class="px-4 py-2">Authorization Group</th>
						<th class="px-4 py-2">User</th>
						<th class="px-4 py-2">Certificate / Store</th>
						<th class="px-4 py-2">Delete</th>
						<th class="px-4 py-2">Update</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="border px-4 py-2"></td>
					</tr>
				</tbody>
			</table>
		</Card>
	</div>
</div>

<style>
	th,
	td {
		border: 1px solid #616161;
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}

	table {
		border-collapse: collapse;
	}
</style>
