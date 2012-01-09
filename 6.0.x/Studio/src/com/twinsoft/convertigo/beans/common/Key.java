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

import java.io.Serializable;

/**
 * This class represents an entity used to be exported to a XML block.
 */
public class Key implements Serializable
{
    public static final long serialVersionUID = -9149048950121769551L;
    
	/**
	 * The keyword label (i.e. appearing on screen).
	 */
	private String text;
	
	/**
	 * The keyword data (i.e. datas to be sent when keyword executed).
	 */
	private String data;

	/**
	 * The keyword action (i.e. action to be done to send the datas).
	 */
	private String action;
	
	public void setText(String text)
	{
		if (text == null)
			this.text = "";
		else
			this.text = text;
	}
	
	public String getText()
	{
		return text;
	}

	public void setData(String data)
	{
		if (data == null)
			this.data = "";
		else
			this.data = data;
	}

	public String getData()
	{
		return data;
	}

	public void setAction(String action)
	{
		if (action == null)
			this.action = "";
		else
			this.action = action;
	}
	
	public String getAction()
	{
		return action;
	}

	/**
	 * Returns true if key equal. 
	 * 
	 * @param key the key to be compared. To be used in comparison, fields must not be null.
	 * 
	 */
	public boolean equals(Key key)
	{
		if (key != null) {
			boolean flag = true;
	
			if (key.text != null)
				if ((flag = key.text.equals(text)) == false)
					return flag;
		
			if (key.data != null)
				if ((flag = key.data.equals(data)) == false)
					return flag;

			if (key.action != null)
				if ((flag = key.action.equals(action)) == false)
					return flag;
		
			return flag;
		}
		
		return false;
	}
	
	public Key()
	{
		setText("");
		setData("");
		setAction("");
	}
	
	public Key(String text, String data, String action)
	{
		setText(text);
		setData(data);
		setAction(action);
	}
}
