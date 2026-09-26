/**
 * The reference of a project, as the engine reads it (ProjectUrlParser):
 * <project name>=<git or http URL>[:path=<subpath>][:branch=<branch>][:autoPull=true].
 */

const GIT = /^(.+?)=(.+\/(?:(.*?)\/\.git)|(?:.*\/(.*?)\.git))(.*)$/;
const OPTION = /:(.+?)=([^:]*)/g;
const HTTP = /^(?:(.+?)=)?(https?:\/\/.*\/(.*?)\.(?:[zZ][iI][pP]|[cC][aA][rR]).*?)(:.*)?$/;

/**
 * @typedef {{ projectName: string, url: string, path: string, branch: string, autoPull: boolean }} ProjectReference
 */

/**
 * @param {string} value
 * @returns {ProjectReference}
 */
export function parseProjectReference(value) {
	const text = String(value ?? '');
	const reference = { projectName: '', url: '', path: '', branch: '', autoPull: false };
	const git = GIT.exec(text);
	if (git) {
		reference.projectName = git[1];
		reference.url = git[2];
		for (const [, key, option] of git[5].matchAll(OPTION)) {
			if (key === 'path') {
				reference.path = option;
			} else if (key === 'branch') {
				reference.branch = option;
			} else if (key === 'autoPull') {
				reference.autoPull = option.toLowerCase() === 'true';
			}
		}
		return reference;
	}
	const http = HTTP.exec(text);
	if (http) {
		reference.projectName = http[1] ?? http[3];
		reference.url = http[2];
		return reference;
	}
	reference.projectName = text.replace(/=.*/, '');
	return reference;
}

/**
 * @param {ProjectReference} reference
 * @returns {string}
 */
export function formatProjectReference({ projectName, url, path, branch, autoPull }) {
	let value = String(projectName ?? '').trim();
	if (String(url ?? '').trim()) {
		value += `=${url.trim()}`;
		if (String(path ?? '').trim()) {
			value += `:path=${path.trim()}`;
		}
		if (String(branch ?? '').trim()) {
			value += `:branch=${branch.trim()}`;
		}
		if (autoPull) {
			value += ':autoPull=true';
		}
	}
	return value;
}
