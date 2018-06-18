/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.nlri;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;

public interface MvpnRegistry {
    /**
     * Decode input buffer to BGP Mvpn.
     *
     * @param type   Nlri Type
     * @param buffer encoded MvpnChoice body in Bytebuf
     * @return MvpnChoice
     */
    @Nullable
    MvpnChoice parseMvpn(@Nonnull NlriType type, @Nonnull ByteBuf buffer);

    /**
     * Encode input BGP mvpn to output buffer.
     *
     * @param mvpn   MvpnChoice
     * @return encoded MvpnChoice body in Bytebuf
     */
    @Nonnull
    ByteBuf serializeMvpn(@Nonnull MvpnChoice mvpn);
}
