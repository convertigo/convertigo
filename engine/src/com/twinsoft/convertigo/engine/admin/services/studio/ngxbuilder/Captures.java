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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Base64;

import javax.imageio.ImageIO;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * The pictures of an application, as the Capture Manager of the application editor of the Eclipse Studio
 * keeps them: its thumbnail, for the dashboard and the Marketplace, and three screens for the detail of
 * the Marketplace.
 * <ul>
 * <li>project</li>
 * <li>action: list (default), save (slot, data: the data URL of a picture), delete (slot)</li>
 * <li>slot: thumbnail, screen1, screen2 or screen3</li>
 * </ul>
 */
@ServiceDefinition(name = "Captures", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Captures extends JSonService {
	private static final String[] SLOTS = { "thumbnail", "screen1", "screen2", "screen3" };

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
		if (project == null) {
			throw new ServiceException("Unknown project " + request.getParameter("project"));
		}
		var action = request.getParameter("action");
		if ("save".equals(action) || "delete".equals(action)) {
			var slot = request.getParameter("slot");
			if (!Arrays.asList(SLOTS).contains(slot)) {
				throw new ServiceException("Unknown slot " + slot);
			}
			if ("save".equals(action)) {
				save(project, slot, request.getParameter("data"));
			} else {
				FileUtils.deleteQuietly(fileOf(project, slot));
			}
		}
		var captures = new JSONArray();
		for (var slot : SLOTS) {
			var file = fileOf(project, slot);
			var capture = new JSONObject().put("slot", slot).put("file", relative(project, file));
			if (file.exists()) {
				var type = file.getName().endsWith(".png") ? "png" : "jpeg";
				capture.put("data", "data:image/" + type + ";base64,"
						+ Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file)));
			}
			captures.put(capture);
		}
		response.put("captures", captures);
	}

	/**
	 * @return the picture of the slot: for the thumbnail, the one the dashboard shows
	 */
	private static File fileOf(Project project, String slot) {
		var dir = project.getDirFile();
		if (!"thumbnail".equals(slot)) {
			return new File(dir, "marketplace/" + slot + ".jpg");
		}
		for (var name : new String[] { "thumbnail.png", "thumbnail.jpg", "thumbnail.auto.jpg" }) {
			var file = new File(dir, name);
			if (file.exists()) {
				return file;
			}
		}
		return new File(dir, "thumbnail.jpg");
	}

	private static void save(Project project, String slot, String data) throws Exception {
		var comma = data == null ? -1 : data.indexOf(',');
		if (comma == -1 || !data.startsWith("data:image/")) {
			throw new ServiceException("The picture is missing.");
		}
		var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(data.substring(comma + 1))));
		if (image == null) {
			throw new ServiceException("The picture cannot be read.");
		}
		var file = fileOf(project, slot);
		if (file.getName().equals("thumbnail.auto.jpg")) {
			// a thumbnail chosen replaces the one captured automatically
			FileUtils.deleteQuietly(file);
			file = new File(project.getDirFile(), "thumbnail.jpg");
		}
		file.getParentFile().mkdirs();
		if (file.getName().endsWith(".png")) {
			ImageIO.write(image, "png", file);
		} else {
			// a JPEG has no alpha channel
			var rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			var graphics = rgb.createGraphics();
			graphics.drawImage(image, 0, 0, Color.WHITE, null);
			graphics.dispose();
			ImageIO.write(rgb, "jpg", file);
		}
	}

	private static String relative(Project project, File file) {
		return project.getDirFile().toPath().relativize(file.toPath()).toString().replace('\\', '/');
	}
}
