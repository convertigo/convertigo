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

import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.beans.common.ISiteClipperRequestCriteria;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.ProcessState;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.extractionrules.siteclipper.IRequestRule;
import com.twinsoft.convertigo.beans.extractionrules.siteclipper.IResponseRule;

public class SiteClipperScreenClass extends ScreenClass {

	private static final long serialVersionUID = 7407000532174142728L;

	public SiteClipperScreenClass() {
		super();
	}

	public List<IRequestRule> getRequestRules() {
		List<ExtractionRule> rules = getExtractionRules();
		List<IRequestRule> requestRules = new ArrayList<IRequestRule>(rules.size());
		for (ExtractionRule rule : rules) {
			if (rule instanceof IRequestRule) {
				requestRules.add((IRequestRule) rule);
			}
		}
		return requestRules;
	}
	
	public List<IResponseRule> getResponseRules() {
		List<ExtractionRule> rules = getExtractionRules();
		List<IResponseRule> responseRules = new ArrayList<IResponseRule>(rules.size());
		for (ExtractionRule rule : rules) {
			if (rule instanceof IResponseRule) {
				responseRules.add((IResponseRule) rule);
			}
		}
		return responseRules;
	}
	
	@Override
	public SiteClipperConnector getConnector() {
		return (SiteClipperConnector) super.getConnector();
	}
	
	@Override
	public int getNumberOfLocalCriterias() {
		// If we are in REQUEST mode, return the number of request criteria.
		Shuttle shuttle = getConnector().getShuttle();
		if (shuttle != null && shuttle.getProcessState() == ProcessState.request) {
			List<Criteria> localCriteria = getLocalCriterias();
			int nRequestLocalCriteria = 0;
			for (Criteria criteria : localCriteria) {
				if (criteria instanceof ISiteClipperRequestCriteria) nRequestLocalCriteria++;
			}
			return nRequestLocalCriteria;
		}
		
		// If we are in RESPONSE mode or if we are not currently in a request,
		// return the total number of local criteria.
		return super.getNumberOfLocalCriterias();
	}
}
