export function formatDuration(ms) {
	if (ms == null) {
		return null;
	}
	const totalSeconds = Math.max(0, Math.floor(ms / 1000));
	const hours = Math.floor(totalSeconds / 3600);
	const minutes = Math.floor((totalSeconds % 3600) / 60);
	const seconds = totalSeconds % 60;
	const pad = (n) => String(n).padStart(2, '0');
	return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

export function formatDate(timestamp) {
	if (timestamp == null) return '';
	return new Date(timestamp).toISOString().split('T')[0];
}

export function formatTime(timestamp) {
	if (timestamp == null) return '';
	return new Date(timestamp).toISOString().split('T')[1].split('Z')[0].replace('.', ',');
}

export function splitDateTime(value) {
	if (!value) return { date: '', time: '' };
	const [date, time = ''] = String(value).split(' ');
	return { date, time };
}

export function joinDateTime(date, time, fallback) {
	return date ? `${date} ${time || fallback}` : '';
}

export function parseDateTimeMs(value) {
	if (!value) return undefined;
	const ms = Date.parse(String(value).replace(',', '.'));
	return Number.isNaN(ms) ? undefined : ms;
}
