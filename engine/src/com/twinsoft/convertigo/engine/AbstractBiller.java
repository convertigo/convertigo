/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.twinsoft.convertigo.engine.util.PropertiesUtils;

public abstract class AbstractBiller {
	public abstract void insertBilling(Context context) throws EngineException;
	
	protected Properties loadFromFile(Context context) throws FileNotFoundException, IOException {
		String propertiesFileName = context.getProjectDirectory() + "/biller.properties";
		Properties properties = PropertiesUtils.load(propertiesFileName);
		return properties;
	}
		
}
