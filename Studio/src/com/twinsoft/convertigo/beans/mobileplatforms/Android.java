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

public class Android extends MobilePlatform {

	private static final long serialVersionUID = 1092999336588542617L;
	
	private String androidCertificateTitle = "";
	private String androidCertificatePw = "";
	private String androidKeystorePw = "";
	
	public Android() {
		super();
		this.cordovaPlatform = "android";
	}

	public String getAndroidCertificateTitle() {
		return androidCertificateTitle;
	}

	public void setAndroidCertificateTitle(String androidCertificateTitle) {
		this.androidCertificateTitle = androidCertificateTitle;
	}

	public String getAndroidCertificatePw() {
		return androidCertificatePw;
	}

	public void setAndroidCertificatePw(String androidCertificatePw) {
		this.androidCertificatePw = androidCertificatePw;
	}

	public String getAndroidKeystorePw() {
		return androidKeystorePw;
	}

	public void setAndroidKeystorePw(String androidKeystorePw) {
		this.androidKeystorePw = androidKeystorePw;
	}

	@Override
	public String getPackageType() {
		return "apk";
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("androidCertificatePw".equals(propertyName)) {
			return true;
		}
		
		if ("androidKeystorePw".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}
}
