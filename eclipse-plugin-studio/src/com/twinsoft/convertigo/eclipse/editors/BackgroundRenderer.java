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

package com.twinsoft.convertigo.eclipse.editors;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;

/**
 * Renders in the background what the engine sends to an editor. The listeners of the engine run in the request, which
 * holds its context: they must not wait for the user interface, which may itself wait for that context (closing the
 * editor removes it). One rendering runs at a time, and the requests received meanwhile (a document generated at each
 * iteration of a loop) cost a single other rendering, of the last state.
 */
public class BackgroundRenderer {
	private final String name;
	private final Runnable render;
	private boolean requested = false;
	private boolean running = false;

	public BackgroundRenderer(String name, Runnable render) {
		this.name = name;
		this.render = render;
	}

	public void request() {
		synchronized (this) {
			requested = true;
			if (running) {
				return;
			}
			running = true;
		}
		try {
			Engine.execute(this::run);
		} catch (RuntimeException e) {
			// never fail the request of the engine for its rendering
			synchronized (this) {
				running = false;
			}
			if (Engine.logStudio != null) {
				Engine.logStudio.debug("(BackgroundRenderer) Failed to start the rendering of " + name, e);
			}
		}
	}

	private void run() {
		while (true) {
			synchronized (this) {
				if (!requested) {
					running = false;
					return;
				}
				requested = false;
			}
			try {
				render.run();
			} catch (Throwable t) {
				if (Engine.logStudio != null) {
					Engine.logStudio.debug("(BackgroundRenderer) Failed to render the output of " + name, t);
				}
			}
		}
	}

	/**
	 * A copy of a document of the engine, which goes on using it once its listeners return.
	 */
	public static Document copyOf(Document document) {
		if (document != null) {
			try {
				if (document.cloneNode(true) instanceof Document copy) {
					return copy;
				}
			} catch (Exception e) {
				// keep the document of the engine
			}
		}
		return document;
	}
}
