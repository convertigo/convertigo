function addMashupAttribute(oNode,output) {
      if(oNode.hasChildNodes()){
            for (var i=0; i < oNode.childNodes.length; i++) {
                  addMashupAttribute(oNode.childNodes[i],output);
            }
      }else{
            if(oNode.nodeValue!=null) output.setAttribute(oNode.parentNode.tagName,oNode.nodeValue);
      }
}

function doMashupEvent(event, eventName, node){
	if (window.parent.tcResponseItemClicked) {
		if(eventName.indexOf('FormSubmitted_')==0) {
			sendFormSubmitted(eventName);
		}
		else {
			var event = event?event:window.event;
			sendItemClicked(node);
		}
	}
}

function sendItemClicked(obj) {
      window.parent.tcResponseItemClicked(window.name, obj);
}

function sendFormSubmitted(eventName) {
      window.parent.tcFormResponseSubmitted( window.name, document, eventName );
}

function showDiv(event) {
	var event = event?event:window.event;
	var pgY = mouseY(event) + 2;
	var pgX = mouseX(event) + 5;
	elt = document.getElementById("mashupTooltip");
	elt.style.display = "block";
	elt.style.top = pgY + "px";
	elt.style.left = pgX + "px";
}

function hideDiv(event) {
	var event = event?event:window.event;
	elt = document.getElementById("mashupTooltip");
	elt.style.display = "none";
}

function mouseX(evt) {
	var ret = null;
	if (evt.pageX) {
		ret = evt.pageX;
	}
	else if (evt.clientX) {
		ret = evt.clientX + (document.documentElement.scrollLeft ? document.documentElement.scrollLeft : document.body.scrollLeft);
	}
	
	return ret;	
}
function mouseY(evt) {
	var ret = null;
	if (evt.pageY) {
		ret = evt.pageY;
	}
	else if (evt.clientY) {
		ret = evt.clientY + (document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop);
	}
	
	return ret;
}