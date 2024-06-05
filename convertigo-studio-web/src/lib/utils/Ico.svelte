<script context="module">
	import DeleteIcon from '~icons/mingcute/delete-line';
	import ReloadIcon from '~icons/simple-line-icons/reload';
	import ExportIcon from '~icons/bytesize/export';
	import ImportIcon from '~icons/bytesize/import';
	import TestIcon from '~icons/fluent-mdl2/test-plan';
	import SaveChanges from '~icons/material-symbols-light/save-as-outline';
	import CancelChanges from '~icons/material-symbols-light/cancel-outline';
	import ReturnIcon from '~icons/mdi/arrow-u-left-top';
	import BackUpRestoreIcon from '~icons/mdi/backup-restore';
	import LeftArrowIcon from '~icons/game-icons/level-three-advanced';
	import HamburgerIcon from '~icons/iconamoon/menu-burger-horizontal-thin';
	import CloudFillIcon from '~icons/iconamoon/cloud-fill';
	import UploadProject from '~icons/icon-park/application-one';
	import NoteIcon from '~icons/fluent/note-48-filled';
	import UpdateIcon from '~icons/dashicons/update';
	import CoffeeLoaderIcon from '~icons/line-md/coffee-loop';
	import AdvancedPropertiesIcon from '~icons/game-icons/level-three-advanced';
	import JobTrs from '~icons/carbon/data-regular';
	import JobSqs from '~icons/material-symbols/api-rounded';
	import JobGrp from '~icons/uim/layer-group';
	import ScheduleCron from '~icons/eos-icons/cronjob';
	import ScheduleRunNow from '~icons/codicon/run-all';
	import ScheduledJob from '~icons/mdi/invoice-scheduled-outline';
	import AddIcon from '~icons/grommet-icons/add';
	import SecretIcon from '~icons/vaadin/key-o';
	import DeployIcon from '~icons/carbon/application';
	import EditIcon from '~icons/mdi/edit-outline';
	import TestPlatformsIcon from '~icons/file-icons/test-ruby';

	const ico = {
		'mingcute:delete-line': DeleteIcon,
		'simple-line-icons:reload': ReloadIcon,
		'bytesize:export': ExportIcon,
		'bytesize:import': ImportIcon,
		'fluent-mdl2:test-plan': TestIcon,
		'material-symbols-light:save-as-outline': SaveChanges,
		'material-symbols-light:cancel-outline': CancelChanges,
		'mdi:arrow-u-left-top': ReturnIcon,
		'mdi:backup-restore': BackUpRestoreIcon,
		'game-icons:level-three-advanced': LeftArrowIcon,
		'iconamoon:menu-burger-horizontal-thin': HamburgerIcon,
		'iconamoon:cloud-fill': CloudFillIcon,
		'icon-park:application-one': UploadProject,
		'fluent:note-48-filled': NoteIcon,
		'dashicons:update': UpdateIcon,
		'line-md:coffee-loop': CoffeeLoaderIcon,
		'game-icons:level-three-advanced': AdvancedPropertiesIcon,
		'carbon:data-regular': JobTrs,
		'material-symbols:api-rounded': JobSqs,
		'uim:layer-group': JobGrp,
		'eos-icons:cronjob': ScheduleCron,
		'codicon:run-all': ScheduleRunNow,
		'mdi:invoice-scheduled-outline': ScheduledJob,
		'grommet-icons:add': AddIcon,
		'vaadin:key-o': SecretIcon,
		'carbon:application': DeployIcon,
		'mdi:edit-outline': EditIcon,
		'file-icons:test-ruby': TestPlatformsIcon
	};
</script>

<script>
	// @ts-nocheck
	export let icon;
</script>

<svelte:component this={ico[icon]} {...$$restProps} class="w-4 h-4" />
