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

package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.util.XMLUtils;

public class IdToXpathManager {
	protected Map<String, Node> idToNode = new HashMap<String, Node>();
	protected List<Object[]> transactionRange = new ArrayList<Object[]>();
	protected int cpt = 0;

	public String addNode(Node node) {
		String id = nextId();
		idToNode.put(id, node);
		return id;
	}

	public Node getNode(String id) {
		return (Node) idToNode.get(id);
	}

	public String getXPath(String id) {
		Node node = getNode(id);
		return (node == null) ? null : XMLUtils.calcXpath(node, null);
	}

	public void setTransaction(String transaction) {
		transactionRange.add(new Object[] { new Integer(cpt), transaction });
	}

	public String getTransaction(String id) {
		Integer iid = Integer.decode(id);
		for (int j = transactionRange.size() - 1; j >= 0; j--) {
			Object[] datas = (Object[]) transactionRange.get(j);
			if (iid.compareTo((Integer) datas[0]) >= 0)
				return (String) datas[1];
		}
		return null;
	}

	protected String nextId() {
		return "" + (cpt++);
	}

	/*
	 * static public void main(String[]args){ IdToXpathManager it = new
	 * IdToXpathManager(); it.setTransaction("A"); for(int
	 * i=0;i<10;i++)it.nextId(); it.setTransaction("B"); for(int
	 * i=0;i<10;i++)it.nextId(); System.out.println(it.getTransaction("0"));
	 * System.out.println(it.getTransaction("1"));
	 * System.out.println(it.getTransaction("10"));
	 * System.out.println(it.getTransaction("11")); }
	 */
}
