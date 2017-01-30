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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dnd/StepSourceTransfer.java $
 * $Author: nicolasa $
 * $Revision: 38509 $
 * $Date: 2014-11-05 15:41:09 +0100 (mer., 05 nov. 2014) $
 */

package com.twinsoft.convertigo.eclipse.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class PaletteSourceTransfer extends ByteArrayTransfer {
	private static final String MYTYPENAME = "PaletteSource";
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static PaletteSourceTransfer _instance = new PaletteSourceTransfer();
	private PaletteSource paletteSource;

	private PaletteSourceTransfer() {
	}

	public static PaletteSourceTransfer getInstance () {
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
	 		return paletteSource;
	 	}
	 
	 	return null;
	}

	public PaletteSource getPaletteSource() {
		return paletteSource;
	}

	public void setPaletteSource(Object paletteSource) {
		if (paletteSource instanceof PaletteSource) {
			this.paletteSource = (PaletteSource) paletteSource;
		} else {
			this.paletteSource = null;
		}
	}
}
