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

var activeButtonObj= null;

	function createButton(name){
		var code= "";
		code+='<table width="100%" border="0" cellpadding="0" cellspacing="0" class="button">';
		code+='	<tr>';
		code+='		<TD bgcolor="#475298" vAlign="top" width="9" height="24"><IMG height="24" width="9" alt="" ';
		code+='			id= "' + name + '_left_img" ';
		code+='			src="../../images/button_left.gif" border="0">';
		code+='		</TD>';
		
		
		code+='		<td bgcolor="#475298" height="24" valign="middle" align="center" class="buttonDisactive" id="' + name + '_txt">';
		code+=name;
		code+='		</td>';
		code+='		<TD bgcolor="#475298"vAlign="top" width="10" height="24"><IMG height="24" width="10" alt="" ';
		code+='			id= "' + name + '_right_img" ';
		code+='			src="../../images/button_right.gif" border="0">';
		code+='		</TD>';
		code+='</tr></table>';
		return code;
	}
	
	function Button(){
		this.name= "";
		this.action= "";
	}
	
	function createBar(item, nbElementPerLine){		
		var myBar= "";
  		myBar+='<TABLE width="100%" border="0" cellPadding="0" cellSpacing="1" bgcolor="#475298"><TBODY>';
        
		var nbElementCreated= 0;
		while(nbElementCreated != item.length){
			myBar+='<TR>';
			nbElementCreatedInThisLine= 0;
			while(((nbElementCreatedInThisLine - nbElementPerLine) != 0) && nbElementCreated != item.length){
				myBar+='<TD onMouseUp="return disactiveButton(this, \'' + item[nbElementCreated].name + '\');"';
				myBar+=' onMouseDown="return activeButton(this, \'' + item[nbElementCreated].name + '\');" ';
				myBar+=' onMouseOut="return disactiveButton(this, \'' + item[nbElementCreated].name + '\');" ';
				myBar+=' onclick="return ' + item[nbElementCreated].action + ';">';
				myBar+=createButton(item[nbElementCreated].name);
				myBar+='</TD>';
				nbElementCreated++;
				nbElementCreatedInThisLine++;
			}
      		myBar+='</TR>';
		}
		
		myBar+='</TBODY></TABLE>';
		return myBar;
	}

	function activeButton(obj, name){
		if(activeButtonObj == obj) return;
		var elt= obj.getElementsByTagName("TD");
		for(i=0 ; i < elt.length ; i++){
			if(elt[i].id == name + '_txt') elt[i].className="buttonActive";
			else if(elt[i].childNodes[0].id == name + '_left_img') elt[i].childNodes[0].src="../../images/button_left_actif.gif";
			else if(elt[i].childNodes[0].id == name + '_right_img') elt[i].childNodes[0].src="../../images/button_right_actif.gif";
		}
		if(activeButtonObj != null) disactiveButton(activeButtonObj, activeButtonObj.id)
		activeButtonObj= obj;
	}
	
	function disactiveButton(obj, name){
		if(activeButtonObj != obj) return;
		var elt= obj.getElementsByTagName("TD");
		for(i=0 ; i < elt.length ; i++){
			if(elt[i].id == name + '_txt') elt[i].className="buttonDisactive";
			else if(elt[i].childNodes[0].id == name + '_left_img') elt[i].childNodes[0].src="../../images/button_left.gif";
			else if(elt[i].childNodes[0].id == name + '_right_img') elt[i].childNodes[0].src="../../images/button_right.gif";
		}
		activeButtonObj= null;	
	}
	
	function changeCaption(obj, caption){
		var elt= obj.getElementsByTagName("TD");
		for(i=0 ; i < elt.length ; i++){
			if(elt[i].id == obj.id + '_txt'){
				elt[i].innerHTML= caption;
				break;
			}
		}
	}
	function getCaption(obj){
		var elt= obj.getElementsByTagName("TD");
		for(i=0 ; i < elt.length ; i++){
			if(elt[i].id == obj.id + '_txt')
				return elt[i].innerHTML;
		}
	}