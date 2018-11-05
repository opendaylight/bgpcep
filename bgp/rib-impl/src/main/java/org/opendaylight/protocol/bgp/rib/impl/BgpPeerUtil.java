/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;

public final class BgpPeerUtil {

    private BgpPeerUtil() {
        throw new UnsupportedOperationException();
    }

    public static Update createEndOfRib(final TablesKey key) {
        return key.getAfi() == Ipv4AddressFamily.class ? new UpdateBuilder().build() :
                new UpdateBuilder()
                        .setAttributes(new AttributesBuilder()
                                .addAugmentation(Attributes2.class, new Attributes2Builder()
                                        .setMpUnreachNlri(new MpUnreachNlriBuilder()
                                                .setAfi(key.getAfi())
                                                .setSafi(key.getSafi())
                                                .build()).build()).build()).build();
    }

    public static boolean isEndOfRib(final UpdateMessage msg) {
        if (msg.getNlri() == null && msg.getWithdrawnRoutes() == null) {
            if (msg.getAttributes() != null) {
                final Attributes2 pa = msg.getAttributes().augmentation(Attributes2.class);
                if (pa != null && msg.getAttributes().augmentation(Attributes1.class) == null) {
                    //only MP_UNREACH_NLRI allowed in EOR
                    if (pa.getMpUnreachNlri() != null && pa.getMpUnreachNlri().getWithdrawnRoutes() == null) {
                        // EOR message contains only MPUnreach attribute and no NLRI
                        return true;
                    }
                }
            } else {
                // true for empty IPv4 Unicast
                return true;
            }
        }
        return false;
    }

}
