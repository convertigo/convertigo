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

package com.twinsoft.convertigo.eclipse.trace;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.trace.ibm.TracePlayer;

/**
 * @author davidm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TracePlayerThread implements Runnable {

	String traceFile;
	public String connectorName;
	TracePlayer mtp = new TracePlayer();
	
	public TracePlayerThread(String threadName, String connectorName, String traceFile) {
        this.connectorName = connectorName;
        this.traceFile = traceFile;
        Thread th = new Thread(this);
        th.setName(threadName);
        th.setDaemon(true);
        synchronized(mtp) {
        	th.start();
        	try {
				mtp.wait();
			} catch (InterruptedException e) {
				ConvertigoPlugin.logException(e, "(TracePlayerThread) : An error occured while mtp.wait() function");
			}
        }
    }
	
	public void stopPlayer() {
		mtp.closeSocket();
		mtp.stop();
		connectorName = null;
        traceFile = null;
	}
	
	public void run() {	
		try {
			synchronized(mtp) {
				mtp.notify();
			}
			mtp.runTrace(traceFile, ConvertigoPlugin.getTraceplayerPort());
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Exception in player thread while playing trace");
		}
	}
}
