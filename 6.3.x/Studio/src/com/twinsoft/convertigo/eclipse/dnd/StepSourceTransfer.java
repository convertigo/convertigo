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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class StepSourceTransfer extends ByteArrayTransfer {
	private static final String MYTYPENAME = "StepSource";
	private static final int MYTYPEID = registerType(MYTYPENAME);
	private static StepSourceTransfer _instance = new StepSourceTransfer();

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
	public TransferData[] getSupportedTypes() {
		return super.getSupportedTypes();
	}

	@Override
	public boolean isSupportedType(TransferData transferData) {
		return super.isSupportedType(transferData);
	}

	@Override
	protected void javaToNative(Object object, TransferData transferData) {
		if (object == null || !(object instanceof StepSource)) return;
		
	 	if (isSupportedType(transferData)) {
	 		StepSource myTypes = (StepSource) object;	
	 		try {
	 			// write data to a byte array and then ask super to convert to pMedium
	 			ByteArrayOutputStream out = new ByteArrayOutputStream();
	 			DataOutputStream writeOut = new DataOutputStream(out);
 				byte[] buff;
 				buff = myTypes.getPriority().getBytes();
 				writeOut.writeInt(buff.length);
 				writeOut.write(buff);
 				
 				buff = myTypes.getXpath().getBytes();
 				writeOut.writeInt(buff.length);
 				writeOut.write(buff);

 				byte[] buffer = out.toByteArray();
	 			writeOut.close();
	 
	 			super.javaToNative(buffer, transferData);
	 			
	 		} catch (Exception e) {
	 			//e.printStackTrace(System.out);
	 		}
	 	}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
	 	if (isSupportedType(transferData)) {
	 		
	 		byte[] buffer = (byte[])super.nativeToJava(transferData);
	 		if (buffer == null) return null;
	 		
	 		StepSource myData = null;
	 		try {
	 			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
	 			DataInputStream readIn = new DataInputStream(in);
	 			while(readIn.available() > 20) {
	 				byte[] buff;
	 				buff = new byte[readIn.readInt()];
	 				readIn.read(buff);
	 				String p = new String(buff);
	 				
	 				buff = new byte[readIn.readInt()];
	 				readIn.read(buff);
	 				String x = new String(buff);
	 				
	 				myData = new StepSource(p,x);
	 			}
	 			readIn.close();
	 		} catch (Exception e) {
	 			//e.printStackTrace(System.out);
	 			return null;
	 		}
	 		return myData;
	 	}
	 
	 	return null;
	}

	
}
