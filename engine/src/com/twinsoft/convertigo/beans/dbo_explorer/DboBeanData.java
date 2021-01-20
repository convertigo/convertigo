/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.dbo_explorer;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;

import com.twinsoft.convertigo.beans.BeansUtils;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DboBeanData {

	private String htmlDescription = null;
	private boolean selectedByDefault;
	private BeanInfo beanInfo;

	public DboBeanData(BeanInfo beanInfo) {
		this.beanInfo = beanInfo;
		selectedByDefault = false;
	}

	public String getDisplayName() {
		return beanInfo.getBeanDescriptor().getDisplayName();
	}

	public String getClassname() {
		return getBeanClass().getName();
	}

	public String getHtmlDescription() {
		if (htmlDescription == null) {
			BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
			String beanDescription = beanDescriptor.getShortDescription();

			String[] beanDescriptions = beanDescription.split("\\|");

			String beanShortDescription = beanDescriptions.length >= 1 ? beanDescriptions[0] : "n/a";
			String beanLongDescription = beanDescriptions.length >= 2 ? beanDescriptions[1] : "n/a";
			beanShortDescription = BeansUtils.cleanDescription(beanShortDescription, true);
			beanLongDescription = BeansUtils.cleanDescription(beanLongDescription, true);

			htmlDescription = "<p>" +
					"<font size=\"4.5\"><u><b>" + getDisplayName() + "</b></u></font>" + "<br><br>"	+
					"<i>" + beanShortDescription + "</i>" + "<br><br>" + beanLongDescription +
			   "</p>";
		}

		return htmlDescription;
	}

	public boolean isSelectedByDefault() {
		return selectedByDefault;
	}

	public void setSelectedByDefault(boolean selectedByDefault) {
		this.selectedByDefault = selectedByDefault;
	}

	public Class<DatabaseObject> getBeanClass() {
		return GenericUtils.cast(beanInfo.getBeanDescriptor().getBeanClass());
	}

}
