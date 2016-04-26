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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MashupInformation implements Serializable {

	private static final long serialVersionUID = -5503096562853016022L;

	private Map<String, MashupDataViewConfiguration> dataviewConfigurations = new HashMap<String, MashupDataViewConfiguration>();
	private Map<String, MashupActionConfiguration> actionConfigurations = new HashMap<String, MashupActionConfiguration>();
	private Map<String, MashupEventConfiguration> eventConfigurations = new HashMap<String, MashupEventConfiguration>();
	
	public MashupInformation() {
		
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (dataviewConfigurations == null) dataviewConfigurations = new HashMap<String, MashupDataViewConfiguration>();
		if (actionConfigurations == null) actionConfigurations = new HashMap<String, MashupActionConfiguration>();
		if (eventConfigurations == null) eventConfigurations = new HashMap<String, MashupEventConfiguration>();
	}

	public void setDataViewConfiguration(String key, MashupDataViewConfiguration mdc) {
		dataviewConfigurations.put(key, mdc);
	}
	
	public MashupDataViewConfiguration getDataViewConfiguration(String key) {
		MashupDataViewConfiguration mdc = (MashupDataViewConfiguration)dataviewConfigurations.get(key);
		return mdc;
	}

	public void setActionConfiguration(String key, MashupActionConfiguration mac) {
		actionConfigurations.put(key, mac);
	}
	
	public MashupActionConfiguration getActionConfiguration(String key) {
		MashupActionConfiguration mac = (MashupActionConfiguration)actionConfigurations.get(key);
		return mac;
	}

	public void setEventConfiguration(String key, MashupEventConfiguration mec) {
		eventConfigurations.put(key, mec);
	}
	
	public MashupEventConfiguration getEventConfiguration(String key) {
		MashupEventConfiguration mec = (MashupEventConfiguration)eventConfigurations.get(key);
		return mec;
	}
}
