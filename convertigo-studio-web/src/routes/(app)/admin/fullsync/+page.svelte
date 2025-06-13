<script>
	import { browser } from '$app/environment';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import Light from '$lib/common/Light.svelte';
	import { getUrl } from '$lib/utils/service';

	let iframe = $state();

	$effect(() => {
		iframe?.contentWindow?.document?.documentElement?.classList.toggle('transition', true);
		iframe?.contentWindow?.document?.documentElement?.classList.toggle('dark', Light.dark);
	});

	function onload() {
		const iframeDoc = iframe.contentWindow.document;

		iframeDoc.documentElement.classList.toggle('dark', Light.dark);
		const style = iframeDoc.createElement('style');
		style.textContent = `
			.transition {
				transition: filter 250ms ease;	
			}
			html {
				filter: invert(0) hue-rotate(0deg);
			}
			html.dark {
				filter: invert(1) hue-rotate(180deg);
			}
			.dark img, .dark video {
				filter: invert(1) hue-rotate(180deg);
			}
		`;
		iframeDoc.head.appendChild(style);
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
		iframe.classList.toggle('opacity-0', false);
	}
</script>

{#if browser}
	<MaxRectangle>
		<iframe
			bind:this={iframe}
			src={getUrl().replace('/services/', '/_utils/')}
			title="fullsync"
			class="h-full w-full rounded-xl opacity-0 transition-opacity duration-200"
			{onload}
		></iframe>
	</MaxRectangle>
{/if}
