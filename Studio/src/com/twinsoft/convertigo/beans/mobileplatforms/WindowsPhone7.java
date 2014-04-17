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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/mobiledevices/WindowsPhone7.java $
 * $Author: julienda $
 * $Revision: 31472 $
 * $Date: 2012-08-23 17:53:27 +0200 (jeu., 23 août 2012) $
 */

package com.twinsoft.convertigo.beans.mobileplatforms;

import com.twinsoft.convertigo.beans.core.MobilePlatform;

public class WindowsPhone7 extends MobilePlatform {

	private static final long serialVersionUID = 1092999336588542621L;
	
	private String windowsPhone7PublisherIDTitle = "";
	private String windowsPhone7PublisherID = "";
	
	public String getWindowsPhone7PublisherIDTitle() {
		return windowsPhone7PublisherIDTitle;
	}

	public void setWindowsPhone7PublisherIDTitle(
			String windowsPhone7PublisherIDTitle) {
		this.windowsPhone7PublisherIDTitle = windowsPhone7PublisherIDTitle;
	}

	public String getWindowsPhone7PublisherID() {
		return windowsPhone7PublisherID;
	}

	public void setWindowsPhone7PublisherID(String windowsPhone7PublisherID) {
		this.windowsPhone7PublisherID = windowsPhone7PublisherID;
	}
	
	@Override
	public String getPackageType() {
		return "xap";
	}
}
