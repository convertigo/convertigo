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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.iJavelin;

public class Date extends JavelinExtractionRule {
	public static final long serialVersionUID = -6735666205939933877L;

	private static SimpleDateFormat day = new SimpleDateFormat("dd");

	private static SimpleDateFormat day_in_week_short = new SimpleDateFormat("EEE");

	private static SimpleDateFormat day_in_week_long = new SimpleDateFormat("EEEE");

	private static SimpleDateFormat month = new SimpleDateFormat("MM");

	private static SimpleDateFormat month_name_short = new SimpleDateFormat("MMM");

	private static SimpleDateFormat month_name_long = new SimpleDateFormat("MMMM");

	private static SimpleDateFormat week_in_year = new SimpleDateFormat("ww");

	private static SimpleDateFormat week_in_month = new SimpleDateFormat("W");

	private static SimpleDateFormat year = new SimpleDateFormat("yyyy");

	private static SimpleDateFormat am_pm_marker = new SimpleDateFormat("a");

	private static SimpleDateFormat hour_in_day_0_to_23 = new SimpleDateFormat("HH");

	private static SimpleDateFormat hour_in_day_1_to_24 = new SimpleDateFormat("kk");

	private static SimpleDateFormat hour_in_day_0_to_11 = new SimpleDateFormat("hh");

	private static SimpleDateFormat hour_in_day_1_to_12 = new SimpleDateFormat("KK");

	private static SimpleDateFormat minutes = new SimpleDateFormat("mm");

	private static SimpleDateFormat seconds = new SimpleDateFormat("ss");

	private static SimpleDateFormat milliseconds = new SimpleDateFormat("SSS");

	private static SimpleDateFormat time_zone_text = new SimpleDateFormat("z");

	private static SimpleDateFormat time_zone_number = new SimpleDateFormat("Z");

	/** Holds value of property format. */
	private String format = "dd/MM/yy";

	/** Holds value of property language. */
	private String language = Locale.getDefault().getLanguage();

	private boolean bday = true;

	private boolean bday_in_week_short = false;

	private boolean bday_in_week_long = false;

	private boolean bmonth = true;

	private boolean bmonth_name_short = false;

	private boolean bmonth_name_long = false;

	private boolean bweek_in_year = false;

	private boolean bweek_in_month = false;

	private boolean byear = true;

	private boolean bam_pm_marker = false;

	private boolean bhour_in_day_0_to_23 = true;

	private boolean bhour_in_day_1_to_24 = false;

	private boolean bhour_in_day_0_to_11 = false;

	private boolean bhour_in_day_1_to_12 = false;

	private boolean bminutes = true;

	private boolean bseconds = true;

	private boolean bmilliseconds = false;

	private boolean btime_zone_text = false;

	private boolean btime_zone_number = false;

	/** Creates new Date */
	public Date() {
		super();
	}
	
	public void init(int reason) {
		if (reason == ExtractionRule.INITIALIZING) {
			nDate = 1;
		}
	}

