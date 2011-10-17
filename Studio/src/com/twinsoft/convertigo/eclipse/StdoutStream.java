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

package com.twinsoft.convertigo.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.ui.console.MessageConsoleStream;

public class StdoutStream extends PrintStream {

	protected ConvertigoPlugin convertigoPlugin;
	protected MessageConsoleStream consoleStream;
	
	public StdoutStream() {
		super(new ByteArrayOutputStream(1));
		convertigoPlugin = ConvertigoPlugin.getDefault();
		consoleStream = convertigoPlugin.stdoutConsoleStream;
	}
	
	public void print(String s) {
    	consoleStream.print(s);
	}
	
	public void println() {
		print("\n");
	}
	
	public void println(boolean b) {
    	print(b);
    	println();
	}
	
	public void println(char c) {
    	print(c);
    	println();
	}
	
	public void println(char[] s) {
    	print(s);
    	println();
    }

	public void println(double d) {
    	print(d);
    	println();
	}
	
	public void println(float f) {
    	print(f);
    	println();
	}
	
	public void println(int i) {
    	print(i);
    	println();
	}
	
	public void println(long l) {
    	print(l);
    	println();
	}
	
	public void println(Object o) {
    	print(o);
    	println();
	}
	
	public void println(String s) {
    	print(s + "\n");
	}
	
	public void print(boolean b) {
    	print(b ? "true" : "false");
	}
	
	public void print(char c) {
    	print("" + c);
	}
	
	public void print(char[] s) {
    	print(new String(s));
	}

	public void print(double d) {
    	print(Double.toString(d));
	}
	
	public void print(float f) {
    	print(Float.toString(f));
	}
	
	public void print(int i) {
    	print(Integer.toString(i));
	}
	
	public void print(long l) {
    	print(Long.toString(l));
	}
	
	public void print(Object o) {
    	print(o.toString());
	}
	
	public void write(byte[] buf, int off, int len) {
    	print(new String(buf, off, len));
	}

	public void write(byte b) {
    	write(new byte[] { b }, 0, 1);
	}

}
