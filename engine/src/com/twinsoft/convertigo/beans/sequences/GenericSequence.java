/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.sequences;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;

public class GenericSequence extends Sequence {

	private static final long serialVersionUID = -3667919340113420742L;

	public GenericSequence() {
		super();
	}

	@Override
    public GenericSequence clone() throws CloneNotSupportedException {
    	GenericSequence clonedObject = (GenericSequence) super.clone();
    	return clonedObject;
    }
    
	public void runCore() throws EngineException {
		executeNextStep(runningThread.javascriptContext, scope);
	}
	
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_STEPS, 0);
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_CALLS, 0);
	}
	
	 

}
