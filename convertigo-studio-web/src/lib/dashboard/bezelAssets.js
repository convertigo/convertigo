import { asset } from '$app/paths';

// The static files typed by AssetPath cannot name a device computed at runtime.

/** @param {string} id the id of a device of Bezels.js */
export function bezelImage(id) {
	return asset(/** @type {import('$app/types').AssetPath} */ (`bezels/${id}.webp`));
}

/** @param {string} id the id of a device of Bezels.js */
export function bezelThumbnail(id) {
	return asset(/** @type {import('$app/types').AssetPath} */ (`bezels/thumbnails/${id}.webp`));
}
