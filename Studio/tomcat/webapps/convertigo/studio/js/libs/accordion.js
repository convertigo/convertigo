/* Original source: https://www.w3schools.com/howto/howto_js_accordion.asp */
function accordionify() {
	var acc = document.getElementsByClassName("accordion");
	for (var i = 0; i < acc.length; ++i) {
	    acc[i].onclick = function () {
	        this.classList.toggle("active");
	        var panel = this.nextElementSibling;
	        panel.style.display = panel.style.display === "" ? "none" : "";
	    }
	}
}
