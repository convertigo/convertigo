/*
 * Copyright (c) 1999-2016 Convertigo SA. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission of Convertigo sarl or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes no  representations  or  warranties  about   the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $Workfile: scriptlib.js $
 * $Author: Davidm $
 * $Modifications jmc $
 * $Revision: 9 $
 * $Date: 16/04/06 15:36 $
 */

/*******************************************************************************
 * ******************************************************************\ **
 * VARIABLES *** \
 ******************************************************************************/
// perform initilizations
var oldSpanClass = null;
var hasCalendar = false;
var hasTableAction = false;

var insertMode = false;
var selectAfterFocus = false;
var focusOnField = null;
var currentFieldOnFocus = '';
var oldSpan = null;
var oldTrBgcolor = null;
var currentObject = null; // cursor focus

// More initilizations
var xmlhttp = getXmlHttpRequest();
var xmlDocument = null;
var MSXMLVersion;
var javelin_formElements = [];
var formFieldsList = {
	"name" : []
};

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

var isIE = navigator.userAgent.indexOf('Gecko') == -1 ? true : false;

/**
 * called from the <head> of index.html
 */
function initVar() {
	if (benchTime) {
		requestTime = new gTime("requestTime");
		totalTime = new gTime("totalTime");
	}

	fifo = [];
	resetHandler();
}

/*******************************************************************************
 * FIN VARIABLES
 ******************************************************************************/
/*******************************************************************************
 * CLASS
 ******************************************************************************/

/** ************************** Class FIFO *************************** */
function FIFO() {
	fifo = [];
}

function FIFO_empty() {
	return (fifo.length === 0);
}

function FIFO_push(elt) {
	fifo.push(elt);
}

function FIFO_pop() {
	if (!FIFO_empty())
		return (fifo.pop());

	return null;
}

function FIFO_shift() {
	if (!FIFO_empty())
		return (fifo.shift());

	return null;
}

FIFO.prototype.push = FIFO_push;
FIFO.prototype.pop = FIFO_pop;
FIFO.prototype.shift = FIFO_shift;
FIFO.prototype.empty = FIFO_empty;

/** ************************* Fin Class fifo ************************ */

/** ************************* String addon methods ****************** */
String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g, "");
};

String.prototype.ltrim = function() {
	return this.replace(/^\s+/, "");
};

String.prototype.rtrim = function() {
	return this.replace(/\s+$/, "");
};

/** ************************* Class twsEvent ************************ */

function twsEvent(e) {
	if (!e)
		e = window.event;

	this.type = e.type;
	this.bubbles = e.bubbles;
	this.cancelable = e.cancelable;
	this.ctrlKey = e.ctrlKey;
	this.altKey = e.altKey;
	this.shiftKey = e.shiftKey;
	this.metakey = e.metaKey;

	// legacy
	this.keyCode = e.keyCode;
	this.charCode = e.which ? e.which : e.keyCode;
	this.which = e.which;
}

/** ********************* Fin Class twsEvent ************************ */
/** ************************* Class gTime *************************** */
function gTime(name, autoStart) {
	this.t_start = null;
	this.t_stop = null;
	this.name = name;

	if (autoStart) {
		var dat = new Date();
		this.t_start = dat.getTime();
	}
}

function gTime_start(name) {
	if (name)
		this.name = name;
	var dat = new Date();
	this.t_start = dat.getTime();
	return this.t_start;
}

function gTime_stop(autoStatus) {
	var dat = new Date();
	this.t_stop = dat.getTime();
	if (autoStatus)
		this.dif(true);
	return this.t_stop;
}

function gTime_dif(autoStatus) {
	var res = " [" + this.name + ":" + (this.t_stop - this.t_start) + "ms]";
	if (benchTime && autoStatus)
		window.status += res;
	return res;
}

gTime.prototype.start = gTime_start;
gTime.prototype.stop = gTime_stop;
gTime.prototype.dif = gTime_dif;
/** ********************* Fin Class gTime *************************** */

/*******************************************************************************
 * FIN CLASS
 ******************************************************************************/

function resetHandler() {
	/**
	 * hook keypress for regular keys
	 */
	window.addEventListener("keypress", onPressed, false);
	/**
	 * hook keydown for function keys
	 */
	window.addEventListener("keydown", onPressed, false);
}

/** ********************** Functions sort *************************** */

function setXSLparam(name, value) {
	var elements = xslDom.getElementsByTagName("param");

	for (var i = 0; i < elements.length; i++) {
		if (elements[i].getAttribute("name") == name) {
			if (value)
				elements[i].childNodes[0].nodeValue = value;

			return elements[i].childNodes[0].nodeValue;
		}
	}
	return null;
}

function getXSLparam(name) {
	return setXSLparam(name, null);
}

function onDataSortClic(type) {
	var imgURL;
	var img;

	if (lastSortColon) {
		img = document.getElementById("CoLiMg" + lastSortColon);
		if (img)
			imgURL = img.src;
	}

	setXSLparam("sortDataType", type);
	XSLT_transformation();
	getPositions();
	resize();

	if (lastSortColon) {
		img = document.getElementById("CoLiMg" + lastSortColon);
		if (img && imgURL)
			img.src = imgURL;
	}
}

