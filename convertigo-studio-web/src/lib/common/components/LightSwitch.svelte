<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Light from '../Light.svelte';

	const icon = $derived.by(() => (Light.light ? 'mdi:weather-sunny' : 'mdi:moon-and-stars'));
</script>

<Switch
	name="mode"
	aria-label="Toggle light mode"
	checked={Light.light}
	onCheckedChange={(e) => (Light.light = e.checked)}
>
	<Switch.Control class="c8o-switch transition-surface">
		<Switch.Thumb>
			<Ico {icon} />
		</Switch.Thumb>
	</Switch.Control>
	<Switch.HiddenInput />
</Switch>
