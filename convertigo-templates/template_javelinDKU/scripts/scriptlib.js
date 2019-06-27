/*
 * Copyright (c) 1999-2004 TWinSoft sarl. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of TWinSoft sarl.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of TWinSoft  sarl or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * TWinSoft  makes  no  representations  or  warranties  about   the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness  for  a particular purpose, or non-infringement. TWinSoft
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $Workfile: scriptlib.js $
 * $Author: Davidm $
 * $Revision: 9 $
 * $Date: 11/12/07 11:16 $
 */

/********************************************************************\
***                          VARIABLES                             ***
\********************************************************************/
var isIE = navigator.userAgent.indexOf('Gecko') == -1 ? true: false;

// perform initilizations
var currentObject = null;
var focusOnField = null;
var currentFieldOnFocus = '';
var oldSpan = null;
var oldSpanClass = null;
var oldTrBgcolor = null;

// More initilizations
var hasCalendar = false;
var hasTableAction = false;
var xmlhttp = getXmlHttpRequest();
var xmlDocument = null;
var MSXMLVersion;
var javelin_formElements = new Array();

var enableBuffering = false;
var isWaiting = false;
var fifo = null;

var lastSortColon = null;

var XSLstep = null;

// Bench
var benchTime = false;
var requestTime = null;
var totalTime = null;

var xslDom = null;
var checkDomDirty; // window timer object for domDirty check
var bCheckDomDirty = false; // boolean for domDirty check

function initVar(){
	if(benchTime){
		requestTime	= new gTime("requestTime");
		totalTime	= new gTime("totalTime");		
	}

	if(enableBuffering){
		fifo = new FIFO();
	}
	
	resetHandler();
}

/********************************************************************\
***                          FIN VARIABLES                         ***
\********************************************************************/
/********************************************************************\
***                              CLASS                             ***
\********************************************************************/
/**************************** Class FIFO ****************************/
function FIFOelt(elt){
	this.value=elt;
	this.next=null;
}

function FIFO() {
	this.first=null;
	this.last=null;
}

function FIFO_push(elt) {
	if(!this.first){
		this.first=this.last=new FIFOelt(elt);
	}else{
		this.last.next=new FIFOelt(elt);
		this.last = this.last.next;
	}
}

function FIFO_pop() {
	if(!this.first)return null;
	elt=this.first.value;
	this.first=this.first.next;
	return elt;
}

function FIFO_empty() { return (!this.first)?true:false; }

FIFO.prototype.push=FIFO_push;
FIFO.prototype.pop=FIFO_pop;
FIFO.prototype.empty=FIFO_empty;
/*************************** Fin Class fifo *************************/

/*************************** String addon methods *******************/
String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g,"");
}
String.prototype.ltrim = function() {
	return this.replace(/^\s+/,"");
}
String.prototype.rtrim = function() {
	return this.replace(/\s+$/,"");
}

/*************************** Class twsEvent *************************/

function twsEvent(e){
	if(!e) e= window.event;
	this.type 			= e.type;
	this.canBubble	= e.canBubble;
	this.cancelable	= e.cancelable;
	this.ctrlKey		= e.ctrlKey;
	this.altKey			= e.altKey;
	this.shiftKey		= e.shiftKey;
	this.metkey			= e.metkey;
	this.keyCode		= e.keyCode;
	this.charCode		= e.charCode;
}

function twsEvent_makeEvent(){
	ke = document.createEvent("KeyEvents");
	ke.initKeyEvent(
		this.type,
    this.canBubble,
    this.cancelable,
    null,
    this.ctrlKey,
    this.altKey,
    this.shiftKey,
    this.metkey,
    this.keyCode,
    this.charCode
	);
	return ke;
}

twsEvent.prototype.preventDefault		= f_void;
twsEvent.prototype.stopPropagation	= f_void;
twsEvent.prototype.makeEvent				= twsEvent_makeEvent;
/*********************** Fin Class twsEvent *************************/
/*************************** Class gTime ****************************/
function gTime(name,autoStart){
	this.t_start			= null;
	this.t_stop				= null;
	this.name					= name;
	if(autoStart){
		var dat = new Date();
		this.t_start = dat.getTime();
	}
}