	/**
	 * Performs custom configuration for this database object.
	 */
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException(
					"Unable to find version number for the database object \""
							+ getName() + "\".\nXML data: " + s);
			throw ee;
		}

		if (VersionUtils.compare(version, "2.0.3") <= 0) {
			format = format.toLowerCase();
			format = format.replace('j', 'd');
			format = format.replace('m', 'M');
			format = format.replace('a', 'y');

			hasChanged = true;
			Engine.logBeans.warn("[Date] The object \"" + getName()
					+ "\" has been updated to version 2.0.4");
		}
	}

	/**
	 * Applies the extraction rule to the current iJavelin object.
	 * 
	 * @param javelin
	 *            the Javelin object.
	 * @param block
	 *            the current block to analyze.
	 * @param blockFactory
	 *            the block context of the current block.
	 * @param dom
	 *            the XML DOM.
	 * 
	 * @return an ExtractionRuleResult object containing the result of the
	 *         query.
	 */
	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block,
			BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();

		xrs = execute1(javelin, block, blockFactory, dom);
		if (xrs.hasMatched) return xrs;

		xrs = execute2(javelin, block, blockFactory, dom);
		return xrs;
	}

	transient private int nDate;
	
	public JavelinExtractionRuleResult execute1(iJavelin javelin, Block block,
			BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		
		Engine.logBeans.debug("Date: trying to find a date form according to the pattern '" + format + "'");

		if (!block.type.equals("field")) {
			Engine.logBeans.debug("Date: block is not field; aborting");
		}
		
		try {
			char separator1, separator2;
			char c;
			int i = 0;
			int fieldLen1 = 0, fieldLen2 = 0, fieldLen3 = 0;
			int len = format.length();
			String day = "", month = "", year = "";
			
			try {
				// First field
				c = format.charAt(i);
				if (c == 'd') day = "1";
				else if (c == 'm') month = "1";
				else if (c == 'y') year = "1";
				do {
					i++;
					fieldLen1++;
				} while ((separator1 = format.charAt(i)) == c);
				
				// Second field
				i++;
				c = format.charAt(i);
				if (c == 'd') day = "2";
				else if (c == 'm') month = "2";
				else if (c == 'y') year = "2";
				do {
					i++;
					fieldLen2++;	
				} while ((separator2 = format.charAt(i)) == c);
				
				// Third field
				c = format.charAt(i + 1);
				if (c == 'd') day = "3";
				else if (c == 'm') month = "3";
				else if (c == 'y') year = "3";
				fieldLen3 = len - i - 1;
				
			} catch (StringIndexOutOfBoundsException e) {
				Engine.logBeans.warn("Date: wrong format!");
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}
			
			Block block1, block2, block3;

			// Verify the first field
			block1 = block;
			int blockFieldLen1 = -1;
			if (block.getOptionalAttribute("size") != null)
				blockFieldLen1 = Integer.parseInt(block.getOptionalAttribute("size"));
			if (blockFieldLen1 != fieldLen1) {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}

			block = blockFactory.getNextBlock(block);
			if (separator1 == ' ') {
				// Two cases: separator block can be a space character or blank
				if (block.type.equals("field")) {
					// Nothing to do
				}
				else if (blockFactory.getNextBlock(block).getText().equals(" ")) {
					block = blockFactory.getNextBlock(block);
				}
				else {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			}
			else {
				if (block.getText().equals("" + separator1)) {
					block = blockFactory.getNextBlock(block);
				}
				else {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			}
			
			// Verify the second field
			block2 = block;
			if (!block.type.equals("field")) {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}

			int blockFieldLen2 = Integer.parseInt(block.getOptionalAttribute("size"));
			if (blockFieldLen2 != fieldLen2) {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}

			block = blockFactory.getNextBlock(block);
			if (separator2 == ' ') {
				// Two cases: separator block can be a space character or blank
				if (block.type.equals("field")) {
					// Nothing to do
				}
				else if (blockFactory.getNextBlock(block).getText().equals(" ")) {
					block = blockFactory.getNextBlock(block);
				}
				else {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			}
			else {
				if (block.getText().equals("" + separator2)) {
					block = blockFactory.getNextBlock(block);
				}
				else {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			}
			
			
			// Verify the third field
			block3 = block;
			if (!block.type.equals("field")) {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}

			int blockFieldLen3 = Integer.parseInt(block.getOptionalAttribute("size"));
			if (blockFieldLen3 != fieldLen3) {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}

			Block newBlock = new Block();
			newBlock.setText("");
			newBlock.type = "date";
			newBlock.name = StringUtils.normalize(getName()) + "_" + nDate;
			nDate++;
			newBlock.setOptionalAttribute("day", day);
			newBlock.setOptionalAttribute("month", month);
			newBlock.setOptionalAttribute("year", year);
			
			blockFactory.insertBlock(newBlock, block1);
			
			newBlock.addOptionalChildren(block1);
			newBlock.addOptionalChildren(block2);
			newBlock.addOptionalChildren(block3);

			blockFactory.removeBlock(block1);
			blockFactory.removeBlock(block2);
			blockFactory.removeBlock(block3);

			xrs.hasMatched = true;
			xrs.newCurrentBlock = newBlock;
			return xrs;

		} catch (Exception e) {
			Engine.logBeans.error("Date: exception while processing the rule", e);
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return xrs;
		}
	}
	
	public JavelinExtractionRuleResult execute2(iJavelin javelin, Block block,
			BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();

		String szBlockText = block.getText();
		SimpleDateFormat sdf;

		Engine.logBeans.debug("Date: trying to find a date block according to the pattern '" + format + "'");

		try {
			sdf = new SimpleDateFormat(format, new Locale(language, Locale
					.getDefault().getDisplayCountry()));
		} catch (IllegalArgumentException e) {
			Engine.logBeans.debug("Error: the format '" + format
					+ "' is not valid. Please see help for more information.");
			sdf = new SimpleDateFormat("dd/MM/yyyy");
		} catch (Exception e) {
			Engine.logBeans
					.debug("Warning: the language '"
							+ language
							+ "' is not defined in ISO 639 language codes or is not available. The default language '"
							+ Locale.getDefault().getLanguage()
							+ "' will be used.");
			sdf = new SimpleDateFormat(format);
		}

		java.util.Date myDate = sdf.parse(szBlockText, new ParsePosition(0));

		if (myDate == null) {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return xrs;
		}

		// check the elements are numeric only
		try {
			if (bday)
				block.setOptionalAttribute("day", day.format(myDate));
			if (bday_in_week_short)
				block.setOptionalAttribute("day_in_week_short",
						day_in_week_short.format(myDate));
			if (bday_in_week_long)
				block.setOptionalAttribute("day_in_week_long", day_in_week_long
						.format(myDate));
			if (bmonth)
				block.setOptionalAttribute("month", month.format(myDate));
			if (bmonth_name_short)
				block.setOptionalAttribute("month_name_short", month_name_short
						.format(myDate));
			if (bmonth_name_long)
				block.setOptionalAttribute("month_name_long", month_name_long
						.format(myDate));
			if (bweek_in_year)
				block.setOptionalAttribute("week_in_year", week_in_year
						.format(myDate));
			if (bweek_in_month)
				block.setOptionalAttribute("week_in_month", week_in_month
						.format(myDate));
			if (byear)
				block.setOptionalAttribute("year", year.format(myDate));

			if (bam_pm_marker)
				block.setOptionalAttribute("am_pm_marker", am_pm_marker
						.format(myDate));
			if (bhour_in_day_0_to_23)
				block.setOptionalAttribute("hour_in_day_0_to_23",
						hour_in_day_0_to_23.format(myDate));
			if (bhour_in_day_1_to_24)
				block.setOptionalAttribute("hour_in_day_1_to_24",
						hour_in_day_1_to_24.format(myDate));
			if (bhour_in_day_0_to_11)
				block.setOptionalAttribute("hour_in_day_0_to_11",
						hour_in_day_0_to_11.format(myDate));
			if (bhour_in_day_1_to_12)
				block.setOptionalAttribute("hour_in_day_1_to_12",
						hour_in_day_1_to_12.format(myDate));
			if (bminutes)
				block.setOptionalAttribute("minutes", minutes.format(myDate));
			if (bseconds)
				block.setOptionalAttribute("seconds", seconds.format(myDate));
			if (bmilliseconds)
				block.setOptionalAttribute("milliseconds", milliseconds
						.format(myDate));
			if (btime_zone_text)
				block.setOptionalAttribute("time_zone_text", time_zone_text
						.format(myDate));
			if (btime_zone_number)
				block.setOptionalAttribute("time_zone_number", time_zone_number
						.format(myDate));

			block.setOptionalAttribute("pattern", format);

			if (block.type.equalsIgnoreCase("field"))
				block.type = "date";

			xrs.hasMatched = true;
			xrs.newCurrentBlock = block;
			return (xrs);

		} catch (Exception e) {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return (xrs);
		}
	}
	
	/**
	 * Getter for property format.
	 * 
	 * @return Value of property format.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Setter for property format.
	 * 
	 * @param format
	 *            New value of property format.
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Getter for property language.
	 * 
	 * @return Value of property language.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Setter for property language.
	 * 
	 * @param language
	 *            New value of property language.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Getter for property bday.
	 * 
	 * @return Value of property bday.
	 */
	public boolean getBday() {
		return bday;
	}

	/**
	 * Setter for property bday.
	 * 
	 * @param value
	 *            New value of property bday.
	 */
	public void setBday(boolean value) {
		this.bday = value;
	}

	/**
	 * Getter for property bday_in_week_short.
	 * 
	 * @return Value of property bday_in_week_short.
	 */
	public boolean getBday_in_week_short() {
		return bday_in_week_short;
	}

	/**
	 * Setter for property bday_in_week_short.
	 * 
	 * @param value
	 *            New value of property bday_in_week_short.
	 */
	public void setBday_in_week_short(boolean value) {
		this.bday_in_week_short = value;
	}

	/**
	 * Getter for property bday_in_week_long.
	 * 
	 * @return Value of property bday_in_week_long.
	 */
	public boolean getBday_in_week_long() {
		return bday_in_week_long;
	}

	/**
	 * Setter for property bday_in_week_long.
	 * 
	 * @param value
	 *            New value of property bday_in_week_long.
	 */
	public void setBday_in_week_long(boolean value) {
		this.bday_in_week_long = value;
	}

	/**
	 * Getter for property bmonth.
	 * 
	 * @return Value of property bmonth.
	 */
	public boolean getBmonth() {
		return bmonth;
	}

	/**
	 * Setter for property bmonth.
	 * 
	 * @param value
	 *            New value of property bmonth.
	 */
	public void setBmonth(boolean value) {
		this.bmonth = value;
	}

	/**
	 * Getter for property bmonth_name_short.
	 * 
	 * @return Value of property bmonth_name_short.
	 */
	public boolean getBmonth_name_short() {
		return bmonth_name_short;
	}

	/**
	 * Setter for property bmonth_name_short.
	 * 
	 * @param value
	 *            New value of property bmonth_name_short.
	 */
	public void setBmonth_name_short(boolean value) {
		this.bmonth_name_short = value;
	}

	/**
	 * Getter for property bmonth_name_long.
	 * 
	 * @return Value of property bmonth_name_long.
	 */
	public boolean getBmonth_name_long() {
		return bmonth_name_long;
	}

	/**
	 * Setter for property bmonth_name_long.
	 * 
	 * @param value
	 *            New value of property bmonth_name_long.
	 */
	public void setBmonth_name_long(boolean value) {
		this.bmonth_name_long = value;
	}

	/**
	 * Getter for property bweek_in_year.
	 * 
	 * @return Value of property bweek_in_year.
	 */
	public boolean getBweek_in_year() {
		return bweek_in_year;
	}

	/**
	 * Setter for property bweek_in_year.
	 * 
	 * @param value
	 *            New value of property bweek_in_year.
	 */
	public void setBweek_in_year(boolean value) {
		this.bweek_in_year = value;
	}

	/**
	 * Getter for property bweek_in_month.
	 * 
	 * @return Value of property bweek_in_month.
	 */
	public boolean getBweek_in_month() {
		return bweek_in_month;
	}

	/**
	 * Setter for property bweek_in_month.
	 * 
	 * @param value
	 *            New value of property bweek_in_month.
	 */
	public void setBweek_in_month(boolean value) {
		this.bweek_in_month = value;
	}

	/**
	 * Getter for property byear.
	 * 
	 * @return Value of property byear.
	 */
	public boolean getByear() {
		return byear;
	}

	/**
	 * Setter for property byear.
	 * 
	 * @param value
	 *            New value of property byear.
	 */
	public void setByear(boolean value) {
		this.byear = value;
	}

	/**
	 * Getter for property bam_pm_marker.
	 * 
	 * @return Value of property bam_pm_marker.
	 */
	public boolean getBam_pm_marker() {
		return bam_pm_marker;
	}

	/**
	 * Setter for property bam_pm_marker.
	 * 
	 * @param value
	 *            New value of property bam_pm_marker.
	 */
	public void setBam_pm_marker(boolean value) {
		this.bam_pm_marker = value;
	}

	/**
	 * Getter for property bhour_in_day_0_to_23.
	 * 
	 * @return Value of property bhour_in_day_0_to_23.
	 */
	public boolean getBhour_in_day_0_to_23() {
		return bhour_in_day_0_to_23;
	}

	/**
	 * Setter for property bhour_in_day_0_to_23.
	 * 
	 * @param value
	 *            New value of property bhour_in_day_0_to_23.
	 */
	public void setBhour_in_day_0_to_23(boolean value) {
		this.bhour_in_day_0_to_23 = value;
	}

	/**
	 * Getter for property bhour_in_day_1_to_24.
	 * 
	 * @return Value of property bhour_in_day_1_to_24.
	 */
	public boolean getBhour_in_day_1_to_24() {
		return bhour_in_day_1_to_24;
	}

	/**
	 * Setter for property bhour_in_day_1_to_24.
	 * 
	 * @param value
	 *            New value of property bhour_in_day_1_to_24.
	 */
	public void setBhour_in_day_1_to_24(boolean value) {
		this.bhour_in_day_1_to_24 = value;
	}

	/**
	 * Getter for property bhour_in_day_0_to_11.
	 * 
	 * @return Value of property bhour_in_day_0_to_11.
	 */
	public boolean getBhour_in_day_0_to_11() {
		return bhour_in_day_0_to_11;
	}

	/**
	 * Setter for property bhour_in_day_0_to_11.
	 * 
	 * @param value
	 *            New value of property bhour_in_day_0_to_11.
	 */
	public void setBhour_in_day_0_to_11(boolean value) {
		this.bhour_in_day_0_to_11 = value;
	}

	/**
	 * Getter for property bhour_in_day_1_to_12.
	 * 
	 * @return Value of property bhour_in_day_1_to_12.
	 */
	public boolean getBhour_in_day_1_to_12() {
		return bhour_in_day_1_to_12;
	}

	/**
	 * Setter for property bhour_in_day_1_to_12.
	 * 
	 * @param value
	 *            New value of property bhour_in_day_1_to_12.
	 */
	public void setBhour_in_day_1_to_12(boolean value) {
		this.bhour_in_day_1_to_12 = value;
	}

	/**
	 * Getter for property bminutes.
	 * 
	 * @return Value of property bminutes.
	 */
	public boolean getBminutes() {
		return bminutes;
	}

	/**
	 * Setter for property bminutes.
	 * 
	 * @param value
	 *            New value of property bminutes.
	 */
	public void setBminutes(boolean value) {
		this.bminutes = value;
	}

	/**
	 * Getter for property bseconds.
	 * 
	 * @return Value of property bseconds.
	 */
	public boolean getBseconds() {
		return bseconds;
	}

	/**
	 * Setter for property bseconds.
	 * 
	 * @param value
	 *            New value of property bseconds.
	 */
	public void setBseconds(boolean value) {
		this.bseconds = value;
	}

	/**
	 * Getter for property bmilliseconds.
	 * 
	 * @return Value of property bmilliseconds.
	 */
	public boolean getBmilliseconds() {
		return bmilliseconds;
	}

	/**
	 * Setter for property bmilliseconds.
	 * 
	 * @param value
	 *            New value of property bmilliseconds.
	 */
	public void setBmilliseconds(boolean value) {
		this.bmilliseconds = value;
	}

	/**
	 * Getter for property btime_zone_text.
	 * 
	 * @return Value of property btime_zone_text.
	 */
	public boolean getBtime_zone_text() {
		return btime_zone_text;
	}

	/**
	 * Setter for property btime_zone_text.
	 * 
	 * @param value
	 *            New value of property btime_zone_text.
	 */
	public void setBtime_zone_text(boolean value) {
		this.btime_zone_text = value;
	}

	/**
	 * Getter for property btime_zone_number.
	 * 
	 * @return Value of property btime_zone_number.
	 */
	public boolean getBtime_zone_number() {
		return btime_zone_number;
	}

	/**
	 * Setter for property btime_zone_number.
	 * 
	 * @param value
	 *            New value of property btime_zone_number.
	 */
	public void setBtime_zone_number(boolean value) {
		this.btime_zone_number = value;
	}
}
