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

if(!document.getElementById && document.all)
	document.getElementById = function(id) {
		return document.all[id];
	}
if(!document.getElementById && document.layer)
	document.getElementById = function(id) {
		return document.layer[id];
	}
hasCalendar= true;

// d : day
// M : month
// y : year
pattern= "dd-MM-yy";

defaultPattern= "dd/MM/yyyy";
englishLabels=
	["Today",
	"January",
	"February",
	"March",
	"April",
	"May",
	"June",
	"July",
	"August",
	"September",
	"October",
	"November",
	"December"]
frenchLabels=
	["Aujourd&acute;hui",
	"Janvier",
	"Février",
	"Mars",
	"Avril",
	"Mai",
	"Juin",
	"Juillet",
	"Août",
	"Septembre",
	"Octobre",
	"Novembre",
	"Décembre"]
currentLabels= frenchLabels;

var separator;
var nb_d;
var nb_m;
var nb_y;
    
/*************************************/
/* FONCTIONS DE CREATION DU CALENDAR */
/*************************************/
function initCalendar(){
	var item= new Array(42);
	for(i= 0 ; i < 42 ; i++){
		item[i]= new Button();
		item[i].name= 'but' + i;
		item[i].action= 'returnDate(this);';
	}
	return item;
}	

function createCalendar(){
	var nbElementPerLine= 7;
	var item= initCalendar();
	var code= "";
	
	var nbElementCreated= 0;
	while(nbElementCreated != item.length){
		code+='<TR>';
		nbElementCreatedInThisLine= 0;
		while(((nbElementCreatedInThisLine - nbElementPerLine) != 0) && nbElementCreated != item.length){
			code+='<TD onclick="return ' + item[nbElementCreated].action + ';" ';
			code+='id="' + item[nbElementCreated].name + '">';
			code+=createButton(item[nbElementCreated].name);
			code+='</TD>';
			nbElementCreated++;
			nbElementCreatedInThisLine++;
		}
		code+='</TR>';
	}
		
	return code;
}

