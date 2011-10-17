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

import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.twinj.iJavelin;

public class JavelinUtils {

    public static boolean isSameAttribute(int attribute1, int attribute2) {
		boolean b1 = ((attribute2 & JavelinExtractionRule.DONT_CARE_FOREGROUND_ATTRIBUTE) == 0 ? (attribute1 & iJavelin.AT_INK) == (attribute2 & iJavelin.AT_INK) : true);
		boolean b2 = ((attribute2 & JavelinExtractionRule.DONT_CARE_BACKGROUND_ATTRIBUTE) == 0 ? (attribute1 & iJavelin.AT_PAPER) == (attribute2 & iJavelin.AT_PAPER) : true);
		boolean b3 = ((attribute2 & JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) == 0 ? (attribute1 & iJavelin.AT_BOLD) == (attribute2 & iJavelin.AT_BOLD) : true);
		boolean b4 = ((attribute2 & JavelinExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE) == 0 ? (attribute1 & iJavelin.AT_UNDERLINE) == (attribute2 & iJavelin.AT_UNDERLINE) : true);
		boolean b5 = ((attribute2 & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) == 0 ?  (attribute1 & iJavelin.AT_INVERT) == (attribute2 & iJavelin.AT_INVERT) : true);
		boolean b6 = ((attribute2 & JavelinExtractionRule.DONT_CARE_BLINK_ATTRIBUTE) == 0 ? (attribute1 & iJavelin.AT_BLINK) == (attribute2 & iJavelin.AT_BLINK) : true);

		// if 'Dont care REVERSE' is Checked, consider Inverted INK and PAPER to be Valid
		if (!b1 && (attribute2 & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) != 0) {
			if ((attribute1 & iJavelin.AT_INK) == ((attribute2 & iJavelin.AT_PAPER) >> 3))
				b1 = true;
			
		}
		if (!b2 && (attribute2 & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) != 0) {
			if (((attribute1 & iJavelin.AT_PAPER)>>3) == (attribute2 & iJavelin.AT_INK))
				b2 = true;
			
		}
		return (b1 && b2 && b3 && b4 && b5 && b6);
    }

}
