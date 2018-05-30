/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import java.util.List;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.attribute.conditions.AsPathLength;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpOriginAttrType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeComparison;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeEq;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeGe;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeLe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.EmptyNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCase;

/**
 * Bgp Attribute Conditions Util per check conditions matchs.
 *
 * @author Claudio D. Gasparini
 */
final class BgpAttributeConditionsUtil {
    private BgpAttributeConditionsUtil() {
        throw new UnsupportedOperationException();
    }

    static boolean matchConditions(
            final Class<? extends AfiSafiType> afiSafi,
            final Attributes attributes,
            final BgpConditions conditions) {
        if (!matchAfiSafi(afiSafi, conditions.getAfiSafiIn())) {
            return false;
        }

        if (!matchAsPathLength(attributes.getAsPath(), conditions.getAsPathLength())) {
            return false;
        }

        if (!matchMED(attributes.getMultiExitDisc(), conditions.getMedEq())) {
            return false;
        }

        if (!matchOrigin(attributes.getOrigin(), conditions.getOriginEq())) {
            return false;
        }

        if (!matchNextHopIn(attributes.getCNextHop(), conditions.getNextHopIn())) {
            return false;
        }

        if (!matchLocalPref(attributes.getLocalPref(), conditions.getLocalPrefEq())) {
            return false;
        }
        return true;
    }

    private static boolean matchAfiSafi(
            final Class<? extends AfiSafiType> afiSafi,
            final List<Class<? extends AfiSafiType>> afiSafiIn) {
        if (afiSafiIn == null) {
            return true;
        }
        return afiSafiIn.contains(afiSafi);
    }

    private static boolean matchMED(final MultiExitDisc multiExitDisc, final Long med) {
        if (multiExitDisc == null || med == null) {
            return true;
        }

        return multiExitDisc.getMed().equals(med);
    }

    private static boolean matchOrigin(final Origin origin, final BgpOriginAttrType originEq) {
        if (origin == null || originEq == null) {
            return true;
        }
        return origin.getValue().getIntValue() == originEq.getIntValue();
    }

    private static boolean matchAsPathLength(final AsPath asPath, final AsPathLength asPathLength) {
        if (asPath == null || asPathLength == null) {
            return true;
        }

        final List<Segments> segments = asPath.getSegments();
        int total = segments.stream().map(AsPathSegment::getAsSequence)
                .filter(Objects::nonNull).mapToInt(List::size).sum();

        if (total == 0) {
            total = segments.stream().map(AsPathSegment::getAsSet)
                    .filter(Objects::nonNull).mapToInt(List::size).sum();
        }

        final Class<? extends AttributeComparison> comp = asPathLength.getOperator();
        final long asPathLenght = asPathLength.getValue();
        if (comp == AttributeEq.class) {
            return total == asPathLenght;
        } else if (comp == AttributeGe.class) {
            return total >= asPathLenght;
        } else if (comp == AttributeLe.class) {
            return total <= asPathLenght;
        }
        return false;
    }


    private static boolean matchNextHopIn(final CNextHop nextHop, final List<IpAddress> nextHopIn) {
        if (nextHop == null || nextHopIn == null || nextHop instanceof EmptyNextHopCase) {
            return true;
        }

        IpAddress global;
        if (nextHop instanceof Ipv4NextHopCase) {
            global = new IpAddress(((Ipv4NextHopCase) nextHop).getIpv4NextHop().getGlobal());
        } else {
            global = new IpAddress(((Ipv6NextHopCase) nextHop).getIpv6NextHop().getGlobal());
        }
        return nextHopIn.contains(global);
    }

    private static boolean matchLocalPref(final LocalPref localPref, final Long localPrefEq) {
        if (localPref == null || localPrefEq == null) {
            return true;
        }
        return localPref.getPref().equals(localPrefEq);
    }
}