function onSortClicTD(sortColon) {
	var imgURL;
	setXSLparam("doSort", "true");
	setXSLparam("sortColon", sortColon);
	if ((lastSortColon == sortColon)
			&& (getXSLparam("sortOrder") == "ascending")) {
		imgURL = "images/descending.gif";
		setXSLparam("sortOrder", "descending");
	} else {
		imgURL = "images/ascending.gif";
		setXSLparam("sortOrder", "ascending");
		lastSortColon = sortColon;
	}
	XSLT_transformation();
	getPositions();
	resize();
	var img = document.getElementById("CoLiMg" + sortColon);
	if (img)
		img.src = imgURL;
}

function onUnsortClic() {
	lastSortColon = null;
	setXSLparam("doSort", "false");
	setXSLparam("sortColon", "-1");
	XSLT_transformation();
	getPositions();
	resize();
}

/** ******************* Fin Functions sort *************************** */

function f_void() {
	return false;
}

function t_void() {
	return true;
}

function getId(el) {
	return document.getElementById(el);
}

function isFocusable(el) {
	// alert("el=" + el.id);
	// alert("el.disabled='" + el.disabled + "'");
	var ret = false;
	if ((isField(el)) && (el.type != 'hidden') && (el.disabled !== true)) {
		ret = true;
	}
	return ret;
}

function isSelectable(el) {
	// alert("el=" + el.id);
	var ret = false;
	if ((isField(el)) && ((el.type == 'text') || (el.type == 'password'))) {
		ret = true;
	}
	return ret;
}

function selectWholeField(field) {
	if (selectAfterFocus)
		setTimeout(function() {
			field.select();
		}, 50); // select all text
}

function focusPrevField() {
	if (document.javelin_form.elements.length > 0) {
		var prvFld = getId(currentFieldOnFocus).getAttribute("previousField");
		// Continuous field case
		if (prvFld !== null) {
			prvFld = getId(prvFld);
			if (isFocusable(prvFld)) {
				prvFld.focus();
				setCaretPosition(prvFld, 0);
				selectWholeField(prvFld);
			}
		}
		// Case this field is not part of a continuous field
		else {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid > 0) {
				myid--;
				if (isFocusable(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					setCaretPosition(document.javelin_form.elements[myid], 0);
					selectWholeField(document.javelin_form.elements[myid]);
					break;
				}
			}
			if (myid === 0) {
				focusOnLastField();
			}
		}
	}
}

function eraseToEnd() {
	var caret = getCaretPosition(focusOnField);
	var text = focusOnField.value;
	var output = text.substring(0, caret);

	focusOnField.value = output;
	focusOnField.selectionStart = caret;
	focusOnField.selectionEnd = focusOnField.selectionStart;

	focusNextField();
}

