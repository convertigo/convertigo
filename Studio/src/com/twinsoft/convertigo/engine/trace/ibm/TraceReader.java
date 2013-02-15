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

package com.twinsoft.convertigo.engine.trace.ibm;

import java.io.*;

import com.twinsoft.convertigo.engine.Engine;

public class TraceReader {

	public static final String TNVT = "TNVT";

	public static final String TN3270 = "TN3270";

	public static final String TN5250 = "TN5250";

	public static final String RECV = "RECV";

	public static final String SEND = "SEND";

	public static final String NVT_DATA = "NVT_DATA";

	public static final String RECORD = "RECORD";

	public static final String EOR = "END_OF_RECORD";

	public static final String ENDOFFILE = "###EOF###";

	public static final String WAIT_COMMAND = ".WAIT ";

	private BufferedReader bufferedReader;

	private static int hexaInts[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

	private static String hexaChars = "0123456789ABCDEF";

	public TraceReader(String s) throws IOException {
		FileReader fr = null;
		BufferedReader br = null;
		try {
			File f = new File(s);
			try {
				fr = new FileReader(f);
			} catch (FileNotFoundException filenotfoundexception) {
				Engine.logEngine.error("Trace file (" + s + ") not found!");
				throw filenotfoundexception;
			}
			
			// Bugfix #891
			// Get the trace file in memory to avoid file lock
			br = new BufferedReader(fr);
			StringBuffer buffer = new StringBuffer("");
			String line;
			while ((line = br.readLine()) != null) {
				buffer.append(line);
				buffer.append("\r\n");
			}
			
			bufferedReader = new BufferedReader(new StringReader(buffer.toString()));
		}
		finally {
			if (fr != null) fr.close();
			if (br != null) br.close();
		}
	}

	public void close() {
		try {
			bufferedReader.close();
			return;
		} catch (IOException _ex) {
			return;
		}
	}

	public String getNextTelnet() {
		String s = null;
		do {
			s = _mth039CD();
			boolean flag1 = lookForString(s, "TNVT")
					|| lookForString(s, "TN3270") || lookForString(s, "TN5250");
			if (flag1 || s.equals("###EOF###"))
				break;
			if (s.startsWith(".WAIT ")) {
				String s1 = s.substring(".WAIT ".length(), s.length());
				try {
					Thread.currentThread();
					Thread.sleep(Integer.valueOf(s1).longValue());
				} catch (InterruptedException _ex) {
				}
			}
		} while (true);
		return s;
	}

	public byte[] getData() {
		int i = 0;
		byte abyte0[] = new byte[16];
		int j = 0;
		byte abyte1[] = new byte[4096];
		char ac[] = new char[2];
		try {
			for (String s = bufferedReader.readLine(); s.startsWith(" - ");) {
				boolean flag1 = lookForString(s, "...same as above...");
				if (flag1) {
					int k = 0;
					ac[0] = s.charAt(4);
					ac[1] = s.charAt(5);
					int i1 = _mth039DD(ac);
					ac[0] = s.charAt(9);
					ac[1] = s.charAt(10);
					int k1 = _mth039DD(ac);
					k = k1 - i1;
					k++;
					for (int l1 = 0; l1 < k; l1++) {
						for (int j2 = 0; j2 < 16; j2++) {
							abyte1[j] = abyte0[j2];
							j++;
						}

					}

				} else {
					int l = 0;
					int j1 = 0;
					boolean flag2 = false;
					String s1 = s.substring(8, 59);
					do {
						for (int i2 = 0; i2 < 4; i2++) {
							ac[0] = s1.charAt(l);
							ac[1] = s1.charAt(l + 1);
							if (ac[0] != ' ' && ac[1] != ' ') {
								byte byte0 = (byte) _mth039DD(ac);
								abyte0[i] = byte0;
								i++;
								abyte1[j] = byte0;
								j++;
								l += 3;
								j1++;
								continue;
							}
							flag2 = true;
							break;
						}

						if (j1 == 16 || flag2)
							break;
						l++;
					} while (true);
				}
				s = bufferedReader.readLine();
				i = 0;
			}

		} catch (IOException ioexception) {
			Engine.logEngine.error("Exception while getting data", ioexception);
			return null;
		}
		if (j == 0) {
			return null;
		} else {
			byte abyte2[] = new byte[j];
			System.arraycopy(abyte1, 0, abyte2, 0, j);
			return abyte2;
		}
	}

	private String _mth039CD() {
		String s = null;
		try {
			for (s = bufferedReader.readLine(); s != null && !s.startsWith("."); s = bufferedReader.readLine())
				;
			if (s == null)
				s = "###EOF###";
		} catch (IOException ioexception) {
			Engine.logEngine.error("Exception while reading the trace file", ioexception);
			s = "###EOF###";
		}
		return s;
	}

	public boolean lookForString(String s, String s1) {
		return s.indexOf(s1) != -1;
	}

	private int _mth039DD(char ac[]) {
		int i = 0;
		for (int j = 0; j < ac.length; j++)
			i += hexaInts[hexaChars.indexOf(ac[j])]
					* (1 << (ac.length - 1 - j) * 4);

		return i;
	}

}