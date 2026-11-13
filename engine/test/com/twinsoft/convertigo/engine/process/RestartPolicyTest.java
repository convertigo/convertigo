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

package com.twinsoft.convertigo.engine.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Duration;

import org.junit.Test;

public class RestartPolicyTest {

	@Test
	public void boundedWithAGrowingDelay() {
		RestartPolicy policy = RestartPolicy.bounded();
		assertEquals(Duration.ofSeconds(2), policy.onFailure());
		assertEquals(Duration.ofSeconds(4), policy.onFailure());
		assertNull(policy.onFailure());
		assertEquals(3, policy.getFailures());

		RestartPolicy capped = new RestartPolicy(10, Duration.ofSeconds(8), Duration.ofSeconds(30), Duration.ofSeconds(60));
		assertEquals(Duration.ofSeconds(8), capped.onFailure());
		assertEquals(Duration.ofSeconds(16), capped.onFailure());
		assertEquals(Duration.ofSeconds(30), capped.onFailure());
		assertEquals(Duration.ofSeconds(30), capped.onFailure());
	}

	@Test
	public void aStableRunResetsTheCount() {
		RestartPolicy policy = RestartPolicy.bounded();
		policy.onFailure();
		policy.onExit(Duration.ofSeconds(1));
		assertEquals(2, policy.getFailures());
		assertEquals(Duration.ofSeconds(2), policy.onExit(Duration.ofSeconds(61)));
		assertEquals(1, policy.getFailures());
	}
}
