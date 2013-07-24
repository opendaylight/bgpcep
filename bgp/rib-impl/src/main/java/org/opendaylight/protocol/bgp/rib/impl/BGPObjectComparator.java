/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.protocol.bgp.parser.AbstractBGPObjectState;


/**
 * This comparator is intended to implement BGP Best Path Selection algorithm, as described at
 * 
 * @see http://www.cisco.com/en/US/tech/tk365/technologies_tech_note09186a0080094431.shtml
 * 
 * @param <T> Actual object state reference
 */
final class BGPObjectComparator<T extends AbstractBGPObjectState<?>> implements Comparator<T> {
	@Override
	public int compare(final T o1, final T o2) {
		if (o1 == o2)
			return 0;
		if (o1 == null)
			return 1;
		if (o2 == null)
			return -1;

		// FIXME: look at ASPath
		// FIXME: look at everything else :-)

		return 0;
	}
}
