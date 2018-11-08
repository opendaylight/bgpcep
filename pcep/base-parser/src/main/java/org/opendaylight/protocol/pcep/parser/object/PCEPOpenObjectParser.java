/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.parser.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link Open}
 */
public class PCEPOpenObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPOpenObjectParser.class);

    private static final int CLASS = 1;
    private static final int TYPE = 1;

    /*
     * lengths of subfields inside multi-field in bits
     */
    private static final int VERSION_SF_LENGTH = 3;

    /*
     * offsets of subfields inside multi-field in bits
     */
    private static final int VERSION_SF_OFFSET = 0;

    private static final int PCEP_VERSION = 1;

    public PCEPOpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final int versionValue = ByteArray.copyBitsRange(bytes.readByte(), VERSION_SF_OFFSET, VERSION_SF_LENGTH);

        final OpenBuilder builder = new OpenBuilder();
        builder.setVersion(new ProtocolVersion((short) versionValue));
        builder.setProcessingRule(header.isProcessingRule());
        builder.setIgnore(header.isIgnore());
        final short keepalive = bytes.readUnsignedByte();
        builder.setKeepalive(keepalive);
        final short deadTimer = bytes.readUnsignedByte();
        if(keepalive == 0) {
            builder.setDeadTimer((short) 0);
        } else {
            builder.setDeadTimer(deadTimer);
        }
        builder.setSessionId(bytes.readUnsignedByte());

        final TlvsBuilder tbuilder = new TlvsBuilder();
        parseTlvs(tbuilder, bytes.slice());
        builder.setTlvs(tbuilder.build());

        final Open obj = builder.build();
        if (versionValue != PCEP_VERSION) {
            // TODO: Should we move this check into the negotiator
            LOG.debug("Unsupported PCEP version {}", versionValue);
            return new UnknownObject(PCEPErrors.PCEP_VERSION_NOT_SUPPORTED, obj);
        }

        return obj;
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        if (tlv instanceof OfList) {
            tbuilder.setOfList((OfList) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Open, "Wrong instance of PCEPObject. Passed %s. Needed OpenObject.", object.getClass());
        final Open open = (Open) object;
        final ByteBuf body = Unpooled.buffer();
        writeUnsignedByte((short) (PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH)), body);
        writeUnsignedByte(open.getKeepalive(), body);
        writeUnsignedByte(open.getDeadTimer(), body);
        Preconditions.checkArgument(open.getSessionId() != null, "SessionId is mandatory.");
        writeUnsignedByte(open.getSessionId(), body);
        serializeTlvs(open.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getOfList() != null) {
            serializeTlv(tlvs.getOfList(), body);
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
