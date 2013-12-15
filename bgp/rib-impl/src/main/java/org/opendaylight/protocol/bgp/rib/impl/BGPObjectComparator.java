/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;

/**
 * This comparator is intended to implement BGP Best Path Selection algorithm, as described at
 * 
 * @see http://www.cisco.com/en/US/tech/tk365/technologies_tech_note09186a0080094431.shtml
 * 
 * @param <T> Actual object state reference
 */
final class BGPObjectComparator implements Comparator<PathAttributes> {

	private final AsNumber ourAS;

	public BGPObjectComparator(final AsNumber ourAs) {
		this.ourAS = ourAs;
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
		// 1. prefer path with accessible nexthop
		// - we assume that all nexthops are accessible

		// 2. prefer path with higher LOCAL_PREF
		if (!o1.getLocalPref().equals(o2.getLocalPref())) {
			return o1.getLocalPref().getPref().compareTo(o2.getLocalPref().getPref());
		}

		// 3. prefer learned path
		// - we assume that all paths are learned

		// 4. prefer the path with the shortest AS_PATH.
		if (!o1.getAsPath().equals(o2.getAsPath())) {
			final Integer i1 = countAsPath(o1.getAsPath().getSegments());
			final Integer i2 = countAsPath(o2.getAsPath().getSegments());
			return i2.compareTo(i1);
		}

		// 5. prefer the path with the lowest origin type
		// - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
		if (!o1.getOrigin().equals(o2.getOrigin())) {
			if (o1.getOrigin().getValue().equals(BgpOrigin.Igp)) {
				return 1;
			}
			if (o2.getOrigin().getValue().equals(BgpOrigin.Igp)) {
				return -1;
			}
			if (o1.getOrigin().getValue().equals(BgpOrigin.Egp)) {
				return 1;
			} else {
				return -1;
			}
		}

		// 6. prefer the path with the lowest multi-exit discriminator (MED)
		if (!o1.getMultiExitDisc().equals(o2.getMultiExitDisc())) {
			return o2.getMultiExitDisc().getMed().compareTo(o1.getMultiExitDisc().getMed());
		}

		// 7. prefer eBGP over iBGP paths
		// EBGP is peering between two different AS, whereas IBGP is between same AS (Autonomous System).
		final AsNumber first = getPeerAs(o1.getAsPath().getSegments());
		final AsNumber second = getPeerAs(o2.getAsPath().getSegments());
		if ((first == null && second == null) || (!first.equals(this.ourAS) && !second.equals(this.ourAS))
				|| (first.equals(this.ourAS) && second.equals(this.ourAS))) {
			return 0;
		}
		if (first == null || first.equals(this.ourAS)) {
			return -1;
		}
		if (second == null || second.equals(this.ourAS)) {
			return 1;
		}

		return 0;
	}

	private static int countAsPath(final List<Segments> segments) {
		// an AS_SET counts as 1, no matter how many ASs are in the set.
		int count = 0;
		boolean setPresent = false;
		for (final Segments s : segments) {
			if (s.getCSegment() instanceof ASetCase) {
				setPresent = true;
			} else {
				final AListCase list = (AListCase) s.getCSegment();
				count += list.getAList().getAsSequence().size();
			}
		}
		return (setPresent) ? ++count : count;
	}

	private static AsNumber getPeerAs(final List<Segments> segments) {
		if (segments.size() == 0) {
			return null;
		}
		final AListCase first = (AListCase) segments.get(0).getCSegment();
		return first.getAList().getAsSequence().get(0).getAs();
	}
}
