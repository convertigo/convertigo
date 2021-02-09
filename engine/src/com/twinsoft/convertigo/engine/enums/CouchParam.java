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

package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

public enum CouchParam {
	all_or_nothing,
	attname,
	attpath,
	attbase64,
	attcontent_type,
	bookmark,
	cancel,
	continuous,
	create_target,
	db,
	ddoc,
	destination,
	destination_rev,
	doc_ids,
	docid,
	execution_stats,
	fields,
	func,
	include_docs,
	json_base,
	key,
	limit,
	merge,
	name,
	new_edits,
	password,
	filter,
	proxy,
	rev,
	section,
	selector,
	skip,
	sort,
	source,
	stable,
	target,
	update,
	use_index,
	value,
	view;
	
	public static final String prefix = "_use_";
	
	public String param() {
		return prefix + name();
	}
	
	public void put(Map<String, String> query, String value) {
		query.put(name(), value);
	}
}
