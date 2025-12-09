<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Light from '../Light.svelte';
</script>

<Switch
	name="mode"
	aria-label="Toggle light mode"
	checked={Light.light}
	onCheckedChange={(e) => (Light.light = e.checked)}
>
	<Switch.Control>
		<Switch.Thumb>
			<Ico icon={Light.light ? 'mdi:weather-sunny' : 'mdi:moon-and-stars'} />
		</Switch.Thumb>
	</Switch.Control>
	<Switch.HiddenInput />
</Switch>
