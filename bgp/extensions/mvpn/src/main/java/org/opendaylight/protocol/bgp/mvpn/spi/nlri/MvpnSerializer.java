/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.nlri;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;

public interface MvpnSerializer<T extends MvpnChoice> {
    /**
     * Serialize mvpn.
     *
     * @param mvpn   mvpn
     * @return Encode mvpn to output buffer
     */
    @NonNull ByteBuf serializeMvpn(@NonNull T mvpn);

    /**
     * returns class of MvpnChoice handled by serializer.
     *
     * @return MvpnChoice Class
     */
    Class<? extends MvpnChoice> getClazz();
}
