/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.CSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;

/**
 * This comparator is intended to implement BGP Best Path Selection algorithm, as described at
 * 
 * @see http://www.cisco.com/en/US/tech/tk365/technologies_tech_note09186a0080094431.shtml
 * 
 * @param <T> Actual object state reference
 */
final class BGPObjectComparator implements Comparator<PathAttributes> {

	public static BGPObjectComparator INSTANCE = new BGPObjectComparator();

	public BGPObjectComparator() {
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
		// if (!o1.getAsPath().equals(o2.getAsPath())) {
		// final int i1 = 0;
		// if (Segments s : o1.getAsPath().getSegments()) {
		//
		// }
		// }

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
			return (o1.getMultiExitDisc().getMed().compareTo(o2.getMultiExitDisc().getMed())) * (-1);
		}

		// 7. prefer eBGP over iBGP paths
		// - if ASes are equal, the paths are iBGP
		AsNumber o1as = null;
		final CSegment o1cs = o1.getAsPath().getSegments().get(o1.getAsPath().getSegments().size() - 1).getCSegment();
		if (o1cs instanceof AListCase) {
			final AsSequence seq = ((AListCase) o1cs).getAList().getAsSequence().get(
					((AListCase) o1cs).getAList().getAsSequence().size() - 1);
			o1as = seq.getAs();
		}
		AsNumber o2as = null;
		final CSegment o2cs = o2.getAsPath().getSegments().get(o2.getAsPath().getSegments().size() - 1).getCSegment();
		if (o2cs instanceof AListCase) {
			final AsSequence seq = ((AListCase) o2cs).getAList().getAsSequence().get(
					((AListCase) o2cs).getAList().getAsSequence().size() - 1);
			o2as = seq.getAs();
		}
		if (!o1as.equals(o2as)) {

		}
		return 0;
	}
}