function codeCalendar(){
	var code= '';
	code+='<div id="calendar_div" style="position: absolute ; top: 50px; left: 50px ; visibility : hidden">';
	code+='	<script src="../../scripts/calendar.js"></script>';
	code+='	<table cellpadding="0" cellspacing="0" border="0">';
	code+='		<tr>';
	code+='			<td colspan="2" rowspan="2"><IMG SRC="../../images/tl.gif" ALT="" WIDTH="21" HEIGHT="11"></td>';
	code+='			<td bgcolor="#FF3100"><img src="../../images/spacer.gif" width="1" height="1"></td>';
	code+='			<td colspan="3" rowspan="2"><IMG SRC="../../images/tr.gif" ALT="" WIDTH="21" HEIGHT="11"></td>';
	code+='		</tr>';
	code+='		<tr><td bgcolor="#475298"><img src="../../images/spacer.gif" width="1" height="10"></td></tr>';
	code+='		<tr>';
	code+='			<td width="1" bgcolor="#FF3300"><img src="../../images/spacer.gif" width="1" height="1"></td>';
	code+='			<td width="20" bgcolor="#475298"><img src="../../images/spacer.gif" width="20" height="1"></td>';
	code+='			<td> ';
	code+='				<table border="0" cellPadding="0" cellSpacing="0" bgcolor="#475298">';
	code+='					<tr>';
	code+='						<td align="center" colspan="7">';
	code+='							<form NAME="calControl" onSubmit="return false;">';
	code+='								<select NAME="month" onChange="selectDate()" size="1" class="buttonNormal">';
	code+='									<option>' + currentLabels[1] +'</option>';
	code+='									<option>' + currentLabels[2] +'</option>';
	code+='									<option>' + currentLabels[3] +' </option>';
	code+='									<option>' + currentLabels[4] +' </option>';
	code+='									<option>' + currentLabels[5] +' </option>';
	code+='									<option>' + currentLabels[6] +' </option>';
	code+='									<option>' + currentLabels[7] +' </option>';
	code+='									<option>' + currentLabels[8] +' </option>';
	code+='									<option>' + currentLabels[9] +' </option>';
	code+='									<option>' + currentLabels[10] +' </option>';
	code+='									<option>' + currentLabels[11] +' </option>';
	code+='									<option>' + currentLabels[12] +' </option>';
	code+='								</select>';
	code+='								<input NAME="year" TYPE="TEXT" SIZE="4" MAXLENGTH="4" onChange="selectDate()" class="buttonNormal">';
	code+='							</form>';
	code+='						</td>';
	code+='					</tr>';
	code+='					<tr align="center">';
	code+='						<td align="center" colspan="7">';
	
	var item= new Array(5);
	item[0]= new Button();
	item[0].name= '&lt;&lt;';
	item[0].action= 'setPreviousYear()';
	
	item[1]= new Button();
	item[1].name= '&lt;';
	item[1].action= 'setPreviousMonth()';
	
	item[2]= new Button();
	item[2].name= currentLabels[0];
	item[2].action= 'setToday()';
	
	item[3]= new Button();
	item[3].name= '&gt;';
	item[3].action= 'setNextMonth()';
	
	item[4]= new Button();
	item[4].name= '&gt;&gt;';
	item[4].action= 'setNextYear()';
	code+= createBar(item, 5);
	
	code+='						</td>';
	code+='					</tr>';
	code+='					<tr class="buttonTitle" valign="top">';
	code+='					  <td align="center" valign="top">Di</td>';
	code+='					  <td align="center">Lu</td>';
	code+='					  <td align="center">Ma</td>';
	code+='					  <td align="center">Me</td>';
	code+='					  <td align="center">Je</td>';
	code+='					  <td align="center">Ve</td>';
	code+='					  <td align="center">Sa</td>';
	code+='					</tr>';	
	code+= createCalendar();
	code+='				</table>';
	code+='			</td>';
	code+='			<td width="18" bgcolor="#475298"><img src="../../images/spacer.gif" width="18" height="1"></td>';
	code+='			<td width="1" bgcolor="#FF3300"><img src="../../images/spacer.gif" width="1" height="1"></td>';
	code+='			<td width="2" bgcolor="#CECECE"><img src="../../images/spacer.gif" width="2" height="1"></td>';
	code+='		</tr>';
	code+='		<tr>';
	code+='			<td colspan="2" rowspan="3"><IMG SRC="../../images/bl.gif" ALT="" WIDTH="21" HEIGHT="22"/></td>';
	code+='			<td height="19" bgcolor="#475298"><img src="../../images/spacer.gif" width="1" height="19"></td>';
	code+='			<td colspan="3" rowspan="3"><IMG SRC="../../images/close.gif" ALT="" WIDTH="21" HEIGHT="22" border="0" onClick="hide_myCalendar();"/></td>';
	code+='		</tr>';
	code+='		<tr><td height="1" bgcolor="#FF3100"><img src="../../images/spacer.gif" width="1" height="1"></td></tr>';
	code+='		<tr><td height="2" bgcolor="#CECECE"><img src="../../images/spacer.gif" width="2" height="1"></td></tr>';
	code+='	</table>';
	code+='</div>';
	
	return code;
}

/***********************/
/* FONCTIONS GENERALES */
/***********************/
function show_myCalendar(e, myPattern) {
	if(myPattern != "" && myPattern != null)
		pattern= myPattern;
	else
		pattern= defaultPattern;
	// update pattern fields
	var i= 0;
	while(pattern.charAt(i) == 'd' || pattern.charAt(i) == 'M' || pattern.charAt(i) == 'y') i++;
	separator= (i<pattern.length)?pattern.charAt(i):"";
	nb_d= pattern.lastIndexOf("d") - pattern.indexOf("d") + 1;
	nb_m= pattern.lastIndexOf("M") - pattern.indexOf("M") + 1;
	nb_y= pattern.lastIndexOf("y") - pattern.indexOf("y") + 1;
	
	if(nb_d > 2 || nb_m > 2 || nb_d+1+nb_m+1+nb_y != pattern.length){
		alert("The pattern " + pattern + " is not implemented. The calendar will not be displayed.");
		return false;
	}
	
	setDate();
	if(!e) e= event;
	var obj= document.getElementById('calendar_div');
	var rightedge = document.body.clientWidth- e.clientX;
	var bottomedge = document.body.clientHeight- e.clientY;
	if (rightedge < obj.offsetWidth)
		obj.style.left = document.body.scrollLeft + e.clientX - obj.offsetWidth;
	else
		obj.style.left = document.body.scrollLeft + e.clientX;
	if (bottomedge < document.getElementById('ie5menu').offsetHeight)
		obj.style.top = document.body.scrollTop + e.clientY - obj.offsetHeight;
	else
		obj.style.top = document.body.scrollTop + e.clientY;
	obj.style.visibility = "visible";
	return false;
}
function hide_myCalendar() {
    for (i=0; i<42; i++)  {
		document.getElementById("but" + i).style.visibility = 'hidden';
    }
	document.getElementById('calendar_div').style.visibility = "hidden";
}


