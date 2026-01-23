/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.events;

public class ProgressEvent implements BaseEvent {
	private String name;
	private String status;
	private Object mutex = new Object();
	
	public ProgressEvent(String name, String status) {
		this.name = name;
		this.status = status;
	}

	public String getName() {
		return name;
	}
	
	public String getStatus() {
		return status;
	}
	
	public ProgressEvent setStatus(String status) {
		synchronized (mutex) {
			this.status = status;
			mutex.notifyAll();
		}
		return this;
	}

	public boolean waitNextStatus() {
		synchronized (mutex) {
			try {
				status = null;
				mutex.wait(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return status != null;
		}
	}
}
