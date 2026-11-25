// Convertigo Studio application viewer only: window.alert() shows its message in a small overlay of the page instead
// of a native dialog that blocks the Studio until someone answers it. The message also goes to the Studio log through
// the console (the Studio listens for the prefix below). A deployed application keeps the browser's alert().
(function () {
	if (window.__c8oViewerAlert) {
		return;
	}
	var prefix = "[Convertigo viewer] alert: ";
	// kept now: the application may replace console.warn later
	var warn = console.warn.bind(console);
	var pending = [];
	var list = null;

	function text(value) {
		try {
			return value === undefined ? "" : String(value);
		} catch (e) {
			return "[unprintable value]";
		}
	}

	function container() {
		if (list && list.isConnected) {
			return list;
		}
		var parent = document.body || document.documentElement;
		if (!parent) {
			return null;
		}
		var host = document.createElement("c8o-viewer-alerts");
		host.setAttribute("style", "all: initial; position: fixed; z-index: 2147483647; top: 8px; left: 50%;"
			+ " transform: translateX(-50%); width: min(480px, calc(100% - 16px)); pointer-events: none;");
		// the application styles cannot reach the overlay, and its own styles do not leak into the application
		var root = host.attachShadow ? host.attachShadow({ mode: "open" }) : host;
		var style = document.createElement("style");
		style.textContent = ".alert { pointer-events: auto; display: flex; gap: 12px; align-items: flex-start;"
			+ " margin-bottom: 8px; padding: 12px 14px; border-radius: 8px; background: #263238; color: #fff;"
			+ " box-shadow: 0 4px 16px rgba(0, 0, 0, .35); font: 14px/1.4 -apple-system, 'Segoe UI', Roboto, sans-serif; }"
			+ " .message { flex: 1; white-space: pre-wrap; overflow-wrap: anywhere; max-height: 40vh; overflow: auto; }"
			+ " button { flex: none; cursor: pointer; border: 0; border-radius: 4px; padding: 4px 12px;"
			+ " background: #fff; color: #263238; font: inherit; font-weight: 600; }";
		list = document.createElement("div");
		root.appendChild(style);
		root.appendChild(list);
		parent.appendChild(host);
		return list;
	}

	function show(message) {
		var target = container();
		if (!target) {
			pending.push(message);
			return;
		}
		var item = document.createElement("div");
		item.className = "alert";
		item.setAttribute("role", "alert");
		var content = document.createElement("div");
		content.className = "message";
		content.textContent = message;
		var close = document.createElement("button");
		close.type = "button";
		close.textContent = "OK";
		close.setAttribute("aria-label", "Close this alert");
		close.addEventListener("click", function () {
			item.remove();
		});
		item.appendChild(content);
		item.appendChild(close);
		target.appendChild(item);
	}

	document.addEventListener("DOMContentLoaded", function () {
		var messages = pending;
		pending = [];
		for (var i = 0; i < messages.length; i++) {
			show(messages[i]);
		}
	});

	Object.defineProperty(window, "__c8oViewerAlert", { value: { show: show } });
	window.alert = function (message) {
		var value = text(message);
		try {
			warn(prefix + value);
		} catch (e) {
			// the overlay is enough
		}
		show(value);
	};
})();
