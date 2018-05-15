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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;

public interface MvpnParser<T extends MvpnChoice> {
    /**
     * Decode input buffer to BGP Mvpn.
     *
     * @param buffer encoded Mvpn body in Bytebuf
     * @return Mvpn
     */
    @Nonnull
    T parseMvpn(@Nonnull ByteBuf buffer);

    /**
     * Returns NlriType handled by parser.
     *
     * @return NlriType
     */
    int getType();
}
