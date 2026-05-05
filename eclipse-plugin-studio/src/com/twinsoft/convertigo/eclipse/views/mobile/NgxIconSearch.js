(function() {
	if (window.c8oIconSearchReady) {
		if (window.c8oFilterIcons) {
			window.c8oFilterIcons();
		}
		return;
	}
	window.c8oIconSearchReady = true;

	function getIconName(anchor) {
		var use = anchor.querySelector("use");
		if (!use) {
			return "";
		}
		var href = use.getAttribute("href") || use.getAttribute("xlink:href") || "";
		return href.replace(/^#/, "");
	}

	function normalize(value) {
		return (value || "").toLowerCase().replace(/[-_]/g, " ");
	}

	var anchors = Array.prototype.slice.call(document.querySelectorAll("a")).filter(function(anchor) {
		return !!getIconName(anchor);
	});
	if (anchors.length === 0) {
		return;
	}

	var bar = document.createElement("div");
	bar.id = "c8o-icon-search";

	var input = document.createElement("input");
	input.type = "search";
	input.placeholder = "Filter icons...";
	input.setAttribute("aria-label", "Search icons");
	input.setAttribute("autocomplete", "off");
	input.setAttribute("spellcheck", "false");

	var styles = document.createElement("div");
	styles.className = "c8o-icon-styles";

	var styleInputs = {};
	["standard", "sharp", "outline"].forEach(function(style) {
		var label = document.createElement("label");
		var checkbox = document.createElement("input");
		checkbox.type = "checkbox";
		checkbox.checked = true;
		checkbox.setAttribute("data-c8o-icon-style", style);
		label.appendChild(checkbox);
		label.appendChild(document.createTextNode(style));
		styles.appendChild(label);
		styleInputs[style] = checkbox;
	});

	var count = document.createElement("span");
	count.className = "c8o-icon-count";

	bar.appendChild(input);
	bar.appendChild(styles);
	bar.appendChild(count);
	bar.addEventListener("click", function(event) {
		event.stopPropagation();
	});
	bar.addEventListener("mousedown", function(event) {
		event.stopPropagation();
	});
	document.body.insertBefore(bar, document.body.firstChild);

	function getIconStyle(name) {
		if (/-sharp$/.test(name)) {
			return "sharp";
		}
		if (/-outline$/.test(name)) {
			return "outline";
		}
		return "standard";
	}

	anchors.forEach(function(anchor) {
		var name = getIconName(anchor);
		anchor.setAttribute("data-c8o-icon-search", normalize(name + " " + anchor.textContent));
		anchor.setAttribute("data-c8o-icon-style", getIconStyle(name));
		anchor.title = name;
	});

	window.c8oFilterIcons = function() {
		var query = normalize(input.value).trim();
		var terms = query ? query.split(/\s+/) : [];
		var visible = 0;
		anchors.forEach(function(anchor) {
			var text = anchor.getAttribute("data-c8o-icon-search") || "";
			var style = anchor.getAttribute("data-c8o-icon-style") || "standard";
			var match = terms.every(function(term) {
				return text.indexOf(term) !== -1;
			}) && styleInputs[style].checked;
			anchor.style.display = match ? "" : "none";
			if (match) {
				visible++;
			}
		});
		count.textContent = visible + " / " + anchors.length;
	};

	input.addEventListener("input", window.c8oFilterIcons);
	Object.keys(styleInputs).forEach(function(style) {
		styleInputs[style].addEventListener("change", window.c8oFilterIcons);
	});
	window.c8oFilterIcons();
})();