function gTime_start(name){
	if(name)this.name = name;
	var dat = new Date();
	this.t_start = dat.getTime();
	return this.t_start;
}

function gTime_stop(autoStatus){
	var dat = new Date();
	this.t_stop = dat.getTime();
	if(autoStatus)this.dif(true);
	return this.t_stop;
}

function gTime_dif(autoStatus){
	var res = " ["+this.name+":"+(this.t_stop-this.t_start)+"ms]";
	if(benchTime && autoStatus)window.status += res;
	return res;
}

gTime.prototype.start			= gTime_start;
gTime.prototype.stop			= gTime_stop;
gTime.prototype.dif				= gTime_dif;
/*********************** Fin Class gTime ****************************/
/********************************************************************\
***                           FIN CLASS                            ***
\********************************************************************/

function resetHandler(){
	if (isIE) {
		// Microsoft IE KeyHandler
		if(enableBuffering){
			document.onkeydown 	= onPressed;
			document.onkeypress	= onPressed;
			document.onkeyup		= f_void;
		}else{
			document.onkeydown 	= handleKeypress;
			document.onkeypress	= null;
			document.onkeyup		= null;
		}
	
		// disable default action while pressing F1 fo IE
		document.onhelp 	= f_void;
	} else {
		// Gecko based browsers (FireFOX, Safari, Mozilla, ....);
		//document.addEventListener('keypress', handleKeypress, true);
		if(enableBuffering){
			window.onkeydown 	= f_void;
			window.onkeypress	= onPressed;
			window.onkeyup		= f_void;
		}else{
			window.onkeydown 	= handleKeypress;
			window.onkeypress	= null;
			window.onkeyup		= null;
		}
	}
}

/************************ Functions sort ****************************/

function setXSLparam(name,value){
	var elements = xslDom.getElementsByTagName(((isIE)?"xsl:":"")+"param");
	for(var i = 0;i<elements.length;i++){
		if(elements[i].getAttribute("name") == name){
			if(value)elements[i].childNodes[0].nodeValue = value;
			return elements[i].childNodes[0].nodeValue;
		}
	}
	return null;
}

function getXSLparam(name){
	return setXSLparam(name,null);
}

function onDataSortClic(type){
	var imgURL;
	if(lastSortColon){
		var img = document.getElementById("CoLiMg"+lastSortColon);
		if(img) imgURL = img.src;
	}
	setXSLparam("sortDataType",type);
	XSLT_transformation();
	getPositions();
	resize();
	if(lastSortColon){
		var img = document.getElementById("CoLiMg"+lastSortColon);
		if(img && imgURL) img.src=imgURL;
	}
}

function onSortClicTD(sortColon){
	var imgURL;
	setXSLparam("doSort","true");
	setXSLparam("sortColon",sortColon);
	if((lastSortColon == sortColon) && (getXSLparam("sortOrder") == "ascending")){
		imgURL="images/descending.gif";
		setXSLparam("sortOrder","descending");
	}else{
		imgURL = "images/ascending.gif";
		setXSLparam("sortOrder","ascending");
		lastSortColon = sortColon;
	}
	XSLT_transformation();
	getPositions();
	resize();
	var img = document.getElementById("CoLiMg"+sortColon);
	if(img) img.src=imgURL;
}

function onUnsortClic(){
	lastSortColon = null;
	setXSLparam("doSort","false");
	setXSLparam("sortColon","-1");
	XSLT_transformation();
	getPositions();
	resize();
}

/********************* Fin Functions sort ****************************/

function f_void(){return false;}
function t_void(){return true;}

function getId(el) {
	return document.getElementById(el);
}

function isFocusable(el) {
	//alert("el=" + el.id);
	//alert("el.disabled='" + el.disabled + "'");
	var ret = false;
	if ((isField(el)) && (el.type != 'hidden') && (el.disabled != true)) {
		ret = true;
	}
	return ret;
}

function isSelectable(el) {
	//alert("el=" + el.id);
	var ret = false;
	if ((isField(el)) && ((el.type == 'text') || (el.type == 'password'))) {
		ret = true;
	}
	return ret;
}

