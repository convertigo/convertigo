/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


class PartInputStream extends InputStream {
	private InputStream inputStream;
	private byte[] boundary;
	
	private byte[] search;
	private int iSearch = 0;
	
	private byte[] saved;
	private int iSaved = 0;
	private int lSaved = 0;
	
	private int lastRead = 0;
	
	PartInputStream(InputStream inputStream, byte[] boundary) {
		this.inputStream = inputStream;
		this.boundary = boundary;
		
		search = new byte[boundary.length];
		saved = new byte[boundary.length];
	}
	
	public boolean isStreamEnd() {
		return lastRead == -1 && iSaved == lSaved;
	}
	
	public boolean isPartEnd() {
		return iSearch == search.length;
	}
	
	boolean nextPart() {
		if (iSearch == search.length) {
			iSearch = 0;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean nextPart(byte[] boundary) {
		if (iSearch == search.length) {
			this.boundary = boundary;
			
			iSearch = 0;
			search = new byte[boundary.length];
			
			saved = Arrays.copyOf(saved, boundary.length);
			
			return true;
		} else {
			return false;
		}
	}
	
	private int peak() throws IOException {
		if (isPartEnd()) {
			return -1;
		} else if (iSaved < lSaved) {
			return ((int) saved[iSaved++]) & 0xff;
		} else {
			return lastRead = inputStream.read();
		}
	}
	
	@Override
	public int read() throws IOException {
		int iByte = peak();
		
		while (iByte != -1) {
			byte b = (byte) iByte;
			
			if (b == boundary[iSearch]) {
				search[iSearch++] = b;
				iByte = peak();
			} else if (iSearch == 0) {
				return iByte;
			} else {
				byte[] pivot = search;
				search = saved;
				saved = pivot;
				
				saved[iSearch++] = b;
				lSaved = iSearch;
				iSearch = iSaved = 0;
				
				return saved[iSaved++];
			}
		}
		
		return -1;
	}
}