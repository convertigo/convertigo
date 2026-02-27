<script>
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import Light from '$lib/common/Light.svelte';

	const assistantBaseUrl = 'https://assistant.convertigo.com/';
	const assistantUrl = $derived.by(() => {
		const url = new URL(assistantBaseUrl);
		url.searchParams.set('dark-theme', String(Light.dark));
		return url.toString();
	});
</script>

<MaxRectangle>
	<iframe
		src={assistantUrl}
		title="Convertigo Assistant"
		class="h-full w-full rounded-xl"
		referrerpolicy="strict-origin-when-cross-origin"
	></iframe>
</MaxRectangle>
