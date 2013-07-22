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

package com.twinsoft.convertigo.engine.util;

import java.util.Locale;

public class DateUtils {
	
	public static Locale getLocale(String isoCountryCode){
		Locale locale = Locale.FRENCH;
		Locale[] locales = Locale.getAvailableLocales();
	    for (int i = 0; i < locales.length; i++){
	    	locale = locales[i];
	    	//System.out.println(i + ": " + locale.getLanguage() + ", " + locale.getCountry() + ", " + locale.getDisplayName());
	    	if (isoCountryCode.equals(locale.getCountry()))
	    		return locale;
	    }
	    return locale;
	}
	

//	public static void main(String[] args) {
//
//	}

}
