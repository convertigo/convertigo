<script context="module">
	import IconGitHub from '~icons/octicon/logo-github-16';
	const ico = {
		'~icons/octicon/logo-github-16': IconGitHub
	};
</script>

<script>
	// @ts-nocheck
	export let icon;
</script>

<svelte:component this={ico[icon]} {...$$restProps}><slot /></svelte:component>
