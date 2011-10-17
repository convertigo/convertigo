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

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.MouseEvent;

public class MouseAdvanceStatement extends MouseStatement {
	private static final long serialVersionUID = -8671654909869364944L;

	protected int screenX = -1;
	protected int screenY = -1;
	protected int clientX = -1;
	protected int clientY = -1;
	protected boolean ctrlKey = false;
	protected boolean altKey = false;
	protected boolean shiftKey = false;
	protected boolean metKey = false;
	protected short button = 0;
	
	public MouseAdvanceStatement() {
		super();
	}

	public MouseAdvanceStatement(String xpath) {
		super(xpath);
	}

	public MouseAdvanceStatement(String action, String xpath) {
		super(action, xpath);
	}

	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) {
		return new MouseEvent(xpath, action, screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metKey, button);
	}
	
	public String toString(){
		return 	(ctrlKey?" + ctrl":"")+
				(altKey?" + alt":"")+
				(shiftKey?" + shift":"")+
				(metKey?" + meta":"")+
				(screenX!=-1 || screenY!=-1?" + screen("+screenX+","+screenY+")":"")+
				(clientX!=-1 || clientY!=-1?" + client("+clientX+","+clientY+")":"")+
				" button("+button+")"+
				super.toString();
	}
	
	public int getClientX() {
		return clientX;
	}

	public void setClientX(int clientX) {
		this.clientX = clientX;
	}

	public int getClientY() {
		return clientY;
	}

	public void setClientY(int clientY) {
		this.clientY = clientY;
	}

	public int getScreenX() {
		return screenX;
	}

	public void setScreenX(int screenX) {
		this.screenX = screenX;
	}

	public int getScreenY() {
		return screenY;
	}

	public void setScreenY(int screenY) {
		this.screenY = screenY;
	}

	public boolean getAltKey() {
		return altKey;
	}

	public void setAltKey(boolean altKey) {
		this.altKey = altKey;
	}

	public short getButton() {
		return button;
	}

	public void setButton(short button) {
		this.button = button;
	}

	public boolean getCtrlKey() {
		return ctrlKey;
	}

	public void setCtrlKey(boolean ctrlKey) {
		this.ctrlKey = ctrlKey;
	}

	public boolean getMetKey() {
		return metKey;
	}

	public void setMetKey(boolean metKey) {
		this.metKey = metKey;
	}

	public boolean getShiftKey() {
		return shiftKey;
	}

	public void setShiftKey(boolean shiftKey) {
		this.shiftKey = shiftKey;
	}
}
