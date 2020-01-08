/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.dialogs;

import com.twinsoft.convertigo.engine.studio.WrapStudio;

public class MultipleDeletionDialog {

	private static final int ALWAYS_ASK = 0;
	private static final int YES_TO_ALL = 1;
	private static final int NO_TO_ALL = 2;
	private String[] buttons;
	private int confirmation = ALWAYS_ASK;
	private String title;
	private boolean hasMultiple;
	private WrapStudio shell;

	public MultipleDeletionDialog(WrapStudio shell, String title, boolean hasMultiple) {
		this.title = title;
		this.shell = shell;
		this.hasMultiple = hasMultiple;
		if (hasMultiple) {
			buttons = new String[] {
				"Yes",		 //	IDialogConstants.YES_LABEL,
				"Yes To All",//	IDialogConstants.YES_TO_ALL_LABEL,
				"No",		 //	IDialogConstants.NO_LABEL,
				"Cancel"	 //	IDialogConstants.CANCEL_LABEL
			};
		} else {
			buttons = new String[] { 
				"Yes",		// IDialogConstants.YES_LABEL,
				"No",		// IDialogConstants.NO_LABEL,
				"Cancel"	// IDialogConstants.CANCEL_LABEL
			};
		}	 
	}

	public boolean shouldBeDeleted(String message) {
		if (confirmation == YES_TO_ALL) {
			return true;
		} else {
			switch (confirmation) {
				case ALWAYS_ASK: {
					if (confirmOverwrite(message)) {
						return true;
					}
					break;
				}
				case YES_TO_ALL: {
					return true;
				}
				case NO_TO_ALL: {
					break;
				}
			}
			return false;
		}
	}

	private boolean confirmOverwrite(String msg) {
		if (shell == null) return false;
		int returnCode = shell.openMessageDialog(title, null, msg, "question", buttons, 0);
		if (hasMultiple) {
			switch (returnCode) {
				case 0:
					return true;
				case 1:
					confirmation = YES_TO_ALL; 
					return true;
				case 2:
					return false;
				case 3:
					confirmation = NO_TO_ALL;
				default:
					return false;
			}
		} else {
			switch (returnCode) {
			case 0:
				return true;
			case 1:
				return false;
			case 2:
			default:
				return false;
			}
		}
	}

	public void setTitle(String title) {
		this.title = title;
	}
}