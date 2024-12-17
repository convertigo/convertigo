<script>
	import { getContext } from 'svelte';
	import { setToastContext } from './service';

	setToastContext(getContext('toast'));
</script>