function focusPrevField(){
	if (document.javelin_form.elements.length > 0) {
		var prvFld = getId(currentFieldOnFocus).getAttribute("previousField");
		// Continuous field case
		if (prvFld != null) {
			prvFld = getId(prvFld);
			if (isFocusable(prvFld)) {
				prvFld.focus();
				if (isSelectable(prvFld)) {
					prvFld.select();
				}
			}
		}
		// Case this field is not part of a continuous field
		else {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid > 0) {
				myid--;
				if (isFocusable(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					if (isSelectable(document.javelin_form.elements[myid])) {
						document.javelin_form.elements[myid].select();
					}
					break;
				}
			}
			if (myid == 0) {
				focusOnLastField();
			}
		}
	}	
}

function focusNextField(){
	if (document.javelin_form.elements.length > 0) {
		var nxtFld = getId(currentFieldOnFocus).getAttribute("nextField");
		// Continuous field case
		if (nxtFld != null) {
			nxtFld = getId(nxtFld);
			if (isFocusable(nxtFld)) {
				nxtFld.focus();
				if (isSelectable(nxtFld)) {
					nxtFld.select();
				}
			}
		}
		// Case this field is not part of a continuous field
		else {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid < document.javelin_form.elements.length-1) {
				myid++;
				if (isFocusable(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					if (isSelectable(document.javelin_form.elements[myid])) {
						document.javelin_form.elements[myid].select();
					}
					break;
				}
			}
			if (myid == document.javelin_form.elements.length-1) {
				myid = -1;
				while (myid < document.javelin_form.elements.length-1) {
					myid++;
					if (isFocusable(document.javelin_form.elements[myid])) {
						document.javelin_form.elements[myid].focus();
						if (isSelectable(document.javelin_form.elements[myid])) {
							document.javelin_form.elements[myid].select();
						}
						break;
					}
				}
			}
		}
	}
}

function focusOnFirstField() {
	var firstField = false;
	if (document.javelin_form.elements.length > 0) {
		//alert("form hasElements : '" + document.javelin_form.elements.length + "'");
		for (i=0;i < document.javelin_form.elements.length-1;i++) {
			//alert("analyzing '" + document.javelin_form.elements[i] + "'");
			if (isFocusable(document.javelin_form.elements[i])) {
				//alert("is a field : '" + document.javelin_form.elements[i].id + "'");
				firstField = document.javelin_form.elements[i];
				break;				
			}
		}
	}
	if (firstField) {
		firstField.focus();
		if (isSelectable(firstField)) {
			firstField.select();
		}
		return true;
	}
	else {
		return false;
	}
}

function focusOnLastField() {
	myid = document.javelin_form.elements.length;
	while (myid > 0) {
		myid--;
		if (isField(document.javelin_form.elements[myid])) {
			document.javelin_form.elements[myid].focus();
			break;
		}
	}
}

function handleKeypress(e) {
   if(!e) e = window.event;
   e.returnValue = false;
   currentObject = e.target;
   var shiftKeyDown = e.shiftKey;
	var match = false;
   //alert("keycode='" + e.keyCode + "'");
   //return;
	for(var i=0;i<keymap_func.length && !match;i++){
		if(e.keyCode == keymap_func[i][0]){
			if(keymap_func[i][1]){
				if(!shiftKeyDown) doAction(keymap_func[i][2]);
				else doAction(keymap_func[i][3]);
			} else {
				if(!shiftKeyDown) keymap_func[i][2]();
				else keymap_func[i][3]();
			}
			match = true;
		}
	}

	if(!match && !enableBuffering){
		e.returnValue = true;
		match = true;
		return match;
	}
	if(!enableBuffering){
  	if (isIE) {
  		// for IE simply return false in the handler to notify that we handled the event
  		if (e.keyCode != 17) // hack for control key ==> have to ignore the folowing line
  			e.keyCode= 0;
  		return false;	
  	} else {
  		// for Gecko, cancel propagation and default handlers
  		e.stopPropagation();
  		e.preventDefault();
  		return false;	
  	}
	}
	return match;
}

