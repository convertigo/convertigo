// The bridge of the desktop Studio to its page: the developer tools on the preview of an application, or on
// the application in a window of its own; and to the pages it shows in frames, as the Eclipse Studio: the
// Marketplace, and the registration page of Convertigo Cloud, which gives the PSC of the Studio.
const { contextBridge, ipcRenderer, webFrame } = require('electron');

if (window !== window.top) {
	// the Marketplace in its frame talks to the Studio as to the Eclipse Studio, through window.java: its
	// messages go to the Studio page, those of the Studio page to its window.receiveFromJava
	if (/^marketplace\./.test(location.hostname)) {
		contextBridge.exposeInMainWorld('java', {
			receiveFromJS: (json) => {
				try {
					window.parent.postMessage(JSON.parse(String(json)), '*');
				} catch {
					// not a message
				}
			}
		});
		window.addEventListener('message', (event) => {
			if (event.source === window.parent) {
				void webFrame.executeJavaScript(`window.receiveFromJava && window.receiveFromJava(${JSON.stringify(event.data)})`);
			}
		});
	} else if (location.origin === 'https://c8ocloud.convertigo.net') {
		// the registration page of Convertigo Cloud gives the PSC of the Studio through window.studio, as to the
		// Eclipse Studio: it goes to the Studio page of the engine that shows the page, only
		const studioOrigin = location.ancestorOrigins?.[0] ?? '';
		contextBridge.exposeInMainWorld('studio', {
			getUsername: () => null,
			getSecret: () => null,
			setPSC: (psc) => {
				if (window.parent === window.top && /^http:\/\/localhost:\d+$/.test(studioOrigin)) {
					window.parent.postMessage({ type: 'convertigo.studio.psc', psc: String(psc ?? '') }, studioOrigin);
				}
			},
			setReady: () => {}
		});
	}
} else if (location.hostname === 'localhost') {
	// the Studio page of the engine only, not the pages of the windows it opens elsewhere
	contextBridge.exposeInMainWorld('convertigoStudio', {
		/** inspects the element at a point of the page, as an element of the application its preview shows */
		inspect: (x, y) => ipcRenderer.send('studio:inspect', Number(x), Number(y)),
		/**
		 * opens the page of an application in a window of its own, with its developer tools, which show its code
		 * only, and the session data of its preview
		 */
		debugApplication: (url, sessionStorage) =>
			ipcRenderer.send('studio:debug-application', String(url), sessionStorage && typeof sessionStorage === 'object' ? { ...sessionStorage } : {})
	});
}
