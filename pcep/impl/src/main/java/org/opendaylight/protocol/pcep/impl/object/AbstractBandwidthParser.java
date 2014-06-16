/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;

abstract class AbstractBandwidthParser extends AbstractObjectWithTlvsParser<BandwidthBuilder> {

    private static final int BANDWIDTH_F_LENGTH = 4;

    AbstractBandwidthParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Bandwidth parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() != BANDWIDTH_F_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: "
                    + BANDWIDTH_F_LENGTH + ".");
        }
        final BandwidthBuilder builder = new BandwidthBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        builder.setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(ByteArray.getAllBytes(bytes)));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Bandwidth, String.format("Wrong instance of PCEPObject. Passed %s . Needed BandwidthObject.", object.getClass()));
        final ByteBuf body = Unpooled.buffer();
        body.writeBytes(((Bandwidth) object).getBandwidth().getValue());
        formatBandwidth(object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    protected abstract void formatBandwidth(final boolean processed, final boolean ignored, final ByteBuf body, final ByteBuf buffer);
}
