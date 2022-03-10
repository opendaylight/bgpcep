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
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.attribute.conditions.AsPathLength;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpOriginAttrType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeComparison;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeEq;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeGe;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeLe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.EmptyNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Bgp Attribute Conditions Util per check conditions matches.
 *
 * @author Claudio D. Gasparini
 */
final class BgpAttributeConditionsUtil {
    private BgpAttributeConditionsUtil() {
        // Hidden on purpose
    }

    static boolean matchConditions(
            final Class<? extends AfiSafiType> afiSafi,
            final Attributes attributes,
            final BgpConditions conditions) {
        return matchAfiSafi(afiSafi, conditions.getAfiSafiIn())
            && matchAsPathLength(attributes.getAsPath(), conditions.getAsPathLength())
            && matchMED(attributes.getMultiExitDisc(), conditions.getMedEq())
            && matchOrigin(attributes.getOrigin(), conditions.getOriginEq())
            && matchNextHopIn(attributes.getCNextHop(), conditions.getNextHopIn())
            && matchLocalPref(attributes.getLocalPref(), conditions.getLocalPrefEq());
    }

    private static boolean matchAfiSafi(final Class<? extends AfiSafiType> afiSafi,
            final Set<Class<? extends AfiSafiType>> afiSafiIn) {
        return afiSafiIn == null || afiSafiIn.contains(afiSafi);
    }

    private static boolean matchMED(final MultiExitDisc multiExitDisc, final Uint32 med) {
        return multiExitDisc == null || med == null || med.equals(multiExitDisc.getMed());
    }

    private static boolean matchOrigin(final Origin origin, final BgpOriginAttrType originEq) {
        return origin == null || originEq == null || origin.getValue().getIntValue() == originEq.getIntValue();
    }

    private static boolean matchAsPathLength(final AsPath asPath, final AsPathLength asPathLength) {
        if (asPath == null || asPathLength == null) {
            return true;
        }

        final List<Segments> segments = asPath.nonnullSegments();
        int total = segments.stream().map(AsPathSegment::getAsSequence)
                .filter(Objects::nonNull).mapToInt(List::size).sum();

        if (total == 0) {
            total = segments.stream().map(AsPathSegment::getAsSet)
                    .filter(Objects::nonNull).mapToInt(Set::size).sum();
        }

        final Class<? extends AttributeComparison> comp = asPathLength.getOperator();
        final long asLength = asPathLength.getValue().toJava();
        if (comp == AttributeEq.class) {
            return total == asLength;
        } else if (comp == AttributeGe.class) {
            return total >= asLength;
        } else if (comp == AttributeLe.class) {
            return total <= asLength;
        }
        return false;
    }

    private static boolean matchNextHopIn(final CNextHop nextHop, final Set<IpAddress> nextHopIn) {
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

    private static boolean matchLocalPref(final LocalPref localPref, final Uint32 localPrefEq) {
        return localPref == null || localPrefEq == null || localPrefEq.equals(localPref.getPref());
    }
}
