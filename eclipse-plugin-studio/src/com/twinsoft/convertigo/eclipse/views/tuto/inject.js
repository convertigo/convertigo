var 	bSuccess = false;

document.addEventListener("DOMContentLoaded", () => {
	if (!("$" in window)) {
		return;
	}
	console.log("init");
	$(document).on("click", ".expandablegif", e => IDE.message({
		"type": "imgEnter",
		"url": e.target.getAttribute("src")
	})).on("click", "a:contains('ᐅ')", e => {
		if (bSuccess || !e.originalEvent.isTrusted) {
			return true;
		}
		$(".tutoerror").fadeIn(500);
		return false;
	}).on("click", ".tutook", _ => {
		$("a:contains('ᐅ')")[0].click();
	}).on("click", ".tutocancel", _ => {
		$(".tutoerror").fadeOut(500);
		$(".tutosuccess").fadeOut(500);
	});
	
	IDE.message({
		"type": "control",
		"json": JSON.stringify(window["tuto"])
	});
}, { once: true });

window.tutoGoNext = function () {
	$(".tutoerror").hide();
	$(".tutosuccess").fadeIn(500);
	bSuccess = true;
};
