/*
 * Copyright (c) 2025 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.auto.bandwidth.capability.tlv.AutoBandwidthCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.auto.bandwidth.capability.tlv.AutoBandwidthCapabilityBuilder;

public final class AutoBandwidthCapabilityParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 36;
    private static final int CONTENT_LENGTH = 4;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof AutoBandwidthCapability, "Auto-Bandwidth Capability is mandatory.");
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeZero(CONTENT_LENGTH);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        buffer.skipBytes(CONTENT_LENGTH);
        return new AutoBandwidthCapabilityBuilder().build();
    }
}
