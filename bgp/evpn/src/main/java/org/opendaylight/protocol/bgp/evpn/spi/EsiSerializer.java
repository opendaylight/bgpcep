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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public interface EsiSerializer {
    /**
     * Serialize Ethernet Segment Identifier.
     *
     * @param esi    Ethernet Segment Identifier
     * @param buffer write in Bytebuf encoded ESI body
     */
    void serializeEsi(@Nonnull Esi esi, @Nonnull ByteBuf buffer);

    /**
     * Serialize Ethernet Segment Identifier Model.
     *
     * @param esi Ethernet Segment Identifier Model
     * @return Ethernet Segment Identifier
     */
    @Nonnull
    Esi serializeEsi(@Nonnull ContainerNode esi);
}
