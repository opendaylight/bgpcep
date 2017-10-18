/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.spi;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public interface EvpnRegistry {
    /**
     * Decode input buffer to BGP Evpn.
     *
     * @param type Nlri Type
     * @param buffer encoded EvpnChoice body in Bytebuf
     * @return EvpnChoice
     */
    EvpnChoice parseEvpn(@Nonnull NlriType type, @Nonnull ByteBuf buffer);

    /**
     * Encode input BGP Evpn to output buffer
     *
     * @param evpn EvpnChoice
     * @param common encoded common Evpn
     * @return encoded EvpnChoice body in Bytebuf
     */
    ByteBuf serializeEvpn(@Nonnull EvpnChoice evpn, @Nonnull ByteBuf common);

    /**
     * Decode Evpn Model to Evpn.
     *
     * @param evpnChoice ChoiceNode containing Evpn
     * @return EvpnChoice
     */
    EvpnChoice serializeEvpnModel(@Nonnull ChoiceNode evpnChoice);

    /**
     * Create Route key from Evpn model
     *
     * @param evpnChoice ChoiceNode containing Evpn
     * @return EvpnChoice
     */
    EvpnChoice serializeEvpnRouteKey(@Nonnull ChoiceNode evpnChoice);
}
