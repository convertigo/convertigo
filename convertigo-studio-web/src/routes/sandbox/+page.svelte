<script>
	import MyComp from './MyComp.svelte';
	import { LightSwitch } from '@skeletonlabs/skeleton';
</script>

<LightSwitch />
<h1 class="text-red-500 dark:text-green-500 dark:hover:text-blue-500 hover:text-green-500">
	sandbox !
</h1>
<MyComp />
<MyComp />

<div class="layout-x-p">
	<div class="w-12 h-6 bg-red-500"></div>

	<div class="w-36 h-4 bg-green-500"></div>
</div>
