<script>
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';

	const assistantUrl = 'https://assistant.convertigo.com/';
</script>

<MaxRectangle>
	<iframe
		src={assistantUrl}
		title="Convertigo Assistant"
		class="h-full w-full rounded-xl"
		referrerpolicy="strict-origin-when-cross-origin"
	></iframe>
</MaxRectangle>
