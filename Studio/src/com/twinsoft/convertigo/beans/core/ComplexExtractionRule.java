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

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;

/**
 * This class defines the class for complex extraction rules,
 * i.e. extraction rules that handle themselves the navigation
 * thru the blocks list.
 */
public abstract class ComplexExtractionRule extends JavelinExtractionRule {
	private static final long serialVersionUID = -8275865088137348139L;

	/**
     * Constructs a new ComplexExtractionRule object.
     */
    public ComplexExtractionRule() {
        super();
    }
    
    /**
     * Tests if a block can be selected for being executing.
     *
     * @param block the block to be tested
     *
     * @return true if the block is in the zone, false otherwize.
     */
    @Override
    public boolean canBlockBeSelected(Block block) {
    	return ((!block.bFinal) && super.canBlockBeSelected(block));
    }
}
