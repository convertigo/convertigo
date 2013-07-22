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

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class DateBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_bminutes = 0;
    private static final int PROPERTY_bday = 1;
    private static final int PROPERTY_format = 2;
    private static final int PROPERTY_bmilliseconds = 3;
    private static final int PROPERTY_bhour_in_day_1_to_12 = 4;
    private static final int PROPERTY_bday_in_week_long = 5;
    private static final int PROPERTY_bhour_in_day_0_to_11 = 6;
    private static final int PROPERTY_bweek_in_month = 7;
    private static final int PROPERTY_bweek_in_year = 8;
    private static final int PROPERTY_btime_zone_number = 9;
    private static final int PROPERTY_byear = 10;
    private static final int PROPERTY_bmonth = 11;
    private static final int PROPERTY_bseconds = 12;
    private static final int PROPERTY_language = 13;
    private static final int PROPERTY_bhour_in_day_1_to_24 = 14;
    private static final int PROPERTY_bhour_in_day_0_to_23 = 15;
    private static final int PROPERTY_btime_zone_text = 16;
    private static final int PROPERTY_bmonth_name_long = 17;
    private static final int PROPERTY_bam_pm_marker = 18;
    private static final int PROPERTY_bmonth_name_short = 19;
    private static final int PROPERTY_bday_in_week_short = 20;

    public DateBeanInfo() {
		try {
			beanClass =  Date.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/date_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/date_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Date");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[21];
			
            properties[PROPERTY_bminutes] = new PropertyDescriptor ( "bminutes", Date.class, "getBminutes", "setBminutes" );
            properties[PROPERTY_bminutes].setDisplayName ( getExternalizedString("property.bminutes.display_name") );
            properties[PROPERTY_bminutes].setShortDescription ( getExternalizedString("property.bminutes.short_description") );
            
            properties[PROPERTY_bday] = new PropertyDescriptor ( "bday", Date.class, "getBday", "setBday" );
            properties[PROPERTY_bday].setDisplayName ( getExternalizedString("property.bday.display_name") );
            properties[PROPERTY_bday].setShortDescription ( getExternalizedString("property.bday.short_description") );
            
            properties[PROPERTY_format] = new PropertyDescriptor ( "format", Date.class, "getFormat", "setFormat" );
            properties[PROPERTY_format].setDisplayName ( getExternalizedString("property.format.display_name") );
            properties[PROPERTY_format].setShortDescription ( getExternalizedString("property.format.short_description") );
            
            properties[PROPERTY_bmilliseconds] = new PropertyDescriptor ( "bmilliseconds", Date.class, "getBmilliseconds", "setBmilliseconds" );
            properties[PROPERTY_bmilliseconds].setDisplayName ( getExternalizedString("property.bmilliseconds.display_name") );
            properties[PROPERTY_bmilliseconds].setShortDescription ( getExternalizedString("property.bmilliseconds.short_description") );
            
            properties[PROPERTY_bhour_in_day_1_to_12] = new PropertyDescriptor ( "bhour_in_day_1_to_12", Date.class, "getBhour_in_day_1_to_12", "setBhour_in_day_1_to_12" );
            properties[PROPERTY_bhour_in_day_1_to_12].setDisplayName ( getExternalizedString("property.bhour_in_day_1_to_12.display_name") );
            properties[PROPERTY_bhour_in_day_1_to_12].setShortDescription ( getExternalizedString("property.bhour_in_day_1_to_12.short_description") );
            
            properties[PROPERTY_bday_in_week_long] = new PropertyDescriptor ( "bday_in_week_long", Date.class, "getBday_in_week_long", "setBday_in_week_long" );
            properties[PROPERTY_bday_in_week_long].setDisplayName ( getExternalizedString("property.bday_in_week_long.display_name") );
            properties[PROPERTY_bday_in_week_long].setShortDescription ( getExternalizedString("property.bday_in_week_long.short_description") );
            
            properties[PROPERTY_bhour_in_day_0_to_11] = new PropertyDescriptor ( "bhour_in_day_0_to_11", Date.class, "getBhour_in_day_0_to_11", "setBhour_in_day_0_to_11" );
            properties[PROPERTY_bhour_in_day_0_to_11].setDisplayName ( getExternalizedString("property.bhour_in_day_0_to_11.display_name") );
            properties[PROPERTY_bhour_in_day_0_to_11].setShortDescription ( getExternalizedString("property.bhour_in_day_0_to_11.short_description") );
            
            properties[PROPERTY_bweek_in_month] = new PropertyDescriptor ( "bweek_in_month", Date.class, "getBweek_in_month", "setBweek_in_month" );
            properties[PROPERTY_bweek_in_month].setDisplayName ( getExternalizedString("property.bweek_in_month.display_name") );
            properties[PROPERTY_bweek_in_month].setShortDescription ( getExternalizedString("property.bweek_in_month.short_description") );
            
            properties[PROPERTY_bweek_in_year] = new PropertyDescriptor ( "bweek_in_year", Date.class, "getBweek_in_year", "setBweek_in_year" );
            properties[PROPERTY_bweek_in_year].setDisplayName ( getExternalizedString("property.bweek_in_year.display_name") );
            properties[PROPERTY_bweek_in_year].setShortDescription ( getExternalizedString("property.bweek_in_year.short_description") );
            
            properties[PROPERTY_btime_zone_number] = new PropertyDescriptor ( "btime_zone_number", Date.class, "getBtime_zone_number", "setBtime_zone_number" );
            properties[PROPERTY_btime_zone_number].setDisplayName ( getExternalizedString("property.btime_zone_number.display_name") );
            properties[PROPERTY_btime_zone_number].setShortDescription ( getExternalizedString("property.btime_zone_number.short_description") );
            
            properties[PROPERTY_byear] = new PropertyDescriptor ( "byear", Date.class, "getByear", "setByear" );
            properties[PROPERTY_byear].setDisplayName ( getExternalizedString("property.byear.display_name") );
            properties[PROPERTY_byear].setShortDescription ( getExternalizedString("property.byear.short_description") );
            
            properties[PROPERTY_bmonth] = new PropertyDescriptor ( "bmonth", Date.class, "getBmonth", "setBmonth" );
            properties[PROPERTY_bmonth].setDisplayName ( getExternalizedString("property.bmonth.display_name") );
            properties[PROPERTY_bmonth].setShortDescription ( getExternalizedString("property.bmonth.short_description") );
            
            properties[PROPERTY_bseconds] = new PropertyDescriptor ( "bseconds", Date.class, "getBseconds", "setBseconds" );
            properties[PROPERTY_bseconds].setDisplayName ( getExternalizedString("property.bseconds.display_name") );
            properties[PROPERTY_bseconds].setShortDescription ( getExternalizedString("property.bseconds.short_description") );
            
            properties[PROPERTY_language] = new PropertyDescriptor ( "language", Date.class, "getLanguage", "setLanguage" );
            properties[PROPERTY_language].setDisplayName ( getExternalizedString("property.language.display_name") );
            properties[PROPERTY_language].setShortDescription ( getExternalizedString("property.language.short_description") );
            
            properties[PROPERTY_bhour_in_day_1_to_24] = new PropertyDescriptor ( "bhour_in_day_1_to_24", Date.class, "getBhour_in_day_1_to_24", "setBhour_in_day_1_to_24" );
            properties[PROPERTY_bhour_in_day_1_to_24].setDisplayName ( getExternalizedString("property.bhour_in_day_1_to_24.display_name") );
            properties[PROPERTY_bhour_in_day_1_to_24].setShortDescription ( getExternalizedString("property.bhour_in_day_1_to_24.short_description") );
            
            properties[PROPERTY_bhour_in_day_0_to_23] = new PropertyDescriptor ( "bhour_in_day_0_to_23", Date.class, "getBhour_in_day_0_to_23", "setBhour_in_day_0_to_23" );
            properties[PROPERTY_bhour_in_day_0_to_23].setDisplayName ( getExternalizedString("property.bhour_in_day_0_to_23.display_name") );
            properties[PROPERTY_bhour_in_day_0_to_23].setShortDescription ( getExternalizedString("property.bhour_in_day_0_to_23.short_description") );
            
            properties[PROPERTY_btime_zone_text] = new PropertyDescriptor ( "btime_zone_text", Date.class, "getBtime_zone_text", "setBtime_zone_text" );
            properties[PROPERTY_btime_zone_text].setDisplayName ( getExternalizedString("property.btime_zone_text.display_name") );
            properties[PROPERTY_btime_zone_text].setShortDescription ( getExternalizedString("property.btime_zone_text.short_description") );
            
            properties[PROPERTY_bmonth_name_long] = new PropertyDescriptor ( "bmonth_name_long", Date.class, "getBmonth_name_long", "setBmonth_name_long" );
            properties[PROPERTY_bmonth_name_long].setDisplayName ( getExternalizedString("property.bmonth_name_long.display_name") );
            properties[PROPERTY_bmonth_name_long].setShortDescription ( getExternalizedString("property.bmonth_name_long.short_description") );
            
            properties[PROPERTY_bam_pm_marker] = new PropertyDescriptor ( "bam_pm_marker", Date.class, "getBam_pm_marker", "setBam_pm_marker" );
            properties[PROPERTY_bam_pm_marker].setDisplayName ( getExternalizedString("property.bam_pm_marker.display_name") );
            properties[PROPERTY_bam_pm_marker].setShortDescription ( getExternalizedString("property.bam_pm_marker.short_description") );
            
            properties[PROPERTY_bmonth_name_short] = new PropertyDescriptor ( "bmonth_name_short", Date.class, "getBmonth_name_short", "setBmonth_name_short" );
            properties[PROPERTY_bmonth_name_short].setDisplayName ( getExternalizedString("property.bmonth_name_short.display_name") );
            properties[PROPERTY_bmonth_name_short].setShortDescription ( getExternalizedString("property.bmonth_name_short.short_description") );
            
            properties[PROPERTY_bday_in_week_short] = new PropertyDescriptor ( "bday_in_week_short", Date.class, "getBday_in_week_short", "setBday_in_week_short" );
            properties[PROPERTY_bday_in_week_short].setDisplayName ( getExternalizedString("property.bday_in_week_short.display_name") );
            properties[PROPERTY_bday_in_week_short].setShortDescription ( getExternalizedString("property.bday_in_week_short.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
