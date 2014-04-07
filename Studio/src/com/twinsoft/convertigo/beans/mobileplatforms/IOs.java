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

public class IOs extends MobilePlatform {

	private static final long serialVersionUID = 370167918956703015L;

	private String iOSCertificateTitle = "";
	private String iOSCertificatePw = "";
	
	public String getiOSCertificateTitle() {
		return iOSCertificateTitle;
	}

	public void setiOSCertificateTitle(String iOSCertificateTitle) {
		this.iOSCertificateTitle = iOSCertificateTitle;
	}

	public String getiOSCertificatePw() {
		return iOSCertificatePw;
	}

	public void setiOSCertificatePw(String iOSCertificatePw) {
		this.iOSCertificatePw = iOSCertificatePw;
	}

	@Override
	public String getPackageType() {
		return "ipa";
	}
}
