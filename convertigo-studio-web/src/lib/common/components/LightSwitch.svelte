<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Light from '../Light.svelte';
</script>

<Switch
	name="mode"
	controlActive="bg-surface-300"
	controlInactive="bg-surface-700"
	classes="gap-0!"
	checked={Light.light}
	onCheckedChange={(e) => (Light.light = e.checked)}
>
	{#snippet inactiveChild()}<Ico icon="mdi:moon-and-stars" />{/snippet}
	{#snippet activeChild()}<Ico icon="mdi:weather-sunny" />{/snippet}
</Switch>
