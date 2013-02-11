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

package com.twinsoft.convertigo.eclipse.property_editors;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class JavelinLanguageEditor extends PropertyWithTagsEditor {

	public static final String LABEL_LANGUAGE_FRENCH = "french";
	public static final String LABEL_LANGUAGE_ENGLISH_EU = "english eu";
	public static final String LABEL_LANGUAGE_GERMAN = "german";
	public static final String LABEL_LANGUAGE_NORSK = "norsk";
	public static final String LABEL_LANGUAGE_SWEDISH = "swedish";
	public static final String LABEL_LANGUAGE_ITALIAN = "italian";
	public static final String LABEL_LANGUAGE_SPANISH = "spanish";
	public static final String LABEL_LANGUAGE_IRISH = "irish";
	public static final String LABEL_LANGUAGE_BELGIAN = "belgian";
	public static final String LABEL_LANGUAGE_THAI = "thai";
	public static final String LABEL_LANGUAGE_TCHEK = "tchek";
	public static final String LABEL_LANGUAGE_ISLAND = "island";
	public static final String LABEL_LANGUAGE_GREEK = "greek";
	public static final String LABEL_LANGUAGE_CYRILLIC = "cyrillic";
	public static final String LABEL_LANGUAGE_RUSSIAN = "russian";
	public static final String LABEL_LANGUAGE_TURKISH = "turkish";
	public static final String LABEL_LANGUAGE_ENGLISH_EU_EURO = "english eu (euro)";
	public static final String LABEL_LANGUAGE_GERMAN_EURO = "german (euro)";
	public static final String LABEL_LANGUAGE_NORSK_EURO = "norsk (euro)";
	public static final String LABEL_LANGUAGE_SWEDISH_EURO = "swedish (euro)";
	public static final String LABEL_LANGUAGE_ITALIAN_EURO = "italian (euro)";
	public static final String LABEL_LANGUAGE_SPANISH_EURO = "spanish (euro)";
	public static final String LABEL_LANGUAGE_IRISH_EURO = "irish (euro)";
	public static final String LABEL_LANGUAGE_FRENCH_EURO = "french_euro (euro)";
	public static final String LABEL_LANGUAGE_BELGIAN_EURO = "belgian (euro)";
	public static final String LABEL_LANGUAGE_ISLAND_EURO = "island (euro)";
	
    public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject) {
        return new String[] { 
    		LABEL_LANGUAGE_FRENCH,
    		LABEL_LANGUAGE_FRENCH_EURO,
    		LABEL_LANGUAGE_ENGLISH_EU,
    		LABEL_LANGUAGE_ENGLISH_EU_EURO,
    		LABEL_LANGUAGE_GERMAN,
    		LABEL_LANGUAGE_GERMAN_EURO,
    		LABEL_LANGUAGE_NORSK,
    		LABEL_LANGUAGE_NORSK_EURO,
    		LABEL_LANGUAGE_SWEDISH,
    		LABEL_LANGUAGE_SWEDISH_EURO,
    		LABEL_LANGUAGE_ITALIAN,
    		LABEL_LANGUAGE_ITALIAN_EURO,
    		LABEL_LANGUAGE_SPANISH,
    		LABEL_LANGUAGE_SPANISH_EURO,
    		LABEL_LANGUAGE_IRISH,
    		LABEL_LANGUAGE_IRISH_EURO,
    		LABEL_LANGUAGE_BELGIAN,
    		LABEL_LANGUAGE_BELGIAN_EURO,
    		LABEL_LANGUAGE_ISLAND,
    		LABEL_LANGUAGE_ISLAND_EURO,
    		LABEL_LANGUAGE_THAI,
    		LABEL_LANGUAGE_TCHEK,
    		LABEL_LANGUAGE_GREEK,
    		LABEL_LANGUAGE_CYRILLIC,
    		LABEL_LANGUAGE_RUSSIAN,
    		LABEL_LANGUAGE_TURKISH,
        };
    }
}
