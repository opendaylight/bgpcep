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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;

public interface EvpnParser {
    /**
     * Decode input buffer to BGP Evpn.
     *
     * @param buffer encoded Evpn body in Bytebuf
     * @return Evpn
     */
    @Nonnull
    EvpnChoice parseEvpn(@Nonnull ByteBuf buffer);
}
