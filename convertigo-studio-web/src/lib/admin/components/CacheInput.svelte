<script>
	export let label = '';
	export let id = '';
	export let name = '';
	export let type = 'text';
	export let value = '';
	export let placeholder = 'Enter value ..';
</script>

<div class="border-common mt-5">
	<label for={id} class="text-input">{label}</label>
	<input {id} {name} {type} {value} {placeholder} class="input-common" />
</div>
