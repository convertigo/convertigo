(() => {
    const CUSTOMIZER_VERSION = '2026.05.28';
    if (window.ConvertigoBaserowCustomizer?.started) {
        window.ConvertigoBaserowCustomizer.clean?.();
        return;
    }

    const _guidedToursToComplete = ['sidebar', 'database'];
    const _unsupportedViewTypes = ['form', 'gallery'];
    let _guidedTourCompletion = null;
    let _lastGuidedTourClick = 0;
    let _commentsSidebarUnregistered = false;
    let _unsupportedRegistryTypesHidden = false;

    function _getNuxtApp() {
        return window.$nuxt && window.$nuxt.$store ? window.$nuxt : null;
    }

    function _getCompletedGuidedTours() {
        const app = _getNuxtApp();
        return app?.$store?.state?.auth?.user?.completed_guided_tours || [];
    }

    async function _markGuidedToursCompleted() {
        const app = _getNuxtApp();
        const store = app?.$store;
        const user = store?.state?.auth?.user;
        if (!app || !store || !user) {
            return false;
        }

        const current = user.completed_guided_tours || [];
        const completed_guided_tours = [...new Set([...current, ..._guidedToursToComplete])];
        if (_guidedToursToComplete.every(it => current.includes(it))) {
            return true;
        }

        if (!_guidedTourCompletion) {
            _guidedTourCompletion = (async () => {
                if (store.dispatch) {
                    await store.dispatch('auth/update', { completed_guided_tours });
                } else if (app.$client?.patch) {
                    const { data } = await app.$client.patch('/user/account/', { completed_guided_tours });
                    store.dispatch?.('auth/forceUpdateUserData', { user: data });
                } else if (store.state.auth.token) {
                    const response = await fetch('/api/user/account/', {
                        method: 'PATCH',
                        headers: {
                            'Content-Type': 'application/json',
                            Authorization: `JWT ${store.state.auth.token}`
                        },
                        body: JSON.stringify({ completed_guided_tours })
                    });
                    if (response.ok) {
                        const data = await response.json();
                        store.dispatch?.('auth/forceUpdateUserData', { user: data });
                    }
                }
            })().catch(() => null).finally(() => {
                _guidedTourCompletion = null;
            });
        }

        return false;
    }

    function _completeGuidedTours() {
        _markGuidedToursCompleted();
        if (_guidedToursToComplete.every(it => _getCompletedGuidedTours().includes(it))) {
            return;
        }

        const step = document.querySelector('.guided-tour-step');
        if (!step) {
            return;
        }

        const now = Date.now();
        if (now - _lastGuidedTourClick < 250) {
            return;
        }

        const button = step.querySelector('.guided-tour-step__foot button, button');
        if (button) {
            _lastGuidedTourClick = now;
            button.click();
        }
    }

    function _normalizeText(text) {
        return (text || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
    }

    function _hidePremiumCommentsSidebar() {
        const app = _getNuxtApp();
        try {
            if (!_commentsSidebarUnregistered && app?.$registry?.exists?.('rowModalSidebar', 'comments')) {
                app.$registry.unregister('rowModalSidebar', 'comments');
                _commentsSidebarUnregistered = true;
            }
        } catch (e) {
            // Ignore registry differences between Baserow versions.
        }

        document.querySelectorAll('.row-edit-modal-sidebar').forEach(sidebar => {
            const tabs = [...sidebar.querySelectorAll('.tabs__header .tabs__item')];
            const commentsTab = tabs.find(tab => {
                const text = _normalizeText(tab.textContent);
                return text.includes('commentaire') || text.includes('comment');
            });
            const historyTab = tabs.find(tab => {
                const text = _normalizeText(tab.textContent);
                return text.includes('historique') || text.includes('history');
            });

            if (commentsTab?.classList.contains('tabs__item--active') && historyTab) {
                historyTab.click();
            }
            commentsTab?.remove();
            sidebar.querySelectorAll('.row-comments').forEach(it => it.remove());
        });
    }

    function _hideUnsupportedBaserowFeatures() {
        const app = _getNuxtApp();
        try {
            if (!_unsupportedRegistryTypesHidden && app?.$registry) {
                ['builder', 'dashboard'].forEach(type => {
                    if (app.$registry.exists?.('application', type)) {
                        const applicationType = app.$registry.get('application', type);
                        applicationType.canBeCreated = () => false;
                        applicationType.isVisible = () => false;
                    }
                });
                if (app.$registry.exists?.('field', 'ai')) {
                    const aiFieldType = app.$registry.get('field', 'ai');
                    aiFieldType.isEnabled = () => false;
                    aiFieldType.isDeactivated = () => true;
                    aiFieldType.getDeactivatedClickModal = () => null;
                }
                if (app.$registry.exists?.('field', 'formula')) {
                    app.$registry.get('field', 'formula').getAdditionalFormInputComponents = () => [];
                }
                _unsupportedViewTypes.forEach(type => {
                    if (app.$registry.exists?.('view', type)) {
                        const viewType = app.$registry.get('view', type);
                        viewType.isDeactivated = () => true;
                        viewType.getDeactivatedClickModal = () => null;
                        viewType.isCompatibleWithDataSync = () => false;
                        viewType.canShare = false;
                    }
                });
                if (app.$registry.exists?.('workspaceSettings', 'generative-ai')) {
                    app.$registry.unregister('workspaceSettings', 'generative-ai');
                }
                _unsupportedRegistryTypesHidden = true;
            }
        } catch (e) {
            // Ignore registry differences between Baserow versions.
        }

        document.querySelectorAll(`${[
            '.context__menu-item:has(.baserow-icon-dashboard, .baserow-icon-application)',
            '.dashboard__application:has(.baserow-icon-dashboard, .baserow-icon-application)',
            '.tree__item:has(.baserow-icon-dashboard, .baserow-icon-application)',
            'li:has(.choice-items__link .baserow-icon-dashboard, .choice-items__link .baserow-icon-application)',
            'li.select__item:has(.baserow-icon-dashboard, .baserow-icon-application, .iconoir-magic-wand)',
            '.select__item:has(.iconoir-magic-wand)',
            '.modal-sidebar__nav-link:has(.iconoir-magic-wand)',
            '.select__footer-create-link[data-highlight="create-view-form"]',
            '.select__footer-create-link[data-highlight="create-view-gallery"]',
            '.select__footer-create-link:has(.baserow-icon-form)',
            '.select__footer-create-link:has(.baserow-icon-gallery)',
            '.select__item:has(.baserow-icon-form)',
            '.select__item:has(.baserow-icon-gallery)',
            '.select__item:has(.fa-sign-out-alt, .iconoir-log-out, .iconoir-log-out-solid)',
            '.context__menu-item:has(.fa-sign-out-alt, .iconoir-log-out, .iconoir-log-out-solid)',
            '.header__filter-item:has(.baserow-icon-form)',
            '.header__filter-item:has(.baserow-icon-gallery)',
            '.form-view'
        ].join(',')}`).forEach(it => it.remove());

        document.querySelectorAll('.create-application-context .context__menu-item').forEach(it => {
            const text = _normalizeText(it.textContent);
            if (text.includes('tableau de bord') || text.includes('dashboard') || text.includes('application')) {
                it.remove();
            }
        });
        document.querySelectorAll('.tree__heading').forEach(it => {
            const text = _normalizeText(it.textContent);
            if (text.includes('applications') || text.includes('tableaux de bord') || text.includes('dashboards')) {
                it.parentElement.remove();
            }
        });
        document.querySelectorAll('a, button, .select__item, .context__menu-item').forEach(it => {
            const text = _normalizeText(it.textContent);
            if (
                text.includes("prompt pour l'ia") ||
                text.includes("modele d'ia") ||
                text.includes("type d'ia") ||
                text.includes('openai') ||
                text.includes('ia generative') ||
                text.includes("generer a l'aide de l'ia") ||
                text.includes('generate using ai') ||
                text.includes('generate formula using ai') ||
                text.includes('ai prompt') ||
                text.includes('formulaire') ||
                text.includes('galerie') ||
                text.includes('gallery') ||
                text.includes('share form') ||
                text.includes('share the form') ||
                text.includes('se deconnecter') ||
                text.includes('deconnexion') ||
                text.includes('logout') ||
                text.includes('log out') ||
                text.includes('sign out')
            ) {
                it.remove();
            }
        });
    }

    function _redirectFromUnsupportedView() {
        const app = _getNuxtApp();
        const store = app?.$store;
        const selectedView = store?.getters?.['view/getSelected'];
        if (!_unsupportedViewTypes.includes(selectedView?.type)) {
            return;
        }

        try {
            const tableId = selectedView.table?.id || Number(app.$route?.params?.tableId);
            const nextView = (store.getters['view/getAll'] || [])
                .filter(view => !_unsupportedViewTypes.includes(view.type) && (!tableId || view.table?.id === tableId))
                .sort((a, b) => (a.order || 0) - (b.order || 0))[0];
            if (!nextView) {
                return;
            }
            app.$router?.replace?.({
                name: 'database-table',
                params: {
                    ...app.$route.params,
                    viewId: nextView.id
                },
                query: app.$route.query
            }).catch?.(() => {});
        } catch (e) {
            // Ignore navigation races while Baserow is still loading.
        }
    }

    function _clean() {
        _hidePremiumCommentsSidebar();
        _hideUnsupportedBaserowFeatures();
        _redirectFromUnsupportedView();
        document.querySelectorAll(`${[
            '.auth__logo',
            '.dashboard__footer',
            '.dashboard__help',
//        '.sidebar__user',
            '.sidebar__foot',
            '.context__menu-item:has(.iconoir-settings, .iconoir-lock)',
            '.tree__item:has(.baserow-icon-application, .iconoir-lock)',
            'li:has(.choice-items__link .iconoir-lock)',
            '.dashboard__sidebar-group:has(.fa-sign-out-alt)',
            '.context__menu-item:has(.fa-sign-out-alt, .iconoir-log-out, .iconoir-log-out-solid)',
            '.select__item:has(.fa-sign-out-alt, .iconoir-log-out, .iconoir-log-out-solid)',
            '.dashboard__resources',
            '.context__menu-item:has(.baserow-icon-dashboard)',
            '.context__menu-item:has(.baserow-icon-application)',
            '.context__menu-item:has(.baserow-icon-form)',
            '.context__menu-item:has(.baserow-icon-gallery)',
            '.tabs__item:has(.iconoir-lock)',
            'li.select__item:has(.iconoir-lock)',
            '.alert:has(.baserow-icon-gitlab)',
            'li.header__filter-item:has(.iconoir-palette)',
            '.select__footer-create-link:has(.iconoir-lock)',
            '.view-sharing__option:has(.iconoir-lock)',
            'div.radio:has(.iconoir-lock)'
        ].join(',')}`).forEach(it => it.remove());
        document.querySelectorAll('.tree__heading').forEach(it => {
            if (it.textContent.toLowerCase().includes('applications')) {
                it.parentElement.remove();
            }
        });
    }

    function _start() {
        _clean();
        _completeGuidedTours();
        const observer = new MutationObserver(() => {
            _clean();
            _completeGuidedTours();
        });
        observer.observe(document.body, { subtree: true, childList: true });
        const tourInterval = setInterval(() => {
            _clean();
            _completeGuidedTours();
            const completed = _getCompletedGuidedTours();
            if (!document.querySelector('.guided-tour-step') && _guidedToursToComplete.every(it => completed.includes(it))) {
                clearInterval(tourInterval);
            }
        }, 250);

        window.ConvertigoBaserowCustomizer = {
            version: CUSTOMIZER_VERSION,
            started: true,
            clean: _clean,
            completeGuidedTours: _completeGuidedTours
        };
    }

    if (document.body) {
        _start();
    } else {
        window.addEventListener('DOMContentLoaded', _start, { once: true });
    }
})();
