/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;

public class NlriUtil {

    private NlriUtil() {

    }

    public static NlriType getNlriType(final CLinkstateDestination destination) {
        final ObjectType ot = destination.getObjectType();
        if (ot instanceof LinkCase) {
            return NlriType.Link;
        }
        if (ot instanceof NodeCase) {
            return NlriType.Node;
        }
        if (ot instanceof PrefixCase) {
            final IpPrefix prefix = ((PrefixCase)ot).getPrefixDescriptors().getIpReachabilityInformation();
            if (prefix.getIpv4Prefix() != null) {
                return NlriType.Ipv4Prefix;
            }
            if (prefix.getIpv6Prefix() != null) {
                return NlriType.Ipv6Prefix;
            }
        }
        return null;
    }
}
