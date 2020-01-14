/*
 * Copyright (c) 2001-2020 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.mobileplatforms;

import com.twinsoft.convertigo.beans.core.MobilePlatform;

public class WindowsPhone8 extends MobilePlatform implements WindowsPhoneKeyProvider {

	private static final long serialVersionUID = 1092999336588542622L;
	
	private String winphonePublisherIdTitle = "";
	
	public WindowsPhone8() {
		super();
		this.cordovaPlatform = "wp8";
	}

	public String getWinphonePublisherIdTitle() {
		return winphonePublisherIdTitle;
	}

	public void setWinphonePublisherIdTitle(String winphonePublisherIdTitle) {
		this.winphonePublisherIdTitle = winphonePublisherIdTitle;
	}

	@Override
	public String getPackageType() {
		return "xap";
	}

	@Override
	public String getCordovaPlatform() {
		return "wp8";
	}
}
