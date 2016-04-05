/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public interface EvpnRegistry {
    Evpn parseEvpn(NlriType type, ByteBuf buffer);

    void serializeEvpn(Evpn evpn, ByteBuf buffer);

    Evpn serializeEvpnModel(Class<? extends Evpn> esRouteClass, ChoiceNode containerNode);
}
