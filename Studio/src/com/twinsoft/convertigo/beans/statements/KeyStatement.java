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
import com.twinsoft.convertigo.engine.parsers.events.KeyEvent;
import com.twinsoft.convertigo.engine.parsers.events.SimpleEvent;

public class KeyStatement extends SimpleEventStatement {

	private static final long serialVersionUID = -8307732995064245341L;

	protected int keyCode = 0;
	protected int charCode = 0;
	protected boolean ctrlKey = false;
	protected boolean altKey = false;
	protected boolean shiftKey = false;
	protected boolean metKey = false;
	
	public KeyStatement() {
		this(SimpleEvent.action_keypress, "");
	}
	
	public KeyStatement(String xpath) {
		this(SimpleEvent.action_keypress, xpath);
	}

	public KeyStatement(String action, String xpath) {
		super(action, xpath);
	}
	
	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) {
		return new KeyEvent(xpath, action, keyCode, charCode, ctrlKey, altKey, shiftKey, metKey);
	}
	
	@Override
	public String toString() {
		return action +
				(ctrlKey ? " + ctrl":"") +
				(altKey ? " + alt":"") +
				(shiftKey ? " + shift":"") +
				(metKey ? " + meta":"") +
				(keyCode != 0 ? " keyCode(" + keyCode + ")":"") +
				(charCode != 0 ? " charCode(" + charCode + ")":"") +
				super.toString();
	}

	public String[] getActionStrings() {
		return SimpleEvent.getKeyboardActions();
	}

	public boolean getAltKey() {
		return altKey;
	}

	public void setAltKey(boolean altKey) {
		this.altKey = altKey;
	}

	public int getCharCode() {
		return charCode;
	}

	public void setCharCode(int charCode) {
		this.charCode = charCode;
	}

	public boolean getCtrlKey() {
		return ctrlKey;
	}

	public void setCtrlKey(boolean ctrlKey) {
		this.ctrlKey = ctrlKey;
	}

	public int getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode;
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