/************************************/
/* FONCTIONS DE GESTION DU CALENDAR */
/************************************/
function setDate() {
	this.dateField = window.dateField;
	var  inDate    = this.dateField.value;
    
	// SET DAY MONTH AND YEAR TO TODAY'S DATE
	var now   = new Date();
	var day   = now.getDate();
	var month = now.getMonth();
	var year  = now.getFullYear();
	
	var inDay = "", inMonth = "", inYear = "";
        
	// IF A DATE WAS PASSED IN THEN PARSE THAT DATE
	if(separator == "" && inDate.length == pattern.length){
		inDay   = inDate.substring(pattern.indexOf("d"),pattern.lastIndexOf("d") + 1);
		inMonth = inDate.substring(pattern.indexOf("M"),pattern.lastIndexOf("M") + 1);
		inYear  = inDate.substring(pattern.indexOf("y"),pattern.lastIndexOf("y") + 1);
	}
	else if(inDate.indexOf(separator) != -1){
		if(pattern.charAt(0) == 'd'){
			inDay   = inDate.substring(0,inDate.indexOf(separator));
			if(pattern.charAt(pattern.indexOf(separator) + 1) == 'M'){
				inMonth = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inYear  = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
			else{
				inYear  = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inMonth = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
		}
		else if(pattern.charAt(0) == 'M'){
			inMonth   = inDate.substring(0,inDate.indexOf(separator));
			if(pattern.charAt(pattern.indexOf(separator) + 1) == 'd'){
				inDay = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inYear  = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
			else{
				inYear  = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inDay = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
		}
		else if(pattern.charAt(0) == 'y'){
			inYear   = inDate.substring(0,inDate.indexOf(separator));
			if(pattern.charAt(pattern.indexOf(separator) + 1) == 'd'){
				inDay = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inMonth  = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
			else{
				inMonth  = inDate.substring(inDate.indexOf(separator) + 1,inDate.lastIndexOf(separator));
				inDay = inDate.substring(inDate.lastIndexOf(separator) + 1,inDate.length);
			}
		}
	}
	else
		alert("Wrong date format. It must be like the pattern " + pattern);	
	
	if (inDay != ""){
		if (inDay.substring(0,1) == "0" && inDay.length > 1)
		    inDay = inDay.substring(1,inDay.length);
		inDay = parseInt(inDay,10);
		day = inDay;
	}
	if (inMonth != ""){
		if (inMonth.substring(0,1) == "0" && inMonth.length > 1)
	    		inMonth = inMonth.substring(1,inMonth.length);
		inMonth = parseInt(inMonth,10);
	    	month = inMonth-1;
	}
	if (inYear != ""){
		if(inYear.length == 2){
			if(parseInt(inYear,10) < 70)
				inYear = "20" + inYear;
			else
				inYear = "19" + inYear;
		}
	    	year = inYear;
	}
	
	this.focusDay                           = day;
	document.calControl.month.selectedIndex = month;
	document.calControl.year.value          = year;
	displayCalendar(day, month, year);
}


function setToday() {
    // SET DAY MONTH AND YEAR TO TODAY'S DATE
    var now   = new Date();
    var day   = now.getDate();
    var month = now.getMonth();
    var year  = now.getFullYear();
    this.focusDay                           = day;
    document.calControl.month.selectedIndex = month;
    document.calControl.year.value          = year;
    displayCalendar(day, month, year);
}


function isFourDigitYear(year) {
    if (year.length != 4) {
        document.calControl.year.select();
        document.calControl.year.focus();
    }
    else {
        return true;
    }
}


function selectDate() {
    var year  = document.calControl.year.value;
    if (isFourDigitYear(year)) {
        var day   = 0;
        var month = document.calControl.month.selectedIndex;
        displayCalendar(day, month, year);
    }
}


function setPreviousYear() {
    var year  = document.calControl.year.value;
    if (isFourDigitYear(year)) {
        var day   = 0;
        var month = document.calControl.month.selectedIndex;
        year--;
        document.calControl.year.value = year;
        displayCalendar(day, month, year);
    }
}


function setPreviousMonth() {
    var year  = document.calControl.year.value;
    if (isFourDigitYear(year)) {
        var day   = 0;
        var month = document.calControl.month.selectedIndex;
        if (month == 0) {
            month = 11;
            if (year > 1000) {
                year--;
                document.calControl.year.value = year;
            }
        }
        else {
            month--;
        }
        document.calControl.month.selectedIndex = month;
        displayCalendar(day, month, year);
    }
}


function setNextMonth() {
    var year  = document.calControl.year.value;
    if (isFourDigitYear(year)) {
        var day   = 0;
        var month = document.calControl.month.selectedIndex;
        if (month == 11) {
            month = 0;
            year++;
            document.calControl.year.value = year;
        }
        else {
            month++;
        }
        document.calControl.month.selectedIndex = month;
        displayCalendar(day, month, year);
    }
}


function setNextYear() {
    var year  = document.calControl.year.value;
    if (isFourDigitYear(year)) {
        var day   = 0;
        var month = document.calControl.month.selectedIndex;
        year++;
        document.calControl.year.value = year;
        displayCalendar(day, month, year);
    }
}


function displayCalendar(day, month, year) {       

    day     = parseInt(day,10);
    month   = parseInt(month,10);
    year    = parseInt(year,10);
    var i   = 0;
    var now = new Date();

    if (day == 0) {
        var nowDay = now.getDate();
    }
    else {
        var nowDay = day;
    }
    var days         = getDaysInMonth(month+1,year);
    var firstOfMonth = new Date (year, month, 1);
    var startingPos  = firstOfMonth.getDay();
    days += startingPos;

    // MAKE BEGINNING NON-DATE BUTTONS BLANK AND HIDDEN
    for (i = 0; i < startingPos; i++) {
		obj= document.getElementById("but" + i);
		changeCaption(obj, '&nbsp;');
		obj.style.visibility = 'hidden';
    }

    // SET VALUES FOR DAYS OF THE MONTH
    for (i = startingPos; i < days; i++){
		myDay= (i-startingPos+1);
		obj= document.getElementById("but" + i);
		changeCaption(obj, myDay);
		obj.style.visibility = 'visible';
		if((focusDay+startingPos-1) == i && obj.focus){
			activeButton(obj, obj.id);
			returnDate(obj);
		}
    }

    // MAKE REMAINING NON-DATE BUTTONS BLANK AND HIDDEN
    for (i=days; i<42; i++)  {
		obj= document.getElementById("but" + i);
		changeCaption(obj, '&nbsp;');
		obj.style.visibility = 'hidden';
    }
}


// GET NUMBER OF DAYS IN MONTH
function getDaysInMonth(month,year)  {
    var days;
    if (month==1 || month==3 || month==5 || month==7 || month==8 ||
        month==10 || month==12)  days=31;
    else if (month==4 || month==6 || month==9 || month==11) days=30;
    else if (month==2)  {
        if (isLeapYear(year)) {
            days=29;
        }
        else {
            days=28;
        }
    }
    return (days);
}


// CHECK TO SEE IF YEAR IS A LEAP YEAR
function isLeapYear (Year) {
    if (((Year % 4)==0) && ((Year % 100)!=0) || ((Year % 400)==0)) {
        return (true);
    }
    else {
        return (false);
    }
}


// SET FORM FIELD VALUE TO THE DATE SELECTED
function returnDate(obj){
    var day   = getCaption(obj);
    var month = (document.calControl.month.selectedIndex)+1;
    var year  = document.calControl.year.value;

	activeButton(obj, obj.id);
    if (nb_y == 2)
    	year= (""+year).substring((""+year).length - 2, (""+year).length);
    if ((""+month).length == 1 && nb_m == 2)
        month="0"+month;
    if ((""+day).length == 1 && nb_d == 2)
        day="0"+day;
    if (day != ""){	    
	    if(pattern.charAt(0) == 'd'){
	    	if(separator == "")
	    		index= pattern.lastIndexOf("d");
	    	else
	    		index= pattern.indexOf(separator);
		if(pattern.charAt(index + 1) == 'M')
			dateField.value = day + separator + month + separator + year;
		else
			dateField.value = day + separator + year + separator + month;
	    }
	    else if(pattern.charAt(0) == 'M'){
	    	if(separator == "")
	    		index= pattern.lastIndexOf("M");
	    	else
	    		index= pattern.indexOf(separator);
		if(pattern.charAt(index + 1) == 'd')
			dateField.value = month + separator + day + separator + year;
		else
			dateField.value = month + separator + year + separator + day;
	     }
	    else if(pattern.charAt(0) == 'y'){
	    	if(separator == "")
	    		index= pattern.lastIndexOf("y");
	    	else
	    		index= pattern.indexOf(separator);
		if(pattern.charAt(index + 1) == 'd')
			dateField.value = year + separator + day + separator + month;
		else
			dateField.value = year + separator + month + separator + day;
	     }
     }    
    
        
}