function consome(){
	if(isWaiting)return;
	var elt;
	var ok=true;
	while(ok && (elt=fifo.pop())){
		if(isIE){
			if(elt.type == "keydown"){
				ok = (!handleKeypress(elt));
			}else{
				if(isPrintableChar(elt.keyCode) && focusOnField){
					focusOnField.value += String.fromCharCode(elt.keyCode);
					no_auto_checkInputChars(elt,focusOnField.size,false,focusOnField);
				}else if(elt.keyCode == 8 && focusOnField){
					focusOnField.value = focusOnField.value.substring(0,focusOnField.value.length);
				}
			}
		}else{
			try{
				var ke = elt.makeEvent(focusOnField);
				if(ok = (!handleKeypress(ke))){
					focusOnField.dispatchEvent(ke);
					no_auto_checkInputChars(ke,focusOnField.size,false,focusOnField);
				}
			}catch(e){
				alert(e);
			}
		}
	}
}

function onPressed(e){
	if(!e) e= window.event;
	fifo.push(new twsEvent(e));
	consome();
	
	if(isIE){
		ret = true;
		for(var i=0;i<keymap_func.length && ret;i++) ret = (e.keyCode != keymap_func[i][0]);
		e.keyCode = 0;
		return ret;
	}
}


// build an associative array containing the javelin_form.elements[] indices
// used for keyup and keydown navigation through javelin_form elements
function initElements() {
   if (document.getElementById("javelin_form")) {
      if (document.javelin_form.elements.length>0) {
         for (i=0;i<document.javelin_form.elements.length;i++) {
            javelin_formElements[document.javelin_form.elements[i].id] = i;
         }
      }
   }
}

function doAction(actionName) {
	document.javelin_form.__javelin_action.value 		= actionName;
	document.javelin_form.__javelin_current_field.value = document.getElementById(currentFieldOnFocus).name;
   ajaxXmlPost(xmlhttp, document.javelin_form);
}

function doMenu(actionName, data){
	document.javelin_form.__javelin_action.value = actionName;
	if(eval(focusOnField)) 
		focusOnField.value = data;

   ajaxXmlPost(xmlhttp, document.javelin_form);
}	 

