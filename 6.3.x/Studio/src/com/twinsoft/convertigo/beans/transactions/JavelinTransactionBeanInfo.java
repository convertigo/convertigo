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

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;

public class JavelinTransactionBeanInfo extends MySimpleBeanInfo {
    
	public JavelinTransactionBeanInfo() {
		try {
			beanClass = JavelinTransaction.class;
			additionalBeanClass = TransactionWithVariables.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/javelintransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/javelintransaction_color_32x32.png";

			properties = new PropertyDescriptor[6];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/JavelinTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("dataStableThreshold", JavelinTransaction.class, "getDataStableThreshold", "setDataStableThreshold");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.dataStableThreshold.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.dataStableThreshold.short_description"));

			properties[1] = new PropertyDescriptor("timeoutForConnect", JavelinTransaction.class, "getTimeoutForConnect", "setTimeoutForConnect");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.timeoutForConnect.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.timeoutForConnect.short_description"));

			properties[2] = new PropertyDescriptor("onlyOnePage", JavelinTransaction.class, "isOnlyOnePage", "setOnlyOnePage");
			properties[2].setDisplayName(getExternalizedString("property.onlyOnePage.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.onlyOnePage.short_description"));
			properties[2].setExpert(true);

			properties[3] = new PropertyDescriptor("timeoutForDataStable", JavelinTransaction.class, "getTimeoutForDataStable", "setTimeoutForDataStable");
			properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.timeoutForDataStable.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.timeoutForDataStable.short_description"));
			
			properties[4] = new PropertyDescriptor("executeExtractionRulesInPanels", JavelinTransaction.class, "isExecuteExtractionRulesInPanels", "setExecuteExtractionRulesInPanels");
			properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.executeExtractionRulesInPanels.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.executeExtractionRulesInPanels.short_description"));
			
			properties[5] = new PropertyDescriptor("removeBlocksNode", JavelinTransaction.class, "isRemoveBlocksNode", "setRemoveBlocksNode");
			properties[5].setExpert(true);
			properties[5].setDisplayName(getExternalizedString("property.removeBlocksNode.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.removeBlocksNode.short_description"));

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

