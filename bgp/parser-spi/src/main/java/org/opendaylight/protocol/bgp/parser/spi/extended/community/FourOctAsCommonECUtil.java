/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.generic.spec.common.ec.As4GenericSpecExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.generic.spec.common.ec.As4GenericSpecExtendedCommunityBuilder;

public final class FourOctAsCommonECUtil {
    private FourOctAsCommonECUtil() {
        throw new UnsupportedOperationException();
    }

    public static As4GenericSpecExtendedCommunity parseCommon(final ByteBuf body) {
        return new As4GenericSpecExtendedCommunityBuilder().setAsNumber(new AsNumber(body.readUnsignedInt()))
            .setLocalAdministrator(body.readUnsignedShort()).build();
    }

    public static void serializeCommon(final As4GenericSpecExtendedCommunity extComm, final ByteBuf body) {
        body.writeInt(extComm.getAsNumber().getValue().intValue());
        ByteBufWriteUtil.writeUnsignedShort(extComm.getLocalAdministrator(), body);
    }
}
