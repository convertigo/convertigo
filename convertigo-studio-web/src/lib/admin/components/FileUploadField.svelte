<script>
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{
		name: string;
		accept?: Record<string, string[]>;
		maxFiles?: number;
		required?: boolean;
		allowDrop?: boolean;
		class?: string;
		title?: string;
		hint?: string;
		triggerLabel?: string;
		triggerIcon?: string;
		dropIcon?: string;
		itemIcon?: string;
		deleteIcon?: string;
		deleteIconSize?: number;
		rejectedText?: string;
		dropzoneClass?: string;
		itemGroupClass?: string;
	}} */
	let {
		name,
		accept = {},
		maxFiles = 1,
		required = false,
		allowDrop = true,
		class: cls = '',
		title = 'Drop or choose a file',
		hint = '',
		triggerLabel = 'Choose file',
		triggerIcon = 'mdi:paperclip',
		dropIcon = 'mdi:briefcase-upload-outline',
		itemIcon = 'mdi:briefcase-upload-outline',
		deleteIcon = 'mdi:delete-outline',
		deleteIconSize = 6,
		rejectedText = '',
		dropzoneClass = '',
		itemGroupClass = 'mt-3 mb-4 layout-y-low'
	} = $props();

	const formatFileSize = (bytes) => {
		if (!Number.isFinite(bytes)) return '';
		if (bytes < 1024) return `${bytes} B`;
		const units = ['KB', 'MB', 'GB', 'TB'];
		let value = bytes / 1024;
		let unitIndex = 0;
		while (value >= 1024 && unitIndex < units.length - 1) {
			value /= 1024;
			unitIndex += 1;
		}
		const decimals = value < 10 ? 1 : 0;
		return `${value.toFixed(decimals)} ${units[unitIndex]}`;
	};

	const rootClass = $derived(['w-full', cls].filter(Boolean).join(' '));
	const dropzoneBase =
		'surface-card-shell flex cursor-pointer select-none flex-col items-center gap-2 border-dashed bg-surface-100-900 p-6 text-center transition-soft data-[dragging=true]:preset-filled-primary-100-900';
	const dropzoneClasses = $derived([dropzoneBase, dropzoneClass].filter(Boolean).join(' '));
	const acceptedExtensions = $derived(
		[...new Set(Object.values(accept).flatMap((values) => values ?? []))]
			.filter(Boolean)
			.map((value) => String(value).trim())
	);
	const resolvedHint = $derived(
		hint || (acceptedExtensions.length > 0 ? `Accepted: ${acceptedExtensions.join(', ')}` : '')
	);
	const resolvedRejectedText = $derived(
		rejectedText ||
			(acceptedExtensions.length > 0
				? `Unsupported file type. Allowed: ${acceptedExtensions.join(', ')}`
				: 'This file cannot be uploaded.')
	);
</script>

{#snippet trigger(attrs)}
	{@const triggerClass = [attrs?.class, 'mx-auto mt-2 button-primary w-fit!']
		.filter(Boolean)
		.join(' ')}
	<button {...attrs} type="button" class={triggerClass}>
		{#if triggerIcon}
			<Ico icon={triggerIcon} size={4} />
		{/if}
		<span>{triggerLabel}</span>
	</button>
{/snippet}

<FileUpload {name} {accept} {maxFiles} {required} {allowDrop} class={rootClass}>
	<FileUpload.Dropzone class={dropzoneClasses}>
		<Ico icon={dropIcon} size="8" class="mx-auto text-primary-500" />
		<p class="text-base font-semibold">{title}</p>
		{#if resolvedHint}
			<p class="text-xs text-muted">{resolvedHint}</p>
		{/if}
		<FileUpload.Trigger element={trigger} />
		<FileUpload.HiddenInput />
	</FileUpload.Dropzone>
	<FileUpload.Context>
		{#snippet children(fileUpload)}
			<FileUpload.ItemGroup class={itemGroupClass}>
				{#each fileUpload().acceptedFiles as file (file.name)}
					<FileUpload.Item
						{file}
						class="layout-x-between items-center rounded-sm border border-surface-200-800 bg-surface-50-950 py-2 px-low shadow-xs"
					>
						<div class="layout-x-low min-w-0 items-center gap-low">
							<Ico icon={itemIcon} size="4" class="text-surface-600-400" />
							<FileUpload.ItemName class="truncate text-sm font-medium">
								{file.name}
							</FileUpload.ItemName>
						</div>
						<div class="layout-x-low items-center gap-low text-xs text-muted">
							<FileUpload.ItemSizeText>{formatFileSize(file.size)}</FileUpload.ItemSizeText>
							<FileUpload.ItemDeleteTrigger class="button-ico-primary h-6 w-6">
								<Ico icon={deleteIcon} size={deleteIconSize} />
							</FileUpload.ItemDeleteTrigger>
						</div>
					</FileUpload.Item>
				{/each}
				{#if (fileUpload().rejectedFiles?.length ?? 0) > 0}
					<p class="text-xs text-error-700-300">{resolvedRejectedText}</p>
				{/if}
			</FileUpload.ItemGroup>
		{/snippet}
	</FileUpload.Context>
</FileUpload>