function doAction(actionName, currentField){
	document.javelin_form.__javelin_action.value = actionName;
	if(!eval(currentField))
		if (document.getElementById(currentFieldOnFocus) != null) {
			document.javelin_form.__javelin_current_field.value = document.getElementById(currentFieldOnFocus).name;
		}
		else {
			document.javelin_form.__javelin_current_field.value = currentFieldOnFocus;
		}
	else
		document.javelin_form.__javelin_current_field.value = currentField;
		
	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function doTransaction(trName, trData) {
	var dataString = "__transaction=" + trName + "&__context=" + getId("__context").value;
	if (trData != "") {
		dataString += "&" + trData;
	}
	ajaxXmlPostData(xmlhttp, dataString);
}

function sendMenu(text) {
	document.javelin_form.editField.value = text;
	doAction('KEnvoi');
}

function refresh() {
	document.javelin_form.__javelin_action.value = "convertigo_refresh";
	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function reconnect() {
	document.javelin_form.__javelin_action.value = "convertigo_reconnect";
	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function no_auto_checkInputChars(event, size, bAutoEnter, Object) {
	if (isPrintableChar(event.keyCode)) {
		if(bAutoEnter){
			if (Object.value.length >= size)
			{
				ajaxXmlPost(xmlhttp, document.javelin_form);
			}
		}
		else {
			if (Object.value.length >= size)
			{
				if (doAutoTab(event.keyCode)) {
					var elt= document.getElementsByTagName("INPUT");
					next=getNextInput((currentObject == null) ? Object:currentObject, elt);
					next.focus();
					next.select();
					focusOnField=next;
					currentObject = null;
				}
			}	
		}
	}
}

function doAutoTab(keyCode) {
	var protectedKeys = "9,37,38,39,40,";
	if (protectedKeys.indexOf(keyCode) != -1) {
		return false;
	}
	else {
		return true;
	}
}

function checkInputChars(event, size, bAutoEnter, Object) {
	if(!enableBuffering)no_auto_checkInputChars(event, size, bAutoEnter, Object);
}

function isPrintableChar(c) {
	return (c >= 32);
}

function getNextInput(Object, elt) {
	for ( i=0; i < elt.length; i++) {
		if (elt[i].id == Object.id ) {
			if (i==elt.length)
				break;
			else {
				for (j=i+1; j < elt.length; j++) {
					if (isField(elt[j]) && (elt[j].type != 'hidden')) {
						return elt[j];
					}
				}
				continue;
			}
		}
	}
	for ( i=0; i < elt.length; i++) {
		if (isField(elt[i]) && (elt[i].type != 'hidden')) {
			return elt[i];
		}
	}
	return Object;
}

function isField(Object) {
	if (Object == null)
		return false;
		
	if (Object.id.indexOf("__field")!=-1) {
		return true;
	}
	else return false;
}

function moveCursor(myField) {
	curLine = currentFieldOnFocus.substring(currentFieldOnFocus.indexOf('_l')+2,currentFieldOnFocus.length);
	targetLine = myField.substring(myField.indexOf('_l')+2,myField.length);
	targetField=(curLine != targetLine)?myField:currentFieldOnFocus;
	todo = "document.javelin_form." + targetField + ".focus()";
	eval(todo);
	currentFieldOnFocus=targetField;
}
				
function spanClick(object, col, lin) {
	if (oldSpan != null) {
		oldSpan.className = "unfocusedStatic";
	}
	oldSpan = object;
	object.className = "focusedStatic";
	currentFieldOnFocus 		= '__field_c' + col + '_l' + lin;
}

function onInputClick(object) {
   //alert("obj='" + object.id +"'");
	focusOnField = object;
	if (oldSpan != null) {
		oldSpan.className = "unfocused";
	}
	oldSpan = document.getElementById(object.id + "parent");
	if(oldSpan){
		oldSpan.className = "fieldfocused";
	}
}

function over_tr(object){
	if(isIE){
	if(oldTrBgcolor){
			object.className = oldTrBgcolor;
			oldTrBgcolor = null;
		}else{
			oldTrBgcolor = object.className;
			object.className = oldTrBgcolor+"over";
		}
	}
}

// ********************************************************************************************
// finds in the XML document the field having the focus, then sets the focus on this field
// ********************************************************************************************
function setAjaxFocus(){
	var node;
	var focusField = null;
	var hide = document.getElementById("focus");
	/*
	if (hide != null) {
		alert("hide value='" + hide.value + "'");
	}
	else {
		alert("hide ='" + hide + "'");
	}
	*/
	if (hide != null) {
		focusField = document.getElementById(hide.value + "_n1");
		//alert("#=" + focusField.id);
		if (focusField != null) {
			if (isFocusable(focusField)) {
				focusField.focus();
			}
			if (isSelectable(focusField)) {
	      		focusField.select();
	      	}
			currentFieldOnFocus= focusField.id;
			focusOnField = focusField;
		}
	}
	else {
		//alert("no hide");
		var col = (document.getElementById("cursorColumn") != null)?document.getElementById("cursorColumn").value:0;
		var lin = (document.getElementById("cursorLine") != null)?document.getElementById("cursorLine").value:0;
		//if (!focusOnFirstField()) {
			//alert("could not focus on first field");
			document.getElementById("__javelin_current_field").value = "__field_c" + col + "_l" + lin;
			currentFieldOnFocus = "__field_c" + col + "_l" + lin;
		//}
	}
}

// ********************************************************************************************
// if date fields are present, add corresponding scripts
// ********************************************************************************************

function addCalendars() {
   var patternComponents = new Array()
   patternComponents["yyyy"] = "%Y";
   patternComponents["yy"] = "%y";
   patternComponents["MM"] = "%m";
   patternComponents["dd"] = "%d";
   var separators = "/-";
   var pattern = "";
   if (document.getElementById("calendar_fields")) {
      var cals = document.getElementById("calendar_fields").value.split(";");
      cals.pop();
      for (i=0;i<cals.length;i++) {
         thisCal = cals[i].split("|");
         for (j=0; j<separators.length; j++) {
            current = separators.charAt(j);
            if (thisCal[1].indexOf(current) != -1) {
               separator = current;
               break;
            }
         }
         var myPatternElements = thisCal[1].split(separator);
         for (j=0; j<myPatternElements.length;j++) {
            if (patternComponents[myPatternElements[j]]) {
               myPatternElements[j] = patternComponents[myPatternElements[j]];
            }
         }
         pattern = myPatternElements.join(separator);
         Calendar.setup({
            inputField  : thisCal[0] + "_n1",   // id of the input field
	       	ifFormat    : pattern,   // format of the input field
	       	daFormat  : pattern
	    	});
      }
   }
}

// ********************************************************************************************
// In error page, open error details (stacktrace, etc...)
// ********************************************************************************************
function showErrorDetails() {
   var detailsDiv = document.getElementById("details");
   if (eval(detailsDiv)) {
      if (detailsDiv.style.display == "none") {
			detailsDiv.style.display = "block";
		}
		else {
			detailsDiv.style.display = "none";
		}
	}
}

/************************************************\
| Begin : Dom dirty (unsollicited changes) check |
\************************************************/
function ddTimer(iter) {
	var timerValues = new Array(250, 330, 420, 500, 1000, 2000, 2000, 3000, 5000, 10000, 15000);
	var res = (iter<timerValues.length)?timerValues[iter]:25000;
	
	return res;
}

function checkDirty(pr, cn, tim) {
	clearTimeout(checkDomDirty);
	//alert("tim='" + tim + "'");
	if (pr && cn && tim < 15) {
		if (tim > 0) {
			var poller = getXmlHttpRequest();
		  	if (isIE) {
		  		poller.onreadystatechange = function () {}
		  		poller.abort();
		   	}
		  	poller.onreadystatechange = function(){
		  		if (poller.readyState == 4) {
		  			if (poller.responseText=="true") {
		  				//alert("true");
		  				refresh();
		  			}
		  			else {
		  				//alert("false");
		  			}
		  		}
		  	};
		  	poller.open("GET", "../../webclipper/" + pr + "/" + cn + "/d", true);
		  	poller.send();
		}
		var newTim = tim + 1;
		checkDomDirty = window.setTimeout("checkDirty('" + pr + "', '" + cn + "', " + newTim + ")", ddTimer(tim));
	}
}

// ********************************************************************************************
// Implements the XML  Ajax ready state listener
// ********************************************************************************************
function XSLT_transformation(){
    var resultDiv = document.getElementById("resultDiv");
    if(window.XSLTProcessor){
          var xsltProcessor = new XSLTProcessor();
          xsltProcessor.importStylesheet(xslDom);
          fragment = xsltProcessor.transformToFragment(xmlDocument, document);
         
          var mydiv = document.createElement("div");
          var myattb = document.createAttribute("id");
          myattb.value = "resultDiv";
          mydiv.setAttributeNode( myattb );
         
          mydiv.appendChild(fragment);                   
          resultDiv.parentNode.replaceChild(mydiv,resultDiv);
    }else{
          oldTrBgcolor = null;
          if (typeof (xmlDocument.transformNode) != "undefined")
        {
                strResult = xmlDocument.transformNode(xslDom);
        } else {
          try {
              var xslt = new ActiveXObject("Msxml2.XSLTemplate");
              var xslDoc = new ActiveXObject("Msxml2.FreeThreadedDOMDocument");
              xslDoc.load(xslDom);
              xslt.stylesheet = xslDoc;
              var xslProc = xslt.createProcessor();
              xslProc.input = xmlDocument;
              xslProc.transform();
              strResult =  xslProc.output;
            }
            catch (e) {
                alert("The type [XSLTProcessor] and the function [XmlDocument.transformNode] are not supported by this browser, can't transform XML document to HTML string!");
                return null;
            }
        }
          resultDiv.innerHTML = strResult;
    }
}

function ajaxReadyStateListener()
{
	if(xmlhttp.readyState == 4) {
		if(benchTime)window.status="";
		
		// when the response has arrived ....
		//alert(xmlhttp.responseText);
		var xmlDom = xmlhttp.responseXML;
		if (MSXMLVersion == 6) {
			xmlDom.resolveExternals = true;
		}
		
		var t_tot = null;
		var chrono = null;
		if(benchTime){
			t_tot = new gTime("TotalPros",true);
			chrono = new gTime("getXSL",true);
		}
		
		if(benchTime)requestTime.stop(true);
		
		// extract the stylesheet URI
		if (XSLstep == 1) {
			xslUri = getStyleSheetURI(xmlDom);
			if (xslUri != null) {
				// save xml document for future use ..
				xmlDocument = xmlDom;
				
				// get the xsl stylesheet  synchronously
				XSLstep = 2;
				xmlhttp.open("GET", xslUri, false);
				xmlhttp.send(null);
				
				xslDom = xmlhttp.responseXML;
				
				if(benchTime)chrono.stop(true);
							
				// apply the xsl transformation
				if(benchTime)chrono.start("xslTrans");
				XSLT_transformation();
				if(benchTime)chrono.stop(true);
				
				
				// handle resizing ..
				if(benchTime)chrono.start("resize");
				getPositions();
				resize();
				if(benchTime){
					chrono.stop(true);
					chrono.start("EndInit");
				}
	
				// initialize for arrow and TAB navigation keys
				initElements();
				
				// sets the focus on the selected field
				setAjaxFocus();
				
	         	// adds calendar scripts
	         	addCalendars();
	         
				isWaiting = false;
				
				//replayBuffer();
				if(enableBuffering)consome();
				
				// turn off the wait sign ..
				//document.getElementById("waitDiv").style.visibility = 'hidden';
				document.getElementById("waitDiv").style.display = 'none';
				
				var rEl = xmlDom.documentElement;
				if (bCheckDomDirty) {
					checkDirty(rEl.getAttribute("project"), rEl.getAttribute("connector"), 0);
				}

				if(benchTime)chrono.stop(true);
				if(benchTime)t_tot.stop(true);
				if(benchTime)totalTime.stop(true);
			}
			else {
				alert("no XSL stylesheet found");
				// turn off the wait sign ..
				//document.getElementById("waitDiv").style.visibility = 'hidden';
				document.getElementById("waitDiv").style.display = 'none';
				
				if(benchTime)chrono.stop(true);
				if(benchTime)t_tot.stop(true);
				if(benchTime)totalTime.stop(true);
			}
		}
	}
}

// ********************************************************************************************
// Implements the XML  Ajax POST
// ********************************************************************************************
function ajaxXmlPost(xmlRequester) {
   if (xmlRequester) {
	XSLstep = 1;
   	xmlRequester.open("POST", ".xml", true);
   	xmlRequester.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=utf-8');
   	xmlRequester.send(null);
   }
}

function ajaxXmlPost(xmlRequester, form) {
   if (xmlRequester) {
   	isWaiting = true;
   	if(benchTime)totalTime.start();
   	
   	// abort any existing request
   	if (xmlRequester  != null) {
   		// Hack to IE to prevent a JSCRIPT.DLL Trap
   		xmlRequester.onreadystatechange = function () {}
   		xmlRequester.abort();
   	}
   	
   	// turn on the wait DIV
   	//document.getElementById("waitDiv").style.visibility = 'visible';
   	document.getElementById("waitDiv").style.display = 'block';
   	clearTimeout(checkDomDirty);
   	if(benchTime)requestTime.start();
   	
   	// set the ajax receive handler
   	xmlRequester.onreadystatechange = ajaxReadyStateListener;
   	
   	// build and send the request
   	XSLstep = 1;
   	xmlRequester.open("POST", ".xml", true);
   	xmlRequester.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=utf-8');
   	if (form != null) {
   		requestString = serializeForm(form);
   		xmlRequester.send(requestString);
   	} else  {
   		xmlRequester.send(null);
   	}
   }
}

function ajaxXmlPostData(xmlRequester, data) {
   if (xmlRequester) {
   	if(benchTime)
   		totalTime.start();
   	
   	// abort any existing request
   	if (xmlRequester  != null) {
   		// Hack to IE to prevent a JSCRIPT.DLL Trap
   		xmlRequester.onreadystatechange = function () {}
   		xmlRequester.abort();
   	}
   	
   	// turn on the wait DIV
   	isWaiting = true;
   	//document.getElementById("waitDiv").style.visibility = 'visible';
   	document.getElementById("waitDiv").style.display = 'block';
   	if(benchTime)requestTime.start();
   	
   	// set the ajax receive handler
   	xmlRequester.onreadystatechange = ajaxReadyStateListener;
   	
   	// build and send the request
	XSLstep = 1;
   	xmlRequester.open("POST", ".xml", true);
   	xmlRequester.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=utf-8');
   	xmlRequester.send(data);
   }
}

//************************************************************************************************/
// Gets the style sheet URI from an XML processing intruction
// returns null if no stylesheet found
//************************************************************************************************/
function getStyleSheetURI(xmlDOM)
{
	/**FIREFOX: childNodes[0];**/
	var pi;
	var i=0;
	do{
		pi = xmlDOM.childNodes[i];
		i++;
	}while(pi != null && pi.nodeName != "xml-stylesheet");
	
	try {
		start = pi.nodeValue.indexOf('href="') + 6;
		styleSheetURI = pi.data.substring(start, pi.nodeValue.length -1);
	} catch (e) {
		return null;
	}
	return(styleSheetURI);
}

//************************************************************************************************/
// Gets the Ajax Http request object
//************************************************************************************************/
function getXmlHttpRequest() {
	/*@cc_on @*/
	/*@if (@_jscript_version >= 5)
	// JScript gives us Conditional compilation, we can cope with old IE versions.
	// and security blocked creation of the objects.
        
	try {
		xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.6.0");
		MSXMLVersion = 6;
		//alert("6!");
	}
	catch (e1) {
		try {
			xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.5.0");
			MSXMLVersion = 5;
			//alert("5!");
		}
		catch (e2) {
			try {
            	xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.4.0");
            	MSXMLVersion = 4;
            	//alert("4!");
			}
			catch (e3) {
				try {
	            	var bInstallMSXML = confirm("WARNING : Your browser does not support MSXML object.\nWould you like to install MSXML right now ?");
					if (bInstallMSXML==true) {
						document.location.href = "installxml.jsp";
					}
					else {
						document.location.href = "http://www.twinsoft.fr";
					}
					xmlhttp = false;
				}
				catch (e4) {
				
					xmlhttp = false;
				}
			}
		}
	}
	 
	@end @*/

	if (!xmlhttp && window.XMLHttpRequest){ // Firefox et autres
	   xmlhttp = new XMLHttpRequest();
	}
	
	if (!xmlhttp && typeof XMLHttpRequest!='undefined') {
		try {
			xmlhttp = new XMLHttpRequest();
		} catch (e) {
			xmlhttp = false;
		}
	}
	
	if (!xmlhttp && window.createRequest) {
		try {
			xmlhttp = window.createRequest();
		} catch (e) {
			xmlhttp = false;
		}
	}

	return xmlhttp;
}

/**
 * Serialize a form into a format which can be sent as a GET string or a POST 
 * content.It correctly ignores disabled fields, maintains order of the fields 
 * as in the elements[] array. The 'file' input type is not supported, as 
 * its content is not available to javascript. This method is used internally
 * by the submit class method.
 */
function serializeForm(theform) {
	var els = theform.elements;
	var len = els.length;
	var queryString = "";
	this.addField = 
		function(name,value) {
			if (name != "") { 
				if (queryString.length>0) {
					queryString += "&";
				}
				queryString += encodeURIComponent(name) + "=" + encodeURIComponent(value);
			}
		};
		
	for (var i=0; i<len; i++) {
		var el = els[i];
		var prvFld = null;
		var nxtFld = null;
		var bigValue = "";
		var spacesString = "                                                                                                                        ";
		if (!el.disabled) {
			switch(el.type) {
				case 'text': case 'password': case 'hidden': case 'textarea':
					// Test if this data is not part of a continuous field
					prvFld = el.getAttribute("previousField");
					if (prvFld == null) {
						nxtFld = el.getAttribute("nextField");
						if (nxtFld != null) {
							//alert("continuous : " + el.id);
							bigValue = (el.value + spacesString).substr(0, el.getAttribute("maxlength"));
							while (nxtFld != null) {
								bigValue += (getId(nxtFld).value + spacesString).substr(0, getId(nxtFld).getAttribute("maxlength"));
								nxtFld = getId(nxtFld).getAttribute("nextField");
							}
							this.addField(el.name, bigValue.rtrim());
						}
						else {
							this.addField(el.name, el.value);
						}
					}
					break;
				case 'select-one':
					if (el.selectedIndex>=0) {
						this.addField(el.name, el.options[el.selectedIndex].value);
					}
					break;
				case 'select-multiple':
					for (var j=0; j<el.options.length; j++) {
						if (el.options[j].selected) {
							this.addField(el.name, el.options[j].value);
						}
					}
					break;
				case 'checkbox': case 'radio':
					if (el.checked) {
						this.addField(el.name,el.value);
					}
					break;
			}
		}
	}
	return queryString;
};