/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.Opaque;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OpaqueUtil {
    static final short GENERIC_LSP_IDENTIFIER = 1;
    static final short EXTENDED_TYPE = 255;
    private static final Logger LOG = LoggerFactory.getLogger(OpaqueUtil.class);
    private static final String SEPARATOR = ":";
    private static final String EMPTY_SEPARATOR = "";

    private OpaqueUtil() {
        throw new UnsupportedOperationException();
    }

    static boolean serializeOpaque(final Opaque opaque, final ByteBuf byteBuf) {
        final Short type = opaque.getOpaqueType();
        switch (type) {
            case GENERIC_LSP_IDENTIFIER:
                ByteBufWriteUtil.writeUnsignedByte(type, byteBuf);
                writeGeneric(opaque.getOpaque(), byteBuf);
                break;
            case EXTENDED_TYPE:
                ByteBufWriteUtil.writeUnsignedByte(type, byteBuf);
                writeExtended(opaque.getOpaque(), opaque.getOpaqueExtendedType(), byteBuf);
                break;
            default:
                LOG.debug("Skipping serialization of Opaque Value {}", opaque);
                return false;
        }
        return true;
    }

    private static void writeExtended(final HexString opaque, final Integer opaqueExtendedType, final ByteBuf byteBuf) {
        final byte[] output = writeOpaqueValue(opaque.getValue());
        ByteBufWriteUtil.writeUnsignedShort(opaqueExtendedType, byteBuf);
        ByteBufWriteUtil.writeUnsignedShort(output.length, byteBuf);
        byteBuf.writeBytes(output);
    }

    private static void writeGeneric(final HexString opaque, final ByteBuf byteBuf) {
        final byte[] output = writeOpaqueValue(opaque.getValue());
        ByteBufWriteUtil.writeUnsignedShort(output.length, byteBuf);
        byteBuf.writeBytes(output);
    }

    private static byte[] writeOpaqueValue(final String opaque) {
        final String joined = opaque.replace(SEPARATOR, EMPTY_SEPARATOR);
        return DatatypeConverter.parseHexBinary(joined);
    }

    static Opaque parseOpaque(final ByteBuf buffer) {
        final short type = buffer.readUnsignedByte();
        final OpaqueValueBuilder builder = new OpaqueValueBuilder();
        switch (type) {
            case GENERIC_LSP_IDENTIFIER:
                builder.setOpaque(buildOpaqueValue(buffer));
                break;
            case EXTENDED_TYPE:
                buildExtended(builder, buffer);
                break;
            default:
                final int length = buffer.readUnsignedShort();
                buffer.skipBytes(length);
                LOG.debug("Skipping parsing of Opaque Value {}", buffer);
                return null;
        }
        builder.setOpaqueType(type);
        return builder.build();
    }

    private static void buildExtended(final OpaqueValueBuilder builder, final ByteBuf buffer) {
        final int extendedType = buffer.readUnsignedShort();
        final HexString opaqueValue = buildOpaqueValue(buffer);
        builder.setOpaqueExtendedType(extendedType).setOpaque(opaqueValue);
    }

    private static HexString buildOpaqueValue(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        final byte[] value = ByteArray.readBytes(buffer, length);
        final String hexDump = ByteBufUtil.hexDump(value);
        final Iterable<String> splitted = Splitter.fixedLength(2).split(hexDump);
        return new HexString(Joiner.on(SEPARATOR).join(splitted));
    }

    static List<OpaqueValue> parseOpaqueList(final ByteBuf byteBuf) {
        final List<OpaqueValue> opaqueValues = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final Opaque opaque = parseOpaque(byteBuf);
            if (opaque != null) {
                opaqueValues.add((OpaqueValue) opaque);
            }
        }
        return opaqueValues;
    }

    static boolean serializeOpaqueList(final List<OpaqueValue> mldpP2mpLsp, final ByteBuf buffer) {
        boolean parsed = false;
        for (final OpaqueValue opaque : mldpP2mpLsp) {
            if (serializeOpaque(opaque, buffer)) {
                parsed = true;
            }
        }
        return parsed;
    }
}
