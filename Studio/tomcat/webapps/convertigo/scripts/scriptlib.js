/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

var focusOnField = null;
var currentFieldOnFocus = '';
var oldSpan = null;


function doAction(actionName){
	parent.inScript= true;
	
	var CGI_BIN_GET= unescape(document.location.search);
	if(eval(document.javelin_form.__sesskey) && eval(document.javelin_form.__context)){
		if(CGI_BIN_GET != null && CGI_BIN_GET.indexOf("__sesskey", 1) != -1 && CGI_BIN_GET.indexOf("__context", 1) != -1){
			var __sesskeyValue= CGI_BIN_GET.slice(CGI_BIN_GET.indexOf("__sesskey", 1), CGI_BIN_GET.length).split("&")[0].split("=")[1];
			var __contextValue= CGI_BIN_GET.slice(CGI_BIN_GET.indexOf("__context", 1), CGI_BIN_GET.length).split("&")[0].split("=")[1];
			document.javelin_form.__sesskey= __sesskeyValue;
			document.javelin_form.__context= __contextValue;	
		}
		else{
			document.javelin_form.__sesskey.value= "";
			//document.javelin_form.__context.value= "";	
		}
	}

	document.javelin_form.__javelin_action.value = actionName;
	document.javelin_form.__javelin_current_field.value = currentFieldOnFocus;
	document.javelin_form.submit();
}

function doMenu(actionName, data){
	document.javelin_form.__javelin_action.value = actionName;
	if(eval(focusOnField)) 
		focusOnField.value = data;
	document.javelin_form.submit();
}	 

function doAction(actionName, currentField){
	parent.inScript= true;
	
	var CGI_BIN_GET= unescape(document.location.search);
	if(eval(document.javelin_form.__sesskey) && eval(document.javelin_form.__context)){
		if(CGI_BIN_GET != null && CGI_BIN_GET.indexOf("__sesskey", 1) != -1 && CGI_BIN_GET.indexOf("__context", 1) != -1){
			var __sesskeyValue= CGI_BIN_GET.slice(CGI_BIN_GET.indexOf("__sesskey", 1), CGI_BIN_GET.length).split("&")[0].split("=")[1];
			var __contextValue= CGI_BIN_GET.slice(CGI_BIN_GET.indexOf("__context", 1), CGI_BIN_GET.length).split("&")[0].split("=")[1];
			document.javelin_form.__sesskey.value= __sesskeyValue;
			document.javelin_form.__context.value= __contextValue;	
		}
		else{
			document.javelin_form.__sesskey.value= "";
			//document.javelin_form.__context.value= "";	
		}
	}
	
	document.javelin_form.__javelin_action.value = actionName;
	if(!eval(currentField))
		document.javelin_form.__javelin_current_field.value = currentFieldOnFocus;
	else
		document.javelin_form.__javelin_current_field.value = currentField;
	document.javelin_form.submit();
}

function sendMenu(text){
	document.javelin_form.editField.value=text;
	doAction('KEnvoi');
}	 
        
function refresh(){
	document.javelin_form.__javelin_action.value = "convertigo_refresh";
	document.javelin_form.submit();
}	 

function reconnect(){
	document.javelin_form.__javelin_action.value = "convertigo_reconnect";
	document.javelin_form.submit();
}

function checkInputChars(event, size, bAutoEnter, Object) {
	if (isPrintableChar(event.keyCode)) {
		if(bAutoEnter){
			if (Object.value.length == size)
			{
				document.javelin_form.submit();
			}
		}
		else {
			if (Object.value.length == size)
			{
				if (event.keyCode != 38 && event.keyCode != 40) {
					var elt= document.getElementsByTagName("INPUT");
					next=getNextInput(Object, elt);
					next.focus();
					next.select();
				}
			}	
		}
	}
}

function isPrintableChar(c) {
	if (c == 9 || c == 16 ) {
		return (false);
	}
	else {
		return (true);
	}
}

function getNextInput(Object, elt) {
	for ( i=0; i < elt.length; i++){
		if (elt[i].name == Object.name ){
			if (i==elt.length)
				break;
			else {
				for (j=i+1; j < elt.length; j++) {
					if (isField(elt[j]) && (elt[j].type != 'hidden')){
						return elt[j];
					}
				}
				continue;
			}
		}
	}
	for ( i=0; i < elt.length; i++){
		if (isField(elt[i]) && (elt[i].type != 'hidden')){
			return elt[i];
		}
	}
	return Object;
}

function isField(Object) {
	if (Object == null)
		return false;
		
	if (Object.name.indexOf("__field")!=-1) { 
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
		oldSpan.style.borderStyle= 'none';
	}
	oldSpan = object;
	object.style.borderStyle	= 'dashed';
	object.style.borderWidth 	= '1px';
	currentFieldOnFocus 		= '__field_c' + col + '_l' + lin;
}

function setFocusOnField(object) {
	if (oldSpan != null) {
		oldSpan.style.borderStyle= 'none';
	}
	currentFieldOnFocus=object.id					
}
