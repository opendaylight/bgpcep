/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;

/**
 * Parser for {@link SymbolicPathName}.
 */
public final class StatefulLspSymbolicNameTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 17;

    @Override
    public SymbolicPathName parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new SymbolicPathNameBuilder().setPathName(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.pcep.ietf.stateful.rev181109.SymbolicPathName(ByteArray.getAllBytes(buffer))).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof SymbolicPathName, "SymbolicPathNameTlv is mandatory.");
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
            .SymbolicPathName spn = ((SymbolicPathName) tlv).getPathName();
        checkArgument(spn != null, "SymbolicPathName is mandatory");
        TlvUtil.formatTlv(TYPE, Unpooled.copiedBuffer(spn.getValue()), buffer);
    }
}
