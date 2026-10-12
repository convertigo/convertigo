/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.flow.FlowSourceLayout;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

/**
 * The deletion of an object of a project as the Studios do it, which the web Studio and the Flow MCP share:
 * what it removes too, then the deletion. The objects of a project leave it in memory and the save of the
 * project writes their removal, as the sources of a removed Flow, which an undo gives back before the save.
 */
public final class DboRemoval {
	private DboRemoval() {
	}

	/**
	 * @return what the deletion of the object removes too: the folders the user may choose, the database of
	 *         a FullSync connector, which may be dropped, and the sources of a Flow, always removed with it
	 */
	public static JSONObject check(DatabaseObject dbo) throws Exception {
		refuse(dbo);
		var response = new JSONObject();
		response.put("folders", new JSONArray(linkedFolders(dbo)));
		if (dbo instanceof CouchDbConnector couch && !couch.getDatabaseName().isEmpty()) {
			response.put("database", couch.getDatabaseName());
		}
		var sources = flowSources(dbo);
		if (!sources.isEmpty()) {
			response.put("sources", relativePaths(dbo.getProject(), sources));
		}
		return response;
	}

	/**
	 * Deletes the object; the folders chosen among those {@link #check} gives and, when asked, the database
	 * of a FullSync connector are removed too, at once.
	 */
	public static JSONObject remove(DatabaseObject dbo, Collection<String> folders, boolean dropDatabase) throws Exception {
		refuse(dbo);
		var response = new JSONObject();
		if (dbo instanceof FlowVirtualObject) {
			DboUtils.copyResult(FlowStudioSupport.removeNode(dbo), response);
			return response;
		}
		if (dbo instanceof Project) {
			return response.put("done", false).put("error", "A project is not deleted as one of its objects.");
		}
		var project = dbo.getProject();
		var flowEngine = project == null ? null : project.getFlowEngine();
		var parent = dbo.getParent();
		var linked = linkedFolders(dbo);
		var sources = flowSources(dbo);
		// as the Eclipse Studio deletes it: the tags of the object are checked before
		dbo.delete();
		if (!sources.isEmpty() && flowEngine != null) {
			// a working copy removal: the save of the project deletes the files, an undo gives them back
			flowEngine.applySourceChanges(Map.of(), sources);
		}
		for (var folder : folders) {
			if (linked.contains(folder)) {
				FileUtils.deleteQuietly(new File(folder));
			}
		}
		if (dropDatabase && dbo instanceof CouchDbConnector couch && !couch.getDatabaseName().isEmpty()) {
			couch.getCouchClient().deleteDatabase(couch.getDatabaseName());
		}
		// notify for app generation
		BuilderUtils.dboRemoved(parent, dbo);
		return response.put("done", true);
	}

