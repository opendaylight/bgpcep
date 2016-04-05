/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import org.opendaylight.protocol.bgp.evpn.spi.EsiParser;
import org.opendaylight.protocol.bgp.evpn.spi.EsiSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

abstract class AbstractEsiType implements EsiParser, EsiSerializer {
    static final int ESI_TYPE_LENGTH = 10;
    static final int ZERO_BYTE = 1;
    static final int MAC_ADDRESS_LENGTH = 6;
    static final NodeIdentifier LD_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "local-discriminator").intern());

    protected final Long extractLD(final ContainerNode t3) {
        if (t3.getChild(LD_NID).isPresent()) {
            return (Long) t3.getChild(LD_NID).get().getValue();
        }
        return null;
    }
}
