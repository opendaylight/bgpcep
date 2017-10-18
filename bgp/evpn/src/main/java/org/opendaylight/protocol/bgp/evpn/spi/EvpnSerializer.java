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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public interface EvpnSerializer {
    /**
     * @param evpn Evpn
     * @param buffer Encode common Evpn parts to output buffer
     * @return Encode Evpn to output buffer
     */
    ByteBuf serializeEvpn(@Nonnull EvpnChoice evpn, @Nonnull ByteBuf buffer);

    /**
     * Serialize Evpn
     *
     * @param evpn ChoiceNode containing Evpn
     * @return Evpn
     */
    EvpnChoice serializeEvpnModel(@Nonnull ContainerNode evpn);

    /**
     * create Route key from Evpn model
     *
     * @param evpn ContainerNode containing Evpn
     * @return Evpn
     */
    EvpnChoice createRouteKey(@Nonnull ContainerNode evpn);
}
