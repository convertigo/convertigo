<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Light from '../Light.svelte';
</script>

<Switch
	name="mode"
	aria-label="Toggle light mode"
	class="gap-none!"
	checked={Light.light}
	onCheckedChange={(e) => (Light.light = e.checked)}
>
	<Switch.Control
		class="layout-x-none h-7 w-12 rounded-full preset-filled-surface-300-700 p-1 transition-colors duration-150 data-[state=checked]:preset-filled-primary-200-800"
	>
		<Switch.Thumb
			class="grid aspect-square h-full place-items-center rounded-full bg-white text-surface-900 transition-transform duration-150 data-[state=checked]:translate-x-5"
		>
			<Ico icon={Light.light ? 'mdi:weather-sunny' : 'mdi:moon-and-stars'} />
		</Switch.Thumb>
	</Switch.Control>
	<Switch.HiddenInput />
</Switch>
