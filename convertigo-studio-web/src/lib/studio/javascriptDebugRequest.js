// The explicit request marker stays in memory, never in storage or request URLs.
let sessionId = '';
export function setJavascriptDebugSession(id) {
	sessionId = id || '';
}
export function javascriptDebugParameters() {
	return sessionId ? { __javascriptDebug: sessionId } : {};
}
