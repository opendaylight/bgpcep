/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.association.range.tlv.AssociationRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.association.type.list.tlv.AssociationTypeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.setup.type.capability.tlv.PathSetupTypeCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.sr.policy.capability.tlv.SrPolicyCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link Open}.
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
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        final int versionValue = ByteArray.copyBitsRange(bytes.readByte(), VERSION_SF_OFFSET, VERSION_SF_LENGTH);

        final short keepalive = bytes.readUnsignedByte();
        final short deadTimer = bytes.readUnsignedByte();
        final Uint8 sessionId = ByteBufUtils.readUint8(bytes);
        final TlvsBuilder tbuilder = new TlvsBuilder();
        parseTlvs(tbuilder, bytes.slice());

        final Open obj = new OpenBuilder()
            .setVersion(new ProtocolVersion(Uint8.valueOf(versionValue)))
            .setProcessingRule(header.getProcessingRule())
            .setIgnore(header.getIgnore())
            .setKeepalive(Uint8.valueOf(keepalive))
            .setSessionId(sessionId)
            .setTlvs(tbuilder.build())
            .setDeadTimer(keepalive == 0 ? Uint8.ZERO : Uint8.valueOf(deadTimer))
            .build();
        if (versionValue != PCEP_VERSION) {
            // TODO: Should we move this check into the negotiator
            LOG.debug("Unsupported PCEP version {}", versionValue);
            return new UnknownObject(PCEPErrors.PCEP_VERSION_NOT_SUPPORTED, obj);
        }

        return obj;
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        if (tlv instanceof OfList ol) {
            tbuilder.setOfList(ol);
        }
        if (tlv instanceof AssociationTypeList atl) {
            tbuilder.setAssociationTypeList(atl);
        }
        if (tlv instanceof AssociationRange ar) {
            tbuilder.setAssociationRange(ar);
        }
        if (tlv instanceof PathSetupTypeCapability pstc) {
            LOG.info("Add Path Setup Type Capability TLV: {}", pstc);
            tbuilder.setPathSetupTypeCapability(pstc);
        }
        if (tlv instanceof SrPolicyCapability spc) {
            tbuilder.setSrPolicyCapability(spc);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Open, "Wrong instance of PCEPObject. Passed %s. Needed OpenObject.",
            object.getClass());
        final Open open = (Open) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeByte(PCEP_VERSION << Byte.SIZE - VERSION_SF_LENGTH);
        ByteBufUtils.writeOrZero(body, open.getKeepalive());
        ByteBufUtils.writeOrZero(body, open.getDeadTimer());
        ByteBufUtils.writeMandatory(body, open.getSessionId(), "SessionId");
        serializeTlvs(open.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    @NonNullByDefault
    public void serializeTlvs(final @Nullable Tlvs tlvs, final ByteBuf body) {
        if (tlvs != null) {
            serializeOptionalTlv(tlvs.getOfList(), body);
            serializeOptionalTlv(tlvs.getAssociationTypeList(), body);
            serializeOptionalTlv(tlvs.getAssociationRange(), body);
            serializeOptionalTlv(tlvs.getPathSetupTypeCapability(), body);
            serializeOptionalTlv(tlvs.getSrPolicyCapability(), body);
            serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        }
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
