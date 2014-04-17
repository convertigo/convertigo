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

package com.twinsoft.convertigo.beans.mobileplatforms;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.engine.enums.Visibility;

public class BlackBerry extends MobilePlatform {

	private static final long serialVersionUID = 1092999336588542618L;
	
	private String bbKeyTitle = "";
	private String bbKeyPw = "";
	
	public String getBbKeyTitle() {
		return bbKeyTitle;
	}
	public void setBbKeyTitle(String bbKeyTitle) {
		this.bbKeyTitle = bbKeyTitle;
	}

	public String getBbKeyPw() {
		return bbKeyPw;
	}

	public void setBbKeyPw(String bbKeyPw) {
		this.bbKeyPw = bbKeyPw;
	}

	@Override
	public String getPackageType() {
		return "jad";
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("bbKeyPw".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

}
