/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MoveStep extends CopyStep {

	private static final long serialVersionUID = 2273726638385880897L;

	@Override
	protected String getAction() {
		return "move";
	}

	@Override
	protected void doActionForSourceFile(File sourceFile, File destinationFile) throws IOException {
		FileUtils.moveFile(sourceFile, destinationFile);
	}

	@Override
	protected void doActionForSourceDirectory(File sourceFile, File destinationFile) throws IOException {
		FileUtils.moveDirectory(sourceFile, destinationFile);
	}
}
