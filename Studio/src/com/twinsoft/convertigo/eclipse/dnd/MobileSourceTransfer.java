/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class MobileSourceTransfer extends ByteArrayTransfer {
	private static final String MYTYPENAME = "MobileSource";
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static MobileSourceTransfer _instance = new MobileSourceTransfer();
	private MobileSource mobileSource;
	
	public MobileSourceTransfer() {
		
	}

	public static MobileSourceTransfer getInstance () {
	 	return _instance;
	}
	
	@Override
	protected String[] getTypeNames(){
		return new String[]{MYTYPENAME};
	}
	
	@Override
	protected int[] getTypeIds(){
		return new int[] {MYTYPEID};
	}

	@Override
	protected void javaToNative(Object object, TransferData transferData) {
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
	 	if (isSupportedType(transferData)) {
	 		return mobileSource;
	 	}
	 	return null;
	}

	public MobileSource getMobileSource() {
		return mobileSource;
	}

	public void setMobileSource(Object mobileSource) {
		if (mobileSource instanceof MobileSource) {
			this.mobileSource = (MobileSource) mobileSource;
		} else {
			this.mobileSource = null;
		}
	}
}
