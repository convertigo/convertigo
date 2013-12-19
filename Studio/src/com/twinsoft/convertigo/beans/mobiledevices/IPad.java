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

package com.twinsoft.convertigo.beans.mobiledevices;

import com.twinsoft.convertigo.beans.core.MobileDevice;

public class IPad extends MobileDevice {

	private static final long serialVersionUID = 370167918956703015L;
	
	public IPad() {
		super();
		setScreenWidth(1024);
		setScreenHeight(768);
		setResourcesPath(MobileDevice.RESOURCES_PATH + "/");
	}

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
}