function focusNextField() {
	if (document.javelin_form.elements.length > 0) {
		var nxtFld = getId(currentFieldOnFocus).getAttribute("nextField");
		// Continuous field case
		if (nxtFld !== null) {
			nxtFld = getId(nxtFld);
			if (isFocusable(nxtFld)) {
				nxtFld.focus();
				setCaretPosition(nxtFld, 0);
				selectWholeField(nxtFld);
			}
		}
		// Case this field is not part of a continuous field
		else {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid < document.javelin_form.elements.length - 1) {
				myid++;
				if (isFocusable(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					setCaretPosition(document.javelin_form.elements[myid], 0);
					selectWholeField(document.javelin_form.elements[myid]);
					break;
				}
			}
			if (myid == document.javelin_form.elements.length - 1) {
				myid = -1;
				while (myid < document.javelin_form.elements.length - 1) {
					myid++;
					if (isFocusable(document.javelin_form.elements[myid])) {
						document.javelin_form.elements[myid].focus();
						setCaretPosition(document.javelin_form.elements[myid],
								0);
						selectWholeField(document.javelin_form.elements[myid]);
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
		// alert("form hasElements : '" + document.javelin_form.elements.length
		// + "'");
		for (var i = 0; i < document.javelin_form.elements.length - 1; i++) {
			// alert("analyzing '" + document.javelin_form.elements[i] + "'");
			if (isFocusable(document.javelin_form.elements[i])) {
				// alert("is a field : '" + document.javelin_form.elements[i].id
				// + "'");
				firstField = document.javelin_form.elements[i];
				break;
			}
		}
	}

	if (firstField) {
		firstField.focus();
		setCaretPosition(firstField, 0);
		selectWholeField(firstField);
		return true;
	}

	return false;
}

function focusOnLastField() {
	myid = document.javelin_form.elements.length;
	while (myid > 0) {
		myid--;
		if (isField(document.javelin_form.elements[myid])) {
			document.javelin_form.elements[myid].focus();
			setCaretPosition(document.javelin_form.elements[myid], 0);
			selectWholeField(document.javelin_form.elements[myid]);
			break;
		}
	}
}

function toggleInsertMode() {
	insertMode = insertMode ? false : true;
}

function isSelected(el) {
	var selectedText = "";

	// IE version
	if (document.selection !== undefined) {
		el.focus();
		var sel = document.selection.createRange();
		if (sel === null)
			return 0;

		selectedText = sel.text;
	} else // Mozilla version
	if (el.selectionStart !== undefined) {
		var startPos = el.selectionStart;
		var endPos = el.selectionEnd;
		selectedText = el.value.substring(startPos, endPos);
	}

	return selectedText.length;
}

function addToModifiedFieldsList(field) {
	for (var i = 0; i < formFieldsList.name.length; i++)
		if (formFieldsList.name[i] == field.name)
			return true;

	formFieldsList.name.push(field.name);
}

function setCaretPosition(elem, caretPos) {
	if (elem !== null) {
		if (elem.selectionStart) {
			elem.focus();
			elem.setSelectionRange(caretPos, caretPos);
		} else {
			elem.focus();
			elem.setSelectionRange(0, 0);
		}
	}
}

function handleChar(elt, substitute) {

	/**
	 * store programmatically the character into the input field
	 */
	var key = String.fromCharCode(elt.which || elt.charCode || elt.keyCode);

	if (substitute !== null)
		key = substitute;

	var caret = getCaretPosition(focusOnField);
	var text = focusOnField.value;
	var output = "";

	if ((focusOnField.selectionEnd > focusOnField.selectionStart)
			&& ((caret == focusOnField.selectionStart) || (caret == focusOnField.selectionEnd)))
		output = text.substring(0, focusOnField.selectionStart) + key
				+ text.substring(focusOnField.selectionEnd);
	else {
		if (insertMode) {
			if (text.length < focusOnField.maxLength)
				// output = text.substring(0, caret) + key +
				// text.substring(caret + (insertMode ? 0:1));
				output = text.substring(0, caret) + key + text.substring(caret);
			else {
				output = text;
				caret = Math.max(0, text.length - 1);
			}
		} else {
			if ((text.length <= focusOnField.maxLength)
					&& (caret < focusOnField.maxLength))
				output = text.substring(0, caret) + key
						+ text.substring(caret + 1);
			else {
				output = text;
				caret = Math.max(0, text.length - 1);
			}
		}
		// output = text.substring(0, caret) + key + text.substring(caret+1);
	}

	focusOnField.value = output;
	focusOnField.selectionStart = caret + 1;
	focusOnField.selectionEnd = focusOnField.selectionStart;

	addToModifiedFieldsList(focusOnField);

	/**
	 * bump cursor position and/or change field
	 */
	no_auto_checkInputChars(elt, focusOnField.maxLength, false, focusOnField);
}

/**
 * if no waitdiv, get all bufferized characters in turn and plays them
 */
function consome() {
	if (!fifo.length || isWaiting)
		return;

	var elt = null;

	while (((elt = fifo.shift()) !== null) && (elt !== undefined)) {
		try {
			/**
			 * parse keyboard events on e.type
			 */
			if (elt.type == "keydown") {
				handleKeydown(elt);
				setTimeout(consome, 0);
				break;
			} else
				handleChar(elt, null);

			if (isWaiting)
				break;
		} catch (e1) {
			// alert (e1);
		}
	}
}

/**
 * check event for fonction key (see keymapsna.js)
 * 
 * @param e
 * @returns {Boolean}
 */
function isKeydown(e) {
	if (e.type != "keydown")
		return false;

	switch (e.keyCode) {
	case 9: // Tabulation
	case 13: // Entr�e
	case 27: // Echap
	case 33: // Page pr�c�dente
	case 34: // Page suivante
	case 38: // Fl�che haut
	case 40: // Fl�che bas
	case 45: // Inser
	case 107: // + du pav� num�rique
	case 109: // - du pav� num�rique
	case 110: // . du pav� num�rique
		return true;
		break;
	case 106: // * du pav� num�rique

		// Recherche champ courant
		var fieldCurrent = document.getElementById(currentFieldOnFocus).name;

		// Calcul Id champ au dessus
		var n = fieldCurrent.indexOf("_l");
		var res = fieldCurrent.substring(0, n + 2);
		var currentLine = fieldCurrent.substring(n + 2);
		var aboveLine = currentLine - 1;
		var fieldAbove = res + aboveLine + "_n1";

		// Recopie valeur du champ superieur s'il existe dans le champ courant +
		// tabulation
		if (document.getElementById(fieldAbove) !== null) {
			document.getElementById(currentFieldOnFocus).value = document
					.getElementById(fieldAbove).value;
			addToModifiedFieldsList(document
					.getElementById(currentFieldOnFocus));
			focusNextField();
		}

		return true;
		break;

	// necessary to handle dirty field flag
	case 8: // Retour arri�re
	case 46: // Suppr
		addToModifiedFieldsList(document.getElementById(currentFieldOnFocus));
		return false;

	default:
		if ((e.keyCode > 111) && (e.keyCode < 124)) // F1 � F12
			return true;
		return false;
	}
}

function onPressed(e) {
	if (!e)
		e = window.event;

	if (e.isTrusted) {
		/**
		 * bufferize needed keyboard events prevent default because we will
		 * handle
		 */
		if ((e.type === "keypress") || isKeydown(e)) {
			fifo.push(new twsEvent(e));
			e.preventDefault();
		}
	}

	consome();
}

/**
 * deal with function keys
 * 
 * @param e
 * @returns {Boolean}
 */
function handleKeydown(e) {
	if (!e)
		e = window.event;

	e.returnValue = false;

	currentObject = e.target; // cursor focus

	var shiftKeyDown = e.shiftKey;
	var match = false;
	var k;

	var keymapStr = '';

	for (var i = 0; i < keymap_func.length && !match; i++) {
		if (e.keyCode == keymap_func[i][0]) {
			if (keymap_func[i][1]) {
				if (shiftKeyDown) {
					keymapStr = keymap_func[i][3];
					if (keymapStr.startsWith("@")) {
						for (k = 1; k < keymapStr.length; k++) {
							handleChar(e, keymapStr.charAt(k));
						}
					} else
						doAction(keymapStr);
				} else {
					keymapStr = keymap_func[i][2];
					if (keymapStr.startsWith("@")) {
						for (k = 1; k < keymapStr.length; k++) {
							handleChar(e, keymapStr.charAt(k));
						}
					} else
						doAction(keymapStr);
				}
			} else {
				if (shiftKeyDown)
					keymap_func[i][3]();
				else
					keymap_func[i][2]();
			}

			match = true;
		}
	}

	return match;
}

// build an associative array containing the javelin_form.elements[] indices
// used for keyup and keydown navigation through javelin_form elements
function initElements() {
	if (document.getElementById("javelin_form")) {
		if (document.javelin_form.elements.length > 0) {
			for (var i = 0; i < document.javelin_form.elements.length; i++) {
				javelin_formElements[document.javelin_form.elements[i].id] = i;
			}
		}
	}
}

function doAction(actionName) {
	document.javelin_form.__javelin_action.value = actionName;
	document.javelin_form.__javelin_current_field.value = document
			.getElementById(currentFieldOnFocus).name;
	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function doMenu(actionName, data) {
	document.javelin_form.__javelin_action.value = actionName;

	if (eval(focusOnField))
		focusOnField.value = data;

	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function doAction(actionName, currentField) {
	document.javelin_form.__javelin_action.value = actionName;

	if (!eval(currentField))
		if (document.getElementById(currentFieldOnFocus) !== null) {
			document.javelin_form.__javelin_current_field.value = document
					.getElementById(currentFieldOnFocus).name;
		} else {
			document.javelin_form.__javelin_current_field.value = currentFieldOnFocus;
		}
	else
		document.javelin_form.__javelin_current_field.value = currentField;

	if (actionName != 'KEY_PF12' && actionName != 'KEY_PF3') {
		var valid = true;
		// https://regex101.com/r/fH3lV1/13
		var numberRegex = /^[\s\u00a0]*[0-9-+:.,\/]*[\s\u00a0]*(CR|DB){0,1}$/;
		$(document.javelin_form).find("input[type2=number]").each(function() {
			var input = $(this);
			var value = input.val().trim();
			if (value.match(numberRegex) === null) {
				if (valid) {
					this.focus();
				}
				this.setCustomValidity('Invalid');
				valid = false;
			}
		});
		if (!valid) {
			return;
		}
	}

	ajaxXmlPost(xmlhttp, document.javelin_form);
}

function doTransaction(trName, trData) {
	var dataString = "__transaction=" + trName + "&__context="
			+ getId("__context").value;

	if (trData !== "") {
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

/*
 * * Returns the caret (cursor) position of the specified text field. * Return
 * value range is 0-oField.value.length.
 */
function getCaretPosition(oField) {
	// Initialize
	var iCaretPos = 0;

	// IE Support
	if (document.selection) {
		// To get cursor position, get empty selection range
		var oSel = document.selection.createRange();

		// Move selection start to 0 position
		oSel.moveStart('character', -oField.value.length);

		// The caret position is selection length
		iCaretPos = oSel.text.length;
	}
	// Firefox support
	else if (typeof oField.selectionStart === 'number')
		iCaretPos = oField.selectionStart;
	// Return results
	return iCaretPos;
}

function no_auto_checkInputChars(event, size, bAutoEnter, Object) {
	if (isPrintableChar(event.keyCode)) {
		var currentCursorPosition = getCaretPosition(Object);

		if (bAutoEnter) {
			if ((Object.value.length >= size)
					|| (currentCursorPosition >= size))
				ajaxXmlPost(xmlhttp, document.javelin_form);
		} else {
			if (currentCursorPosition >= size) {
				if (doAutoTab(event.keyCode)) {
					var elt = document.getElementById("javelin_form");

					try {
						next = getNextInput(
								((currentObject === null) || (currentObject === undefined)) ? Object
										: currentObject, elt); // cursor focus
						next.focus();
						focusOnField = next;

						// 
						// avoid reselect same input in case of single field
						//
						if (Object.id != focusOnField.id) {
							setCaretPosition(focusOnField, 0);
							selectWholeField(focusOnField);
						} else {
							// truncate field if needed
							if (currentCursorPosition > size)
								Object.value = Object.value.substr(0, size);
							setCaretPosition(focusOnField, size);
						}
					} catch (e1) {
						if (currentCursorPosition > size)
							Object.value = Object.value.substr(0, size);
					}
				}
			}
		}
	}
}

function doAutoTab(key) {
	var protectedKeys = "9,37,38,39,40,";
	if (protectedKeys.indexOf(key) != -1) {
		return false;
	} else {
		return true;
	}
}

function checkInputChars(event, size, bAutoEnter, Object) {
}

// function checkInput(event, numeric, size, bAutoEnter, Object) {
//
// // On ne teste que les num�riques
// // Les caract�re blanc s�cables des champs alpha sont transform�s en
// // caract�re blanc simple
// if (!numeric) {
// return;
// }
//
// var fieldValue = Object.value.trim();
//
// // Si la valeur n'est pas remplie, on ne teste pas
// if (fieldValue == "") {
// // console.log(Object.id + " : " + fieldValue + " : vide, pas de
// // verification");
// return;
// }
//
// // On applique le regex
// // var regexstring = '^([\\d\-\\.\\,\\\]{0,' + size + '})$';
// var regex = /^[0-9-.,:\/]*$/;
// // console.log(regexstring);
// var re = new RegExp(regex);
// if (re.test(fieldValue)) {
// // console.log(Object.id + " : " + fieldValue + " : numerique");
// document.getElementById(currentFieldOnFocus).setCustomValidity('');
// return;
// } else {
// console.log(Object.id + " : " + fieldValue + " : non numerique");
// document.getElementById(currentFieldOnFocus).setCustomValidity(
// 'Invalid');
// return;
// }
// }

function isPrintableChar(c) {
	return (c >= 32);
}

function isInputType(elt) {
	return ((elt.tagName === "INPUT") || (elt.tagName === "TEXTAREA"));
}

function getNextInput(Object, elt) {
	var i;

	/**
	 * first search in following fields
	 */
	for (i = 0; i < elt.length; i++) {
		if (isInputType(elt[i]) && (elt[i].id == Object.id)) {
			for (j = i + 1; j < elt.length; j++) {
				if (isField(elt[j]) && (elt[j].type != 'hidden')) {
					return elt[j];
				}
			}
		}
	}

	/**
	 * then from the beginning
	 */
	for (i = 0; i < elt.length; i++) {
		if (isInputType(elt[i]) && isField(elt[i]) && (elt[i].type != 'hidden'))
			return elt[i];
	}

	return Object;
}

function isField(Object) {
	if (Object === null)
		return false;

	if (Object.id.indexOf("__field") != -1) {
		return true;
	} else
		return false;
}

function moveCursor(myField) {
	curLine = currentFieldOnFocus.substring(
			currentFieldOnFocus.indexOf('_l') + 2, currentFieldOnFocus.length);
	targetLine = myField.substring(myField.indexOf('_l') + 2, myField.length);
	targetField = (curLine != targetLine) ? myField : currentFieldOnFocus;
	todo = "document.javelin_form." + targetField + ".focus()";
	eval(todo);
	currentFieldOnFocus = targetField;
}

function spanClick(object, col, lin) {
	if (oldSpan !== null) {
		oldSpan.className = "unfocusedStatic";
	}
	oldSpan = object;
	object.className = "focusedStatic";
	currentFieldOnFocus = '__field_c' + col + '_l' + lin;
}

function onInputClick(object) {
	// alert("obj='" + object.id +"'");
	focusOnField = object;
	if (oldSpan !== null) {
		oldSpan.className = "unfocused";
	}
	oldSpan = document.getElementById(object.id + "parent");
	if (oldSpan) {
		oldSpan.className = "fieldfocused";
	}
}

function over_tr(object) {
	// should be removed
}

// ********************************************************************************************
// finds in the XML document the field having the focus, then sets the focus on
// this field
// ********************************************************************************************
function setAjaxFocus() {
	var node;
	var focusField = null;
	var hide = document.getElementById("focus");
	/*
	 * if (hide !== null) { alert("hide value='" + hide.value + "'"); } else {
	 * alert("hide ='" + hide + "'"); }
	 */
	if (hide !== null) {
		focusField = document.getElementById(hide.value + "_n1");
		// alert("#=" + focusField.id);
		if (focusField !== null) {
			if (isFocusable(focusField)) {
				focusField.focus();
			}
			if (isSelectable(focusField)) {
				selectWholeField(focusField);
			}
			currentFieldOnFocus = focusField.id;
			focusOnField = focusField;
		}
	} else {
		// alert("no hide");
		var col = (document.getElementById("cursorColumn") !== null) ? document
				.getElementById("cursorColumn").value : 0;
		var lin = (document.getElementById("cursorLine") !== null) ? document
				.getElementById("cursorLine").value : 0;
		// if (!focusOnFirstField()) {
		// alert("could not focus on first field");
		document.getElementById("__javelin_current_field").value = "__field_c"
				+ col + "_l" + lin;
		currentFieldOnFocus = "__field_c" + col + "_l" + lin;
		// }
	}
}

// ********************************************************************************************
// if date fields are present, add corresponding scripts
// ********************************************************************************************

function addCalendars() {
	var patternComponents = [];
	patternComponents.yyyy = "%Y";
	patternComponents.yy = "%y";
	patternComponents.MM = "%m";
	patternComponents.dd = "%d";

	var separators = "/-";
	var pattern = "";
	if (document.getElementById("calendar_fields")) {
		var cals = document.getElementById("calendar_fields").value.split(";");
		cals.pop();
		for (var i = 0; i < cals.length; i++) {
			thisCal = cals[i].split("|");
			for (j = 0; j < separators.length; j++) {
				current = separators.charAt(j);
				if (thisCal[1].indexOf(current) != -1) {
					separator = current;
					break;
				}
			}
			var myPatternElements = thisCal[1].split(separator);
			for (var j = 0; j < myPatternElements.length; j++) {
				if (patternComponents[myPatternElements[j]]) {
					myPatternElements[j] = patternComponents[myPatternElements[j]];
				}
			}
			pattern = myPatternElements.join(separator);
			Calendar.setup({
				inputField : thisCal[0] + "_n1", // id of the input field
				ifFormat : pattern, // format of the input field
				daFormat : pattern
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
		} else {
			detailsDiv.style.display = "none";
		}
	}
}

/*******************************************************************************
 * **********************************************\ | Begin : Dom dirty
 * (unsollicited changes) check | \
 ******************************************************************************/
function ddTimer(iter) {
	var timerValues = new Array(250, 330, 420, 500, 1000, 2000, 2000, 3000,
			5000, 10000, 15000);
	var res = (iter < timerValues.length) ? timerValues[iter] : 25000;

	return res;
}

function checkDirty(pr, cn, tim) {
	clearTimeout(checkDomDirty);
	// alert("tim='" + tim + "'");
	if (pr && cn && tim < 15) {
		if (tim > 0) {
			var poller = getXmlHttpRequest();

			poller.onreadystatechange = function() {
				if (poller.readyState == 4) {
					if (poller.responseText == "true") {
						// alert("true");
						refresh();
					} else {
						// alert("false");
					}
				}
			};
			poller
					.open("GET", "../../webclipper/" + pr + "/" + cn + "/d",
							true);
			poller.send();
		}
		var newTim = tim + 1;
		checkDomDirty = window.setTimeout("checkDirty('" + pr + "', '" + cn
				+ "', " + newTim + ")", ddTimer(tim));
	}
}

// ********************************************************************************************
// Implements the XML Ajax ready state listener
// ********************************************************************************************
function XSLT_transformation() {
	var resultDiv = document.getElementById("resultDiv");
	if (window.XSLTProcessor) {
		var xsltProcessor = new XSLTProcessor();
		xsltProcessor.importStylesheet(xslDom);
		fragment = xsltProcessor.transformToFragment(xmlDocument, document);

		var mydiv = document.createElement("div");
		var myattb = document.createAttribute("id");
		myattb.value = "resultDiv";
		mydiv.setAttributeNode(myattb);

		mydiv.appendChild(fragment);
		resultDiv.parentNode.replaceChild(mydiv, resultDiv);
	} else {
		oldTrBgcolor = null;
		if (typeof (xmlDocument.transformNode) != "undefined") {
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
				strResult = xslProc.output;
			} catch (e) {
				alert("The type [XSLTProcessor] and the function [XmlDocument.transformNode] are not supported by this browser, can't transform XML document to HTML string!");
				return null;
			}
		}
		resultDiv.innerHTML = strResult;
	}

	// do specific code ..
	// fillReferenceNb();

	// fillTransaction();
}

function ajaxReadyStateListener() {
	if (xmlhttp.readyState == 4) {
		if (benchTime)
			window.status = "";

		// when the response has arrived ....
		// alert(xmlhttp.responseText);
		var xmlDom = xmlhttp.responseXML;
		if (MSXMLVersion == 6) {
			xmlDom.resolveExternals = true;
		}

		if (!C8O._hook("xml_response", xmlDom)) {
			return;
		}

		var t_tot = null;
		var chrono = null;
		if (benchTime) {
			t_tot = new gTime("TotalPros", true);
			chrono = new gTime("getXSL", true);
		}

		if (benchTime)
			requestTime.stop(true);

		// extract the stylesheet URI
		if (XSLstep == 1) {
			xslUri = getStyleSheetURI(xmlDom);
			if (xslUri !== null) {
				// save xml document for future use ..
				xmlDocument = xmlDom;

				// get the xsl stylesheet synchronously
				XSLstep = 2;
				xmlhttp.open("GET", xslUri, false);
				xmlhttp.send(null);

				xslDom = xmlhttp.responseXML;

				if (benchTime)
					chrono.stop(true);

				// apply the xsl transformation
				if (benchTime)
					chrono.start("xslTrans");
				XSLT_transformation();
				if (benchTime)
					chrono.stop(true);

				// handle resizing ..
				if (benchTime)
					chrono.start("resize");

				getPositions();
				resize();

				if (benchTime) {
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

				// turn off the wait sign ..
				// document.getElementById("waitDiv").style.visibility =
				// 'hidden';
				document.getElementById("waitDiv").style.display = 'none';

				var rEl = xmlDom.documentElement;
				if (bCheckDomDirty) {
					checkDirty(rEl.getAttribute("project"), rEl
							.getAttribute("connector"), 0);
				}

				// On force le mode de saisie en remplacement � chaque nouvelle
				// page
				insertMode = false;

				// R�solution automatique d'une URL dans une navigateur
				var urlelt = "";

				for (var i = 0; i < rEl.getElementsByTagName("URL").length; i++) {
					urlelt += (getText(rEl.getElementsByTagName("URL").item(i)))
							.trim();
				}

				if (urlelt.length !== 0) {

					// R�solution dans un nouvel onglet du butineur
					console.log("URL: " + urlelt);
					window.open(urlelt, "_blank");

					// Entr�e sur la fen�tre affich�e au niveau de l'�mulation
					// web
					setTimeout(function() {
						doAction("KEY_ENTER");
					}, 0);
				}

				if (benchTime)
					chrono.stop(true);
				if (benchTime)
					t_tot.stop(true);
				if (benchTime)
					totalTime.stop(true);

				/*
				 * waitdiv is hidden again, consome any bufferized character
				 */
				setTimeout(consome, 500);
			} else {
				alert("no XSL stylesheet found");
				// turn off the wait sign ..
				// document.getElementById("waitDiv").style.visibility =
				// 'hidden';
				document.getElementById("waitDiv").style.display = 'none';

				if (benchTime)
					chrono.stop(true);
				if (benchTime)
					t_tot.stop(true);
				if (benchTime)
					totalTime.stop(true);
			}
		}
	}
}

// ********************************************************************************************
// propri�t� textContent : indisponible sur IE9
// propri�t� text : indisponible apr�s IE9
// Cette fontion assure de renvoyer le texte quelle que soit la version de IE
// ********************************************************************************************
function getText(el) {
	return el.textContent || el.text;
}

// ********************************************************************************************
// Implements the XML Ajax POST
// ********************************************************************************************
function ajaxXmlPost(xmlRequester) {
	if (xmlRequester) {
		XSLstep = 1;
		xmlRequester.open("POST", ".xml", true);
		xmlRequester.setRequestHeader('Content-Type',
				'application/x-www-form-urlencoded; charset=utf-8');
		xmlRequester.send(null);
	}
}

function ajaxXmlPost(xmlRequester, form) {
	if (xmlRequester) {
		if (form) {
			form.__javelin_modified_fields.value = JSON
					.stringify(formFieldsList);
			// reset the dirty list
			formFieldsList = {
				"name" : []
			};
		}

		isWaiting = true;
		if (benchTime)
			totalTime.start();

		// abort any existing request
		if (xmlRequester !== null) {
			// Hack to IE to prevent a JSCRIPT.DLL Trap
			xmlRequester.onreadystatechange = function() {
			};
			xmlRequester.abort();
		}

		// turn on the wait DIV
		// document.getElementById("waitDiv").style.visibility = 'visible';
		document.getElementById("waitDiv").style.display = 'block';
		clearTimeout(checkDomDirty);
		if (benchTime)
			requestTime.start();

		// set the ajax receive handler
		xmlRequester.onreadystatechange = ajaxReadyStateListener;

		// build and send the request
		XSLstep = 1;
		xmlRequester.open("POST", ".xml", true);
		xmlRequester.setRequestHeader('Content-Type',
				'application/x-www-form-urlencoded; charset=utf-8');
		if (form !== null) {
			requestString = serializeForm(form);
			xmlRequester.send(requestString);

		} else {
			xmlRequester.send(null);
		}
	}
}

function ajaxXmlPostData(xmlRequester, data) {
	if (xmlRequester) {
		if (benchTime)
			totalTime.start();

		// abort any existing request
		if (xmlRequester !== null) {
			// Hack to IE to prevent a JSCRIPT.DLL Trap
			xmlRequester.onreadystatechange = function() {
			};
			xmlRequester.abort();
		}

		// turn on the wait DIV
		isWaiting = true;
		// document.getElementById("waitDiv").style.visibility = 'visible';
		document.getElementById("waitDiv").style.display = 'block';
		if (benchTime)
			requestTime.start();

		// set the ajax receive handler
		xmlRequester.onreadystatechange = ajaxReadyStateListener;

		// build and send the request
		XSLstep = 1;
		xmlRequester.open("POST", ".xml", true);
		xmlRequester.setRequestHeader('Content-Type',
				'application/x-www-form-urlencoded; charset=utf-8');

		// alert("data= " + data);

		xmlRequester.send(data);
	}
}

// ************************************************************************************************/
// Gets the style sheet URI from an XML processing intruction
// returns null if no stylesheet found
// ************************************************************************************************/
function getStyleSheetURI(xmlDOM) {
	/** FIREFOX: childNodes[0];* */
	var pi;
	var i = 0;
	do {
		pi = xmlDOM.childNodes[i];
		i++;
	} while (pi !== null && pi.nodeName != "xml-stylesheet");

	try {
		start = pi.nodeValue.indexOf('href="') + 6;
		styleSheetURI = pi.data.substring(start, pi.nodeValue.length - 1);
	} catch (e) {
		return null;
	}
	return (styleSheetURI);
}

// ************************************************************************************************/
// Gets the Ajax Http request object
// ************************************************************************************************/
function getXmlHttpRequest() {
	/* @cc_on @ */
	/*
	 * @if (@_jscript_version >= 5) // JScript gives us Conditional compilation,
	 * we can cope with old IE versions. // and security blocked creation of the
	 * objects.
	 * 
	 * try { xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.6.0"); MSXMLVersion =
	 * 6; //alert("6!"); } catch (e1) { try { xmlhttp = new
	 * ActiveXObject("Msxml2.XMLHTTP.5.0"); MSXMLVersion = 5; //alert("5!"); }
	 * catch (e2) { try { xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.4.0");
	 * MSXMLVersion = 4; //alert("4!"); } catch (e3) { try { var bInstallMSXML =
	 * confirm("WARNING : Your browser does not support MSXML object.\nWould you
	 * like to install MSXML right now ?"); if (bInstallMSXML==true) {
	 * document.location.href = "installxml.jsp"; } else {
	 * document.location.href = "http://www.twinsoft.fr"; } xmlhttp = false; }
	 * catch (e4) {
	 * 
	 * xmlhttp = false; } } } }
	 * 
	 * @end @
	 */

	if (!xmlhttp && window.XMLHttpRequest) { // Firefox et autres
		xmlhttp = new XMLHttpRequest();
	}

	if (!xmlhttp && typeof XMLHttpRequest != 'undefined') {
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
 * as in the elements[] array. The 'file' input type is not supported, as its
 * content is not available to javascript. This method is used internally by the
 * submit class method.
 */
function serializeForm(theform) {
	var els = theform.elements;
	var len = els.length;
	var queryString = "";
	this.addField = function(name, value) {
		if (name !== "") {
			if (queryString.length > 0) {
				queryString += "&";
			}
			
			// Pour les num�riques, on encode directement la valeur (espaces � gauche compris)
			if (el.getAttribute("type2") === "number") {
				queryString += encodeURIComponent(name) + "=" + encodeURIComponent(value);
			}
			
			// Pour les alphanum�riques, on encode la valeur, puis on converti les blancs non s�cables (EBCDIC x'41') par des blancs s�cables (EBCDIC x'40').
			// Cela permet de ne pas avoir de caract�re mal transmis aux logiciels tiers (ex. : WORD qui fait des mises en pages fantiasistes avec des blancs non s�cables)
			else {
				queryString += encodeURIComponent(name) + "=" + encodeURIComponent(value).replace(/(\u00A0|%C2%A0)/g, ' ');
			}
		}
	};

	for (var i = 0; i < len; i++) {
		var el = els[i];
		var prvFld = null;
		var nxtFld = null;
		var bigValue = "";
		var spacesString = "                                                                                                                        ";
		if (!el.disabled) {
			switch (el.type) {
			case 'text':
			case 'password':
			case 'hidden':
			case 'textarea':
				// Test if this data is not part of a continuous field
				prvFld = el.getAttribute("previousField");
				if (prvFld === null) {
					nxtFld = el.getAttribute("nextField");
					if (nxtFld !== null) {
						// alert("continuous : " + el.id);
						bigValue = (el.value + spacesString).substr(0, el
								.getAttribute("maxlength"));
						while (nxtFld !== null) {

							if (getId(nxtFld) !== null) {
								bigValue += (getId(nxtFld).value + spacesString)
										.substr(0, getId(nxtFld).getAttribute(
												"maxlength"));
								nxtFld = getId(nxtFld)
										.getAttribute("nextField");
							} else {
								nxtFld = null;
							}
						}
						this.addField(el.name, bigValue.rtrim());
					} else {
						this.addField(el.name, el.value);
					}
				}
				break;
			case 'select-one':
				if (el.selectedIndex >= 0) {
					this.addField(el.name, el.options[el.selectedIndex].value);
				}
				break;
			case 'select-multiple':
				for (var j = 0; j < el.options.length; j++) {
					if (el.options[j].selected) {
						this.addField(el.name, el.options[j].value);
					}
				}
				break;
			case 'checkbox':
			case 'radio':
				if (el.checked) {
					this.addField(el.name, el.value);
				}
				break;
			}
		}
	}
	return queryString;
}