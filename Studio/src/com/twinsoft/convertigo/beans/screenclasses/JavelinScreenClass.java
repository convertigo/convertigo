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

package com.twinsoft.convertigo.beans.screenclasses;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.engine.EngineException;
/**
 * This class defines a screen class.
 */
public class JavelinScreenClass extends ScreenClass {

	private static final long serialVersionUID = 2909539434790792690L;

	/**
     * Constructs a new ScreenClass object.
     */
    public JavelinScreenClass() {
        super();
    }
    
    /**
     * The BlockFactory object to use by the ScreenClass.
     */
    transient public BlockFactory blockFactory = null;
    
    /**
     * Defines the automatic screen stable detection (i.e. when receiving
     * Javelin's DataStable event (default).
     */
    public static final int SCREEN_STABLE_DETECTION_CRITERIA_AUTOMATIC = 0;
    
    /**
     * Defines the screen stable detection over timeout.
     */
    public static final int SCREEN_STABLE_DETECTION_CRITERIA_TIMEOUT = 1;
    
    /**
     * Defines the screen stable detection over screen data.
     */
    public static final int SCREEN_STABLE_DETECTION_CRITERIA_DATA = 2;
    
    /**
     * Retrieves the block factory for this screen class.
     *
     * @return the block factory for this screen class.
     *
     * @exception EngineException if no block factory found.
     */
    public BlockFactory getBlockFactory() throws EngineException{
    	checkSubLoaded();
    	
        if (blockFactory == null) {
            if (parent instanceof JavelinScreenClass) {
                return ((JavelinScreenClass) parent).getBlockFactory();
            }
            else {
            	throw new EngineException("No block factory found!");
            }
        }

        return blockFactory;
    }
    
    /**
     * Set the blockfactory for the screen class.
     */
    public void setBlockFactory(BlockFactory blockFactory) {
        blockFactory.setParent(this);
        this.blockFactory = blockFactory;
    }
    
    public BlockFactory getLocalBlockFactory() {
    	checkSubLoaded();
    	
        return blockFactory;
    }
    
    public void add(DatabaseObject databaseObject) throws EngineException {
        super.add(databaseObject);
        if (databaseObject instanceof BlockFactory) {
            setBlockFactory((BlockFactory) databaseObject);
        }
        /*
        else {
            throw new EngineException("You cannot add to a screen class a database object of type " + databaseObject.getClass().getName());
        }
        */
    }

}
