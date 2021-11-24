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

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.AutoRemoveFilePolicy;
import com.twinsoft.convertigo.engine.enums.FileExistPolicy;

public class DownloadHttpTransactionBeanInfo extends MySimpleBeanInfo {

	public DownloadHttpTransactionBeanInfo() {
		try {
			beanClass = DownloadHttpTransaction.class;
			additionalBeanClass = AbstractHttpTransaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/downloadhttptransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/downloadhttptransaction_color_32x32.png";

			properties = new PropertyDescriptor[4];

			resourceBundle = getResourceBundle("res/DownloadHttpTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties[0] = new PropertyDescriptor("filename", beanClass, "getFilename", "setFilename");
			properties[0].setDisplayName(getExternalizedString("property.filename.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.filename.short_description"));

			properties[1] = new PropertyDescriptor("folder", beanClass, "getFolder", "setFolder");
			properties[1].setDisplayName(getExternalizedString("property.folder.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.folder.short_description"));

			properties[2] = new PropertyDescriptor("fileExistPolicy", beanClass, "getFileExistPolicy", "setFileExistPolicy");
			properties[2].setDisplayName(getExternalizedString("property.fileExistPolicy.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.fileExistPolicy.short_description"));
			properties[2].setPropertyEditorClass(FileExistPolicy.class);

			properties[3] = new PropertyDescriptor("autoRemoveFilePolicy", beanClass, "getAutoRemoveFilePolicy", "setAutoRemoveFilePolicy");
			properties[3].setDisplayName(getExternalizedString("property.autoRemoveFilePolicy.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.autoRemoveFilePolicy.short_description"));
			properties[3].setPropertyEditorClass(AutoRemoveFilePolicy.class);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

