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

package com.twinsoft.convertigo.eclipse.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class StepSourceTransfer extends ByteArrayTransfer {
	private static final String MYTYPENAME = "StepSource";
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static StepSourceTransfer _instance = new StepSourceTransfer();
	private StepSource stepSource;

	private StepSourceTransfer() {
	}

	public static StepSourceTransfer getInstance () {
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
	 		return stepSource;
	 	}
	 
	 	return null;
	}

	public StepSource getStepSource() {
		return stepSource;
	}

	public void setStepSource(Object stepSource) {
		if (stepSource instanceof StepSource) {
			this.stepSource = (StepSource) stepSource;
		} else {
			this.stepSource = null;
		}
	}
}
