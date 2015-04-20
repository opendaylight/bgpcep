/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link PathBinding}
 */
public class PathBindingTlvParser implements TlvParser, TlvSerializer {

    // TODO: to be confirmed by IANA
    public static final int TYPE = 31;

    private static final short MPLS_LABEL = 0;

    protected static final Set<Short> BINDING_TYPES = ImmutableSet.of(MPLS_LABEL);

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof PathBinding, "PathBinding is mandatory.");
        final PathBinding pTlv = (PathBinding) tlv;
        final Short bType = pTlv.getBindingType();
        Preconditions.checkArgument(BINDING_TYPES.contains(bType), "Unsupported Path Binding Type: %s", bType);
        final ByteBuf body = Unpooled.buffer();
        writeUnsignedByte(bType, body);
        body.writeBytes(pTlv.getBindingValue());
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final short type = buffer.readUnsignedByte();
        if (!BINDING_TYPES.contains(type)) {
            throw new PCEPDeserializerException("Unsupported Path Binding Type.");
        }
        final byte[] value = ByteArray.readAllBytes(buffer);
        return new PathBindingBuilder().setBindingType(type).setBindingValue(value).build();
    }
}
