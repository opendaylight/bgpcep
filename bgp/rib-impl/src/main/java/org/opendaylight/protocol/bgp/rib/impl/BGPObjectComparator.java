/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * This comparator is intended to implement BGP Best Path Selection algorithm, as described at
 * 
 * @see http://www.cisco.com/en/US/tech/tk365/technologies_tech_note09186a0080094431.shtml
 * 
 * @param <T> Actual object state reference
 */
final class BGPObjectComparator implements Comparator<PathAttributes> {
	public static final BGPObjectComparator INSTANCE = new BGPObjectComparator();

	private BGPObjectComparator() {
	}

	@Override
	public int compare(final PathAttributes o1, final PathAttributes o2) {
		if (o1 == null) {
			return 1;
		}
		if (o2 == null) {
			return -1;
		}
		if (o1.equals(o2)) {
			return 0;
		}

		// FIXME: BUG-185: implement here

		return 0;
	}
}
