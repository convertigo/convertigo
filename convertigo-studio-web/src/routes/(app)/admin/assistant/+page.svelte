<script>
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';

	const assistantUrl =
		'https://backend-apps.convertigo.net/convertigo/projects/ConvertigoAssistant/DisplayObjects/mobile/';
</script>

<MaxRectangle>
	<iframe
		src={assistantUrl}
		title="Convertigo Assistant"
		class="h-full w-full rounded-xl"
		referrerpolicy="strict-origin-when-cross-origin"
	></iframe>
</MaxRectangle>
