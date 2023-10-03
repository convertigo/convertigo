package com.twinsoft.convertigo.engine.admin.services.studio;

import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.FolderType;

public class Utils {
	public static final Pattern parseQName = Pattern.compile("(.*?)(?::(\\w+?))?");

	public static DatabaseObject getDbo(String id) throws Exception {
		var reg = parseQName.matcher(id);
		reg.matches();
		var ft = FolderType.parse(reg.group(2));
		var qname = ft == null ? id : reg.group(1);
		return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
	}

}