	/**
	 * Refuses the deletions the Eclipse Studio refuses. The default connector and the default transaction
	 * can be deleted: a project or a connector may have none.
	 */
	static void refuse(DatabaseObject dbo) throws ServiceException {
		if (dbo instanceof com.twinsoft.convertigo.beans.core.ScreenClass && dbo.getParent() instanceof Project) {
			throw new ServiceException("Cannot delete the root screen class!");
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.steps.ThenStep || dbo instanceof com.twinsoft.convertigo.beans.steps.ElseStep) {
			throw new ServiceException("Cannot delete this step!");
		}
		if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent page && page.isRoot
				|| dbo instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent mobilePage && mobilePage.isRoot) {
			throw new ServiceException("Cannot delete the root page!");
		}
	}

	/**
	 * @return the folders linked to the object, which the deletion can remove too: the SOAP templates and
	 *         the traces of a connector, the resources of a mobile platform
	 */
	static List<String> linkedFolders(DatabaseObject dbo) {
		var folders = new ArrayList<String>();
		if (dbo instanceof Connector) {
			var projectDir = Engine.projectDir(dbo.getProject().getName());
			for (var kind : new String[] { "soap-templates", "Traces" }) {
				var folder = new File(projectDir + "/" + kind + "/" + dbo.getName());
				if (folder.exists()) {
					folders.add(folder.getAbsolutePath());
				}
			}
		} else if (dbo instanceof MobilePlatform platform && platform.getResourceFolder().exists()) {
			folders.add(platform.getResourceFolder().getAbsolutePath());
		}
		return folders;
	}

	/** @return the saved files of a Flow: its source and its learned schemas */
	private static List<String> flowSources(DatabaseObject dbo) throws Exception {
		var sources = new ArrayList<String>();
		if (!(dbo instanceof Flow flow) || flow.getProject() == null) {
			return sources;
		}
		var source = flow.getFlowSourceFile();
		if (source != null && source.isFile()) {
			sources.add(source.getCanonicalPath());
		}
		sources.addAll(schemaSources(flow.getProject(), flow.getName()));
		return sources;
	}

	/** @return the learned schemas of the Flow of this name */
	private static List<String> schemaSources(Project project, String flowName) throws Exception {
		var sources = new ArrayList<String>();
		var schemas = new File(new File(project.getDirFile(), FlowSourceLayout.current().root()), "schemas/" + flowName);
		if (schemas.isDirectory()) {
			for (var file : FileUtils.listFiles(schemas, null, true)) {
				if (FlowEngine.isWorkingCopySource(file.getName())) {
					sources.add(file.getCanonicalPath());
				}
			}
		}
		return sources;
	}

	/**
	 * @return the sources of a Flow the project does not have, as a Flow deleted by an older Studio left
	 *         them: its FlowScript, saved or a working copy, and its learned schemas; none when the project
	 *         has an object of this name or no FlowScript of it
	 */
	public static List<String> orphanFlowSources(Project project, String flowName) throws Exception {
		var sources = new ArrayList<String>();
		if (project == null || flowName == null || !FLOW_NAME.matcher(flowName).matches()) {
			return sources;
		}
		for (var sequence : project.getSequencesList()) {
			if (flowName.equals(sequence.getName())) {
				return sources;
			}
		}
		var source = new File(new File(project.getDirFile(), FlowSourceLayout.current().flows()), flowName + ".flow.js");
		var flowEngine = project.getFlowEngine();
		if (!source.isFile() && (flowEngine == null || !flowEngine.hasSource(source.getPath()))) {
			return sources;
		}
		var path = source.getCanonicalPath();
		// already removed, as a working copy the save of the project writes
		if (flowEngine != null && flowEngine.getSourceChanges().removals().contains(path)) {
			return sources;
		}
		sources.add(path);
		sources.addAll(schemaSources(project, flowName));
		return sources;
	}

	/**
	 * @return what removing the sources of a Flow the project does not have removes, as {@link #check} tells
	 *         it for an object, or null when the project has the Flow or no FlowScript of it
	 */
	public static JSONObject checkOrphanFlow(Project project, String flowName) throws Exception {
		var sources = orphanFlowSources(project, flowName);
		if (sources.isEmpty()) {
			return null;
		}
		return new JSONObject().put("folders", new JSONArray()).put("sources", relativePaths(project, sources));
	}

	/**
	 * Removes the sources of a Flow the project does not have, as the deletion of a Flow removes its own: a
	 * working copy removal, which the save of the project writes and an undo gives back.
	 */
	public static JSONObject removeOrphanFlow(Project project, String flowName) throws Exception {
		var response = new JSONObject();
		var sources = orphanFlowSources(project, flowName);
		var flowEngine = project == null ? null : project.getFlowEngine();
		if (sources.isEmpty() || flowEngine == null) {
			return response.put("done", false).put("error", "The project has no Flow source " + flowName + " without its Flow.");
		}
		flowEngine.applySourceChanges(Map.of(), sources);
		project.hasChanged = true;
		return response.put("done", true);
	}

	private static JSONArray relativePaths(Project project, List<String> sources) throws Exception {
		var root = project.getDirFile().getCanonicalPath();
		var paths = new JSONArray();
		for (var path : sources) {
			paths.put(path.substring(root.length() + 1).replace('\\', '/'));
		}
		return paths;
	}

	private static final java.util.regex.Pattern FLOW_NAME = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
}
