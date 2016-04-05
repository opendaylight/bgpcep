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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public interface EvpnRegistry {
    /**
     * Parse EvpnChoice
     *
     * @param type Nlri Type
     * @param buffer encoded EvpnChoice body in Bytebuf
     * @return EvpnChoice
     */
    EvpnChoice parseEvpn(NlriType type, ByteBuf buffer);

    /**
     * Serialize EvpnChoice
     *
     * @param evpn EvpnChoice
     * @param common encoded common Evpn
     * @return encoded EvpnChoice body in Bytebuf
     */
    ByteBuf serializeEvpn(EvpnChoice evpn, ByteBuf common);

    /**
     * @param evpnChoice ChoiceNode containing EvpnChoice
     * @return EvpnChoice
     */
    EvpnChoice serializeEvpnModel(ChoiceNode evpnChoice);

    /**
     * @param evpnChoice ChoiceNode containing EvpnChoice
     * @return EvpnChoice
     */
    EvpnChoice serializeEvpnRouteKey(ChoiceNode evpnChoice);
}
