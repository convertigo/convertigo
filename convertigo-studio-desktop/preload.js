// The bridge of the desktop Studio to its page: the developer tools on the preview of an application, or on
// the application in a window of its own.
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
	}
} else contextBridge.exposeInMainWorld('convertigoStudio', {
	/** inspects the element at a point of the page, as an element of the application its preview shows */
	inspect: (x, y) => ipcRenderer.send('studio:inspect', Number(x), Number(y)),
	/**
	 * opens the page of an application in a window of its own, with its developer tools, which show its code
	 * only, and the session data of its preview
	 */
	debugApplication: (url, sessionStorage) =>
		ipcRenderer.send('studio:debug-application', String(url), sessionStorage && typeof sessionStorage === 'object' ? { ...sessionStorage } : {})
});
