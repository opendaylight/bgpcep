/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.Esi;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

@NonNullByDefault
public interface EsiRegistry {
    /**
     * Parse Esi.
     *
     * @param buffer encoded ESI body in Bytebuf
     * @return Ethernet Segment Identifier
     */
    @Nullable Esi parseEsi(ByteBuf buffer);

    /**
     * Parse Esi Model.
     *
     * @param esi ChoiceNode containing ESI
     * @return Ethernet Segment Identifier
     */
    @Nullable Esi parseEsiModel(ChoiceNode esi);

    /**
     * Serialize Esi.
     *
     * @param esi    Ethernet Segment Identifier
     * @param buffer write in Bytebuf encoded ESI body
     */
    void serializeEsi(Esi esi, ByteBuf buffer);
}
