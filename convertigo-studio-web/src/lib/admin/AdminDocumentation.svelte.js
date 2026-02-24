const ADMIN_DOC_BASE =
	'https://doc.convertigo.com/documentation/8.4.x/operating-guide/using-convertigo-administration-console/';

const ADMIN_PAGE_ANCHORS = {
	'/admin': 'home-page',
	'/admin/config': 'configuration-page',
	'/admin/projects': 'projects',
	'/admin/connections': 'connections',
	'/admin/logs': 'logs-1',
	'/admin/fullsync': 'fullsync',
	'/admin/cache': 'cache-1',
	'/admin/scheduler': 'scheduler',
	'/admin/roles': 'roles',
	'/admin/certificates': 'certificates',
	'/admin/keys': 'keys',
	'/admin/symbols': 'global-symbols'
};

const CONFIG_CATEGORY_ANCHORS = {
	Main: 'main-parameters',
	Session: 'session-management',
	Account: 'accounts-and-security',
	Logs: 'logs',
	Context: 'real-time-activity-monitoring',
	XmlGeneration: 'xml-generation',
	HttpClient: 'http-client',
	Network: 'network',
	Proxy: 'proxy',
	Ssl: 'ssl',
	Cache: 'cache',
	Analytics: 'analytics',
	Notifications: 'notifications',
	MobileBuilder: 'mobile-builder',
	FullSync: 'full-sync'
};

function normalizePath(pathname = '') {
	const raw = String(pathname ?? '').trim();
	if (!raw) return '';
	const noQuery = raw.split(/[?#]/, 1)[0];
	if (noQuery.length > 1 && noQuery.endsWith('/')) {
		return noQuery.slice(0, -1);
	}
	return noQuery;
}

function withAnchor(anchor) {
	if (!anchor) return undefined;
	return `${ADMIN_DOC_BASE}#${anchor}`;
}

export function getAdminPageDocHref(pathname) {
	const normalized = normalizePath(pathname);
	if (!normalized.startsWith('/admin')) return undefined;
	const directAnchor = ADMIN_PAGE_ANCHORS[normalized];
	if (directAnchor) {
		return withAnchor(directAnchor);
	}
	const firstSegment = normalized.split('/').slice(0, 3).join('/');
	return withAnchor(ADMIN_PAGE_ANCHORS[firstSegment]);
}

export function getAdminConfigCategoryDocHref(categoryName) {
	const categoryKey = String(categoryName ?? '').trim();
	return withAnchor(CONFIG_CATEGORY_ANCHORS[categoryKey]);
}

export function getAdminConfigDocHref(categoryName) {
	return getAdminConfigCategoryDocHref(categoryName);
}
