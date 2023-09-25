<script>
	import {
		AppBar,
		AppShell
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { callService } from '$lib/convertigo';

	import Palette from '$lib/Palette.svelte';


</script>

<AppShell>
	<svelte:fragment slot="header">
		<AppBar>
			<svelte:fragment slot="lead">
				<nav>
					<a href="/">HOME</a>
				</nav>
			</svelte:fragment>
			
			<h1>Palette page</h1>		
		</AppBar>
	</svelte:fragment>
	<div>
		<Palette />
	</div>
</AppShell>