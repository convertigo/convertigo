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

/***********************/
/* GESTION DES COOKIES */
/***********************/
function getCookieVal(offset)
{
   var endstr=document.cookie.indexOf (";", offset);
   if (endstr==-1) endstr=document.cookie.length;
   return unescape(document.cookie.substring(offset, endstr));
}
function getCookie(nom)
{
   var arg=nom+"=";
   var alen=arg.length;
   var clen=document.cookie.length;
   var i=0;
   while (i<clen)
   {
      var j=i+alen;
      if (document.cookie.substring(i, j)==arg) return getCookieVal(j);
      i=document.cookie.indexOf(" ",i)+1;
      if (i==0) break;
   }
   return null;
}
function setCookie(nom, valeur){
	var today = new Date();
	var today_plus_one_year = today.getTime() + 3.15576e+10;
	var one_year = new Date(today_plus_one_year);
	document.cookie= nom + "=" + escape(valeur) + "; expires=" + one_year;
}