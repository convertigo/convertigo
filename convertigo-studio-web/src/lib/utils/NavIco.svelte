<script context="module">
	import IconGitHub from '~icons/octicon/logo-github-16';
	import gearIcon from '~icons/ph/gear-six-thin';
	import HomeIcon from '~icons/material-symbols-light/home-outline';
	import GearIconConfig from '~icons/ph/gear-thin';
	import ConnectionsIcon from '~icons/ph/plugs-connected-thin';
	import ProjectsIcon from '~icons/ph/folder-thin';
	import CertificateIcon from '~icons/ph/certificate-thin';
	import LogsIcon from '~icons/lets-icons/search-light';
	import TracePlayerIcon from '~icons/ph/video-thin';
	import CacheIcon from '~icons/material-symbols-light/update';
	import SchedulerIcon from '~icons/material-symbols-light/schedule-outline';
	import KeysIcon from '~icons/material-symbols-light/key-outline';
	import RolesIcon from '~icons/lets-icons/user-alt-light';
	import SymbolsIcon from '~icons/et/tools-2';
	import FullSyncIcon from '~icons/material-symbols-light/sync-outline';
	import TestPlatformsIcon from '~icons/file-icons/test-ruby';
	import SwaggerIcon from '~icons/codicon/json';
	import ConvertigoIcon from '~icons/material-symbols-light/cloud-outline';
	import DevNetworkIcon from '~icons/material-symbols-light/sync-outline';
	import DocumentationIcon from '~icons/arcticons/doc-scanner';

	const ico = {
		'octicon:logo-github-16': IconGitHub,
		'ph:gear-six-thin': gearIcon,
		'material-symbols-light:home-outline': HomeIcon,
		'ph:gear-thin': GearIconConfig,
		'ph:plugs-connected-thin': ConnectionsIcon,
		'ph:folder-thin': ProjectsIcon,
		'ph:certificate-thin': CertificateIcon,
		'lets-icons:search-light': LogsIcon,
		'ph:video-thin': TracePlayerIcon,
		'material-symbols-light:update': CacheIcon,
		'material-symbols-light:schedule-outline': SchedulerIcon,
		'material-symbols-light:key-outline': KeysIcon,
		'lets-icons:user-alt-light': RolesIcon,
		'et:tools-2': SymbolsIcon,
		'material-symbols-light:sync-outline': FullSyncIcon,
		'file-icons:test-ruby': TestPlatformsIcon,
		'codicon:json': SwaggerIcon,
		'material-symbols-light:cloud-outline': ConvertigoIcon,
		'material-symbols-light:sync-outline': DevNetworkIcon,
		'arcticons:doc-scanner': DocumentationIcon
	};
</script>

<script>
	// @ts-nocheck
	export let icon;
</script>

<svelte:component this={ico[icon]} {...$$restProps} class="w-5 h-5" />
