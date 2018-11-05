/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
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
        final Update update;
        if (key.getAfi() == Ipv4AddressFamily.class) {
            update = new UpdateBuilder().build();
        } else {
            update = new UpdateBuilder()
                    .setAttributes(new AttributesBuilder()
                            .addAugmentation(Attributes2.class, new Attributes2Builder()
                                    .setMpUnreachNlri(new MpUnreachNlriBuilder()
                                            .setAfi(key.getAfi())
                                            .setSafi(key.getSafi())
                                            .build()).build()).build()).build();
        }

        return update;
    }
}
