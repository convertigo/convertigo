<script context="module">
	import ArcticonsDocScanner from '~icons/arcticons/doc-scanner';
	import CodiconJson from '~icons/codicon/json';
	import EtTools2 from '~icons/et/tools-2';
	import FileIconsTestRuby from '~icons/file-icons/test-ruby';
	import LetsIconsSearchLight from '~icons/lets-icons/search-light';
	import LetsIconsUserAltLight from '~icons/lets-icons/user-alt-light';
	import MaterialSymbolsLightCloudOutline from '~icons/material-symbols-light/cloud-outline';
	import MaterialSymbolsLightHomeOutline from '~icons/material-symbols-light/home-outline';
	import MaterialSymbolsLightKeyOutline from '~icons/material-symbols-light/key-outline';
	import MaterialSymbolsLightScheduleOutline from '~icons/material-symbols-light/schedule-outline';
	import MaterialSymbolsLightSyncOutline from '~icons/material-symbols-light/sync-outline';
	import MaterialSymbolsLightUpdate from '~icons/material-symbols-light/update';
	import OcticonLogoGithub16 from '~icons/octicon/logo-github-16';
	import PhCertificateThin from '~icons/ph/certificate-thin';
	import PhFolderThin from '~icons/ph/folder-thin';
	import PhGearSixThin from '~icons/ph/gear-six-thin';
	import PhGearThin from '~icons/ph/gear-thin';
	import PhPlugsConnectedThin from '~icons/ph/plugs-connected-thin';
	import PhVideoThin from '~icons/ph/video-thin';

	const ico = {
		'arcticons:doc-scanner': ArcticonsDocScanner,
		'codicon:json': CodiconJson,
		'et:tools-2': EtTools2,
		'file-icons:test-ruby': FileIconsTestRuby,
		'lets-icons:search-light': LetsIconsSearchLight,
		'lets-icons:user-alt-light': LetsIconsUserAltLight,
		'material-symbols-light:cloud-outline': MaterialSymbolsLightCloudOutline,
		'material-symbols-light:home-outline': MaterialSymbolsLightHomeOutline,
		'material-symbols-light:key-outline': MaterialSymbolsLightKeyOutline,
		'material-symbols-light:schedule-outline': MaterialSymbolsLightScheduleOutline,
		'material-symbols-light:sync-outline': MaterialSymbolsLightSyncOutline,
		'material-symbols-light:update': MaterialSymbolsLightUpdate,
		'octicon:logo-github-16': OcticonLogoGithub16,
		'ph:certificate-thin': PhCertificateThin,
		'ph:folder-thin': PhFolderThin,
		'ph:gear-six-thin': PhGearSixThin,
		'ph:gear-thin': PhGearThin,
		'ph:plugs-connected-thin': PhPlugsConnectedThin,
		'ph:video-thin': PhVideoThin
	};
</script>

<script>
	// @ts-nocheck
	export let icon;
</script>

<svelte:component this={ico[icon]} {...$$restProps} class="w-5 h-5" />
