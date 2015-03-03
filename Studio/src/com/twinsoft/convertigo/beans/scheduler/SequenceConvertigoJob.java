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

package com.twinsoft.convertigo.beans.scheduler;

import java.util.List;
import java.util.Map;

import com.twinsoft.convertigo.engine.enums.Parameter;

public class SequenceConvertigoJob extends AbstractConvertigoJob {
	public static final String prob_noSequence = "no sequence";
	
	private String sequenceName = null;
	
	public String getSequenceName() {
		return sequenceName;
	}
	public void setSequenceName(String sequenceNameName) {
		this.sequenceName = sequenceNameName;
	}
		
	@Override
	protected void addParameters(Map<String, String[]> parameters) {
		if (sequenceName != null && sequenceName.length() > 0) {
			parameters.put(Parameter.Sequence.getName(), new String[]{sequenceName});
		}
		super.addParameters(parameters);
	}
	
	@Override
	public void checkProblems(List<String> problems){
		super.checkProblems(problems);
		if(sequenceName==null || sequenceName.length()==0) problems.add(prob_noSequence);
	}
}
