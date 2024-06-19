<script context="module">
	import CarbonApplication from '~icons/carbon/application';
	import CarbonDataRegular from '~icons/carbon/data-regular';
	import CodiconRunAll from '~icons/codicon/run-all';
	import DashiconsUpdate from '~icons/dashicons/update';
	import EosIconsCronjob from '~icons/eos-icons/cronjob';
	import FileIconsTestRuby from '~icons/file-icons/test-ruby';
	import FluentMdl2TestPlan from '~icons/fluent-mdl2/test-plan';
	import FluentNote48Filled from '~icons/fluent/note-48-filled';
	import GameIconsLevelThreeAdvanced from '~icons/game-icons/level-three-advanced';
	import GrommetIconsAdd from '~icons/grommet-icons/add';
	import GrommetIconsFormSubtract from '~icons/grommet-icons/form-subtract';
	import IconamoonCloudFill from '~icons/iconamoon/cloud-fill';
	import IconamoonMenuBurgerHorizontalThin from '~icons/iconamoon/menu-burger-horizontal-thin';
	import IconParkApplicationOne from '~icons/icon-park/application-one';
	import LineMdCoffeeLoop from '~icons/line-md/coffee-loop';
	import MaterialSymbolsApiRounded from '~icons/material-symbols/api-rounded';
	import MaterialSymbolsLightCancelOutline from '~icons/material-symbols-light/cancel-outline';
	import MaterialSymbolsLightSaveAsOutline from '~icons/material-symbols-light/save-as-outline';
	import MdiArrowULeftTop from '~icons/mdi/arrow-u-left-top';
	import MdiBackupRestore from '~icons/mdi/backup-restore';
	import MdiDotsVertical from '~icons/mdi/dots-vertical';
	import MdiEditOutline from '~icons/mdi/edit-outline';
	import MdiEye from '~icons/mdi/eye';
	import MdiEyeOff from '~icons/mdi/eye-off';
	import MdiFilter from '~icons/mdi/filter';
	import MdiInvoiceScheduledOutline from '~icons/mdi/invoice-scheduled-outline';
	import MdiResizeHorizontal from '~icons/mdi/resize-horizontal';
	import MingcuteDeleteLine from '~icons/mingcute/delete-line';
	import SimpleLineIconsReload from '~icons/simple-line-icons/reload';
	import VaadinKeyO from '~icons/vaadin/key-o';
	import UimLayerGroup from '~icons/uim/layer-group';
	import BytesizeExport from '~icons/bytesize/export';
	import BytesizeImport from '~icons/bytesize/import';

	const ico = {
		'carbon:application': CarbonApplication,
		'carbon:data-regular': CarbonDataRegular,
		'codicon:run-all': CodiconRunAll,
		'dashicons:update': DashiconsUpdate,
		'eos-icons:cronjob': EosIconsCronjob,
		'file-icons:test-ruby': FileIconsTestRuby,
		'fluent-mdl2:test-plan': FluentMdl2TestPlan,
		'fluent:note-48-filled': FluentNote48Filled,
		'game-icons:level-three-advanced': GameIconsLevelThreeAdvanced,
		'grommet-icons:add': GrommetIconsAdd,
		'grommet-icons:form-subtract': GrommetIconsFormSubtract,
		'iconamoon:cloud-fill': IconamoonCloudFill,
		'iconamoon:menu-burger-horizontal-thin': IconamoonMenuBurgerHorizontalThin,
		'icon-park:application-one': IconParkApplicationOne,
		'line-md:coffee-loop': LineMdCoffeeLoop,
		'material-symbols:api-rounded': MaterialSymbolsApiRounded,
		'material-symbols-light:cancel-outline': MaterialSymbolsLightCancelOutline,
		'material-symbols-light:save-as-outline': MaterialSymbolsLightSaveAsOutline,
		'mdi:arrow-u-left-top': MdiArrowULeftTop,
		'mdi:backup-restore': MdiBackupRestore,
		'mdi:dots-vertical': MdiDotsVertical,
		'mdi:edit-outline': MdiEditOutline,
		'mdi:eye': MdiEye,
		'mdi:eye-off': MdiEyeOff,
		'mdi:filter': MdiFilter,
		'mdi:invoice-scheduled-outline': MdiInvoiceScheduledOutline,
		'mdi:resize-horizontal': MdiResizeHorizontal,
		'mingcute:delete-line': MingcuteDeleteLine,
		'simple-line-icons:reload': SimpleLineIconsReload,
		'vaadin:key-o': VaadinKeyO,
		'uim:layer-group': UimLayerGroup,
		'bytesize:export': BytesizeExport,
		'bytesize:import': BytesizeImport
	};
</script>

<script>
	// @ts-nocheck
	export let icon;
</script>

<svelte:component this={ico[icon]} {...$$restProps} class="w-4 h-4" />
