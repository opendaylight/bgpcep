/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.attributes;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.Opaque;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValueBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpaqueUtil {
    public static final short GENERIC_LSP_IDENTIFIER = 1;
    public static final short EXTENDED_TYPE = 255;
    private static final Logger LOG = LoggerFactory.getLogger(OpaqueUtil.class);

    private OpaqueUtil() {
        // Hidden on purpose
    }

    public static boolean serializeOpaque(final Opaque opaque, final ByteBuf byteBuf) {
        final Uint8 type = opaque.getOpaqueType();
        switch (type.toJava()) {
            case GENERIC_LSP_IDENTIFIER:
                ByteBufUtils.write(byteBuf, type);
                writeGeneric(opaque.getOpaque(), byteBuf);
                break;
            case EXTENDED_TYPE:
                ByteBufUtils.write(byteBuf, type);
                writeExtended(opaque.getOpaque(), opaque.getOpaqueExtendedType(), byteBuf);
                break;
            default:
                LOG.debug("Skipping serialization of Opaque Value {}", opaque);
                return false;
        }
        return true;
    }

    private static void writeExtended(final HexString opaque, final Uint16 opaqueExtendedType, final ByteBuf byteBuf) {
        final byte[] output = writeOpaqueValue(opaque);
        ByteBufUtils.writeOrZero(byteBuf, opaqueExtendedType);
        byteBuf.writeShort(output.length);
        byteBuf.writeBytes(output);
    }

    private static void writeGeneric(final HexString opaque, final ByteBuf byteBuf) {
        final byte[] output = writeOpaqueValue(opaque);
        byteBuf.writeShort(output.length);
        byteBuf.writeBytes(output);
    }

    private static byte[] writeOpaqueValue(final HexString opaque) {
        return IetfYangUtil.INSTANCE.hexStringBytes(opaque);
    }

    public static Opaque parseOpaque(final ByteBuf buffer) {
        final Uint8 type = ByteBufUtils.readUint8(buffer);
        final OpaqueValueBuilder builder = new OpaqueValueBuilder();
        switch (type.toJava()) {
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
        return builder.setOpaqueType(type).build();
    }

    private static void buildExtended(final OpaqueValueBuilder builder, final ByteBuf buffer) {
        final Uint16 extendedType = ByteBufUtils.readUint16(buffer);
        final HexString opaqueValue = buildOpaqueValue(buffer);
        builder.setOpaqueExtendedType(extendedType).setOpaque(opaqueValue);
    }

    private static HexString buildOpaqueValue(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        return IetfYangUtil.INSTANCE.hexStringFor(ByteArray.readBytes(buffer, length));
    }

    public static List<OpaqueValue> parseOpaqueList(final ByteBuf byteBuf) {
        final List<OpaqueValue> opaqueValues = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final Opaque opaque = parseOpaque(byteBuf);
            if (opaque != null) {
                opaqueValues.add((OpaqueValue) opaque);
            }
        }
        return opaqueValues;
    }

    public static boolean serializeOpaqueList(final List<OpaqueValue> mldpP2mpLsp, final ByteBuf buffer) {
        boolean parsed = false;
        for (final OpaqueValue opaque : mldpP2mpLsp) {
            if (serializeOpaque(opaque, buffer)) {
                parsed = true;
            }
        }
        return parsed;
    }
}
