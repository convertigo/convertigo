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

package com.twinsoft.convertigo.beans.common;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

public class DataType extends JavelinExtractionRule {
    
	private static final long serialVersionUID = -1076337113088827299L;

	public static final int DETECTION_POLICY_DEFAULT = 0;
	public static final int DETECTION_POLICY_AUTOMATIC = 1;
	public static final int DETECTION_POLICY_EXPLICIT = 2;
	
	private int detectionPolicy = DataType.DETECTION_POLICY_DEFAULT;
	
	public int getDetectionPolicy() {
		return detectionPolicy;
	}
	
	public void setDetectionPolicy(int detectionPolicy) {
		this.detectionPolicy = detectionPolicy;
	}
	
	private String explicitDataType = "";
	
	public String getExplicitDataType() {
		return explicitDataType;
	}
	
	public void setExplicitDataType(String explicitDataType) {
		this.explicitDataType = explicitDataType;
	}
	
    public DataType() {
        super();
    }
    	
    /**
     * Applies the extraction rule to the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		Engine.logBeans.trace("Analyzing the data type of block: " + block);

		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		xrs.newCurrentBlock = block;
		
		if (block.type.equals("static") || block.type.equals("field")) {
			xrs.hasMatched = true;

			if (detectionPolicy == DataType.DETECTION_POLICY_EXPLICIT) {
				block.setOptionalAttribute("datatype", explicitDataType);
				Engine.logBeans.trace("The block has been explicitly data typed to '" + explicitDataType + "'");
				return xrs;
			}
			else {
				String blockText = block.getText();
		        
				// boolean?
				String s = blockText.toLowerCase();
				if (s.equals("true") || s.equals("false")) {
					Engine.logBeans.trace("The block is a boolean.");
					block.setOptionalAttribute("dataType", "boolean");
					return xrs;
				}
				Engine.logBeans.trace("The block is not an boolean.");

				// integer number?
				try {
					long l = Math.abs(Long.parseLong(blockText));
					if (detectionPolicy == DataType.DETECTION_POLICY_DEFAULT) {
						Engine.logBeans.trace("The block seems to be an integer number; defaulting to int.");
						block.setOptionalAttribute("datatype", "int");
					}
					else {
						if (l < Byte.MAX_VALUE) {
							Engine.logBeans.trace("The block is a byte.");
							block.setOptionalAttribute("datatype", "byte");
						}
						else if (l < Short.MAX_VALUE) {
							Engine.logBeans.trace("The block is a short.");
							block.setOptionalAttribute("datatype", "short");
						}
						else if (l < Integer.MAX_VALUE) {
							Engine.logBeans.trace("The block is an integer.");
							block.setOptionalAttribute("datatype", "int");
						}
						else {
							Engine.logBeans.trace("The block is a long.");
							block.setOptionalAttribute("datatype", "long");
						}
					}
					return xrs;
				}
				catch (NumberFormatException e) {
					Engine.logBeans.trace("The block is not an integer number.");
				}

				// floating point number?
				try {
					double d = Math.abs(Double.parseDouble(blockText));
					if (detectionPolicy == DataType.DETECTION_POLICY_DEFAULT) {
						Engine.logBeans.trace("The block seems to be a floating point number; defaulting to float.");
						block.setOptionalAttribute("dataType", "float");
					}
					else {
						if (d < Float.MAX_VALUE) {
							Engine.logBeans.trace("The block is a float.");
							block.setOptionalAttribute("datatype", "float");
						}
						else {
							Engine.logBeans.trace("The block is a double.");
							block.setOptionalAttribute("datatype", "double");
						}
					}
					return xrs;
				}
				catch (NumberFormatException e) {
					Engine.logBeans.trace("The block is not a floating point number.");
				}

				// TODO: gérer le type date

				// Default type
				block.setOptionalAttribute("datatype", "string");
				Engine.logBeans.trace("The block is a string.");
				return xrs;
			}
		}

		xrs.hasMatched = false;
		return xrs;
    }
}
