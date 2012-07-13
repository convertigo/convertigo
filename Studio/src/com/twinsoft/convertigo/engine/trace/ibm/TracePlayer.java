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

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.Log;

public class TracePlayer {
	protected Log log = null;

	public TracePlayer() {
		OutputStream os = System.out;
		
		try {
			Class<?> plugin = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			os = (OutputStream) plugin.getDeclaredField("traceConsoleStream").get(plugin.getMethod("getDefault"));
		} catch (Exception e) {}
		log = new Log(os);
		log.logLevel = Log.LOGLEVEL_MESSAGE;
	}

	public void closeSocket() {
		try {
			serverSocket.close();
		} catch (Exception _ex) {
		}
		serverSocket = null;
	}

	public void stop() {
		if (sessions != null) {
			int i = sessions.size();
			for (int j = 0; j < i; j++)
				try {
					SessionPlayer sessionplayer = (SessionPlayer) sessions
							.elementAt(j);
					sessionplayer.close();
					SessionPlayer sessionplayer1 = sessionplayer;
					sessionplayer1.interrupt();
				} catch (Exception _ex) {
				}

		}
	}

	public void runTrace(String file, int port) {
		sessions = new Vector<SessionPlayer>();
		String s;
		int i;

		s = file;
		i = port;

		log.message("Twinsoft IBM Trace Player");
		log.message("File: " + s + " on port " + i);

		try {
			serverSocket = new ServerSocket(i);
		} catch (IOException _ex) {
			log.exception(_ex, "Failed to create ServerSocket on port " + i + "!");
			return;
		}
		log.message("Server playing trace " + s + " on port " + i + ".");
		do {
			Socket socket = null;
			sp = null;
			try {
				socket = serverSocket.accept();
			} catch (IOException _ex) {
				break;
			}
			try {
				sp = new SessionPlayer(socket, s);
				sessions.addElement(sp);
			} catch (Exception _ex) {
				try {
					socket.close();
				} catch (IOException _ex2) {
				}
				break;
			}
			sp.start();
		} while (Engine.theApp != null && Engine.isStarted);
		try {
			serverSocket.close();
			return;
		} catch (Exception _ex) {
			return;
		}
	}

	private ServerSocket serverSocket;

	private SessionPlayer sp;

	private Vector<SessionPlayer> sessions;
}