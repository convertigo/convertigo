<script>
	import { browser } from '$app/environment';
	import Card from '$lib/admin/components/Card.svelte';
	import { getUrl } from '$lib/utils/service';

	let iframe = $state();
	function onload() {
		const iframeDoc = iframe.contentWindow.document;
		const observer = new MutationObserver((mutations) => {
			mutations.forEach((mutation) => {
				mutation.addedNodes.forEach((node) => {
					// @ts-ignore
					if (node.id === 'dashboard') {
						const breadcrumbText = iframeDoc.querySelector(
							'.faux-header__breadcrumbs-element'
						)?.textContent;
						if (breadcrumbText === 'Job Configuration') {
							const breadcrumbs = iframeDoc.querySelector('.faux-header__breadcrumbs');
							const backButton = document.createElement('button');
							backButton.type = 'button';
							backButton.className = 'faux-header__doc-header-backlink';
							backButton.innerHTML =
								'<i class="faux-header__doc-header-backlink__icon fonticon fonticon-left-open"></i>';
							breadcrumbs.prepend(backButton);
							backButton.addEventListener('click', () => {
								iframe.contentWindow.history.back();
							});
						}
					}

					if (!iframeDoc.getElementById('btRefresh')) {
						const headerDocsLeft = iframeDoc.querySelector('#header-docs-left > div');
						const breadcrumbs = iframeDoc.querySelector('#breadcrumbs > div');
						const refreshButton = document.createElement('button');
						refreshButton.id = 'btRefresh';
						refreshButton.type = 'button';
						refreshButton.className = 'faux-header__doc-header-backlink';
						refreshButton.textContent = '↻';
						headerDocsLeft?.prepend(refreshButton);
						breadcrumbs?.prepend(refreshButton);
						refreshButton.addEventListener('click', () => {
							iframe.contentWindow.location.reload();
						});
					}

					if (
						iframeDoc.querySelector('.fauxton-table-list th:nth-child(2)')?.textContent === 'Size'
					) {
						const th = iframeDoc.querySelector('.fauxton-table-list th:nth-child(2)');
						const tds = iframeDoc.querySelectorAll('.fauxton-table-list tr td:nth-child(2)');
						th.remove();
						tds.forEach((td) => td.remove());
					}

					const selectorsToRemove = [
						{ selector: '.design-doc-body li a', contains: 'Metadata' },
						{ selector: '.nav-list li a', contains: 'Permissions' },
						{ selector: '.nav-list li a', contains: 'Changes' }
					];
					for (let { selector, contains } of selectorsToRemove) {
						const elements = iframeDoc.querySelectorAll(selector);
						elements.forEach((element) => {
							if (element.textContent.includes(contains)) {
								element.closest('li').remove();
							}
						});
					}
				});
			});
		});

		const config = { childList: true, subtree: true };
		observer.observe(iframeDoc.body, config);
	}
</script>

<Card title="Full Sync" class="h-full">
	{#if browser}
		<iframe
			bind:this={iframe}
			src={getUrl().replace('/services/', '/_utils/')}
			title="fullsync"
			class="h-full rounded-xl"
			{onload}
		></iframe>
	{/if}
</Card>
