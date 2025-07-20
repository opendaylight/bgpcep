/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.pcep.spi.VendorInformationUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.AssociationGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.AssociationGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.association.group.AssociationTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.association.group.AssociationTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.DisjointnessFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.ProtocolOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.AssociationTypeTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.BidirectionalLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.BidirectionalLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.Disjointness;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.DisjointnessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.PathProtection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.PathProtectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.SrPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.SrPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.disjointness.tlvs.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.disjointness.tlvs.StatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.tlvs.CpathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.tlvs.PolicyIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspFlag;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAssociationGroupParser extends CommonObjectParser implements ObjectSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAssociationGroupParser.class);

    private static final int CLASS = 40;

    // Association Object Flags definition
    private static final int RESERVED = 2;
    private static final int FLAGS_SIZE = 16;
    private static final int R_FLAG = 15;

    // TLVs Code Point definition
    private static final int GLOBAL_ASSOCIATION_SOURCE_TLV = 30;
    private static final int EXTENDED_ASSOCIATION_ID_TLV = 31;
    private static final int PATH_PROTECTION_TLV = 38;
    private static final int DISJOINTNESS_CONFIGURATION_TLV = 46;
    private static final int DISJOINTNESS_STATUS_TLV = 47;
    private static final int POLICY_PARAMETERS_TLV = 48;
    private static final int BIDIRECTIONAL_LSP_TLV = 54;
    private static final int SR_POLICY_NAME = 56;
    private static final int SR_POLICY_CPATH_ID = 57;
    private static final int SR_POLICY_CPATH_NAME = 58;
    private static final int SR_POLICY_CPATH_PREF = 59;

    // TLVs Flags definition
    private static final int TLV_FLAGS_SIZE = 32;
    // Path Protection Flags
    private static final int PROTECTION_P_FLAG = 31;
    private static final int PROTECTION_S_FLAG = 30;
    private static final int LSP_FLAG_FULL_REROUTING = 1;
    private static final int LSP_FLAG_TRAFFIC_REROUTING = 2;
    private static final int LSP_FLAG_TRAFFIC_PROTECTION = 3;
    private static final int LSP_FLAG_UNI_PROTECION = 4;
    private static final int LSP_FLAG_BIDIR_PROTECTION = 5;
    // Disjointness Flags
    private static final int LINK_DIVERSE_FLAG = 31;
    private static final int NODE_DIVERSE_FLAG = 30;
    private static final int SRLG_DIVERSE_FLAG = 29;
    private static final int SHORTEST_PATH_FLAG = 28;
    private static final int STRICT_DISJOINT_FLAG = 27;
    // Bidirectional Flags
    private static final int REVERSE_FLAG = 31;
    private static final int CO_ROUTED_FLAG = 30;
    // SR Policy
    private static final int CPATH_RESERVED = 3;
    private static final int IPV4_ENDPOINT_SIZE = 2;
    private static final int IPV6_ENDPOINT_SIZE = 5;


    public AbstractAssociationGroupParser(final int objectType) {
        super(CLASS, objectType);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        buffer.skipBytes(RESERVED);
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        int type = buffer.readUnsignedShort();
        final AssociationType assocType = AssociationType.forValue(type);
        if (assocType == null) {
            throw new PCEPDeserializerException("Non standard / Not Supported Association Type: " + type);
        }
        return new AssociationGroupBuilder()
            .setIgnore(header.getIgnore())
            .setProcessingRule(header.getProcessingRule())
            .setRemovalFlag(flags.get(R_FLAG))
            .setAssociationType(assocType)
            .setAssociationId(readUint16(buffer))
            .setAssociationSource(parseAssociationSource(buffer))
            .setAssociationTlvs(parseAssociationTlvs(assocType, buffer))
            .build();
    }

    protected abstract IpAddressNoZone parseAssociationSource(ByteBuf buffer);

    private static AssociationTlvs parseAssociationTlvs(final AssociationType associationType, final ByteBuf buffer)
            throws PCEPDeserializerException {
        final AssociationTlvsBuilder tlvsBuilder = new AssociationTlvsBuilder();
        // First, parse common Association TLVs
        boolean commonTlvs = true;
        while (buffer.isReadable() && commonTlvs) {
            switch (buffer.getUnsignedShort(buffer.readerIndex())) {
                case GLOBAL_ASSOCIATION_SOURCE_TLV ->
                    tlvsBuilder.setGlobalAssociationSource(parseGlobalAssociation(buffer));
                case EXTENDED_ASSOCIATION_ID_TLV ->
                    tlvsBuilder.setExtendedAssociationId(parseExtendedAssociationId(buffer));
                default -> commonTlvs = false;
            }
        }
        // Then, look to specific Association TLVs according to Association Type
        final AssociationTypeTlvs associationTlvs = switch (associationType) {
            case PathProtection -> parsePathProtection(buffer);
            case Disjoint -> parseDisjointness(buffer);
            case Policy -> parsePolicy(buffer);
            case SingleSideLsp, DoubleSideLsp -> parseBidirectionalLSP(buffer);
            case SrPolicy -> parseSrPolicy(buffer, tlvsBuilder.getExtendedAssociationId());
            default -> {
                LOG.debug("Unsupported Association Group Type: {}", associationType);
                yield null;
            }
        };
        return tlvsBuilder.setAssociationTypeTlvs(associationTlvs).build();
    }

    private static Uint32 parseGlobalAssociation(final ByteBuf buffer) throws PCEPDeserializerException {
        final int type = buffer.readUnsignedShort();
        if (type != GLOBAL_ASSOCIATION_SOURCE_TLV) {
            throw new PCEPDeserializerException("Wrong Global Association Source TLV. Passed: " + type
                    + "; Expected: <= " + GLOBAL_ASSOCIATION_SOURCE_TLV + ".");
        }
        final int length = buffer.readUnsignedShort();
        if (length > buffer.readableBytes()) {
            throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                + buffer.readableBytes() + ".");
        }
        return readUint32(buffer);
    }

    private static List<Uint32> parseExtendedAssociationId(final ByteBuf buffer) throws PCEPDeserializerException {
        final int type = buffer.readUnsignedShort();
        if (type != EXTENDED_ASSOCIATION_ID_TLV) {
            throw new PCEPDeserializerException("Wrong Extended Association ID TLV. Passed: " + type
                    + "; Expected: <= " + EXTENDED_ASSOCIATION_ID_TLV + ".");
        }
        final int length = buffer.readUnsignedShort();
        if (length > buffer.readableBytes()) {
            throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                + buffer.readableBytes() + ".");
        }
        final var extendedAssociationId = new ArrayList<Uint32>();
        for (int i = 0; i < length / 4; i++) {
            extendedAssociationId.add(readUint32(buffer));
        }
        return extendedAssociationId;
    }

    private static PathProtection parsePathProtection(final ByteBuf buffer) throws PCEPDeserializerException {
        final int type = buffer.readUnsignedShort();
        if (type != PATH_PROTECTION_TLV) {
            throw new PCEPDeserializerException("Wrong Path Protection TLV. Passed: " + type + "; Expected: <= "
                + PATH_PROTECTION_TLV + ".");
        }
        final int length = buffer.readUnsignedShort();
        if (length > buffer.readableBytes()) {
            throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                + buffer.readableBytes() + ".");
        }
        final BitArray flags = BitArray.valueOf(buffer, TLV_FLAGS_SIZE);
        return new PathProtectionBuilder()
            .setProtecting(flags.get(PROTECTION_P_FLAG))
            .setSecondary(flags.get(PROTECTION_S_FLAG))
            .setProtectionType(LspFlag.forValue(flags.array()[0]))
            .build();
    }

    private static Disjointness parseDisjointness(final ByteBuf buffer) throws PCEPDeserializerException {
        final DisjointnessBuilder disjointBuilder = new DisjointnessBuilder();

        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            if (length > buffer.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                    + buffer.readableBytes() + ".");
            }
            switch (type) {
                case DISJOINTNESS_CONFIGURATION_TLV -> {
                    final BitArray flags = BitArray.valueOf(buffer, TLV_FLAGS_SIZE);
                    disjointBuilder.setConfiguration(new ConfigurationBuilder()
                        .setLinkDiverse(flags.get(LINK_DIVERSE_FLAG))
                        .setNodeDiverse(flags.get(NODE_DIVERSE_FLAG))
                        .setSrlgDiverse(flags.get(SRLG_DIVERSE_FLAG))
                        .setShortestPath(flags.get(SHORTEST_PATH_FLAG))
                        .setStrictDisjointness(flags.get(STRICT_DISJOINT_FLAG))
                        .build());
                }
                case DISJOINTNESS_STATUS_TLV -> {
                    final BitArray flags = BitArray.valueOf(buffer, TLV_FLAGS_SIZE);
                    disjointBuilder.setStatus(new StatusBuilder()
                        .setLinkDiverse(flags.get(LINK_DIVERSE_FLAG))
                        .setNodeDiverse(flags.get(NODE_DIVERSE_FLAG))
                        .setSrlgDiverse(flags.get(SRLG_DIVERSE_FLAG))
                        .setShortestPath(flags.get(SHORTEST_PATH_FLAG))
                        .setStrictDisjointness(flags.get(STRICT_DISJOINT_FLAG))
                        .build());
                }
                case OFListTlvParser.TYPE -> {
                    final var ofCodes = ImmutableSet.<OfId>builder();
                    for (int i = 0; i < length / 2; i++) {
                        ofCodes.add(new OfId(ByteBufUtils.readUint16(buffer)));
                    }
                    disjointBuilder.setOfList(new OfListBuilder().setCodes(ofCodes.build()).build());
                }
                case VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE -> {
                    buffer.skipBytes(length);
                    LOG.debug("Skipped unsupported Vendor Information");
                }
                default -> {
                    buffer.skipBytes(length);
                    LOG.warn("Unknow Disjointness TLVs: {}", type);
                }
            }
            // Skip padding if any
            buffer.skipBytes(TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
        return disjointBuilder.build();
    }

    private static Policy parsePolicy(final ByteBuf buffer) throws PCEPDeserializerException {
        final int type = buffer.readUnsignedShort();
        if (type != POLICY_PARAMETERS_TLV) {
            throw new PCEPDeserializerException("Wrong Policy Parmeter TLV. Passed: " + type
                    + "; Expected: <= " + POLICY_PARAMETERS_TLV + ".");
        }
        final int length = buffer.readUnsignedShort();
        if (length > buffer.readableBytes()) {
            throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                + buffer.readableBytes() + ".");
        }
        final var policyParameters = new byte[length];
        buffer.readBytes(policyParameters, 0, length);
        return new PolicyBuilder().setPolicyParameters(policyParameters).build();
    }

    private static BidirectionalLsp parseBidirectionalLSP(final ByteBuf buffer) throws PCEPDeserializerException {
        final int type = buffer.readUnsignedShort();
        if (type != BIDIRECTIONAL_LSP_TLV) {
            throw new PCEPDeserializerException("Wrong Bidirectional LSP TLV. Passed: " + type + "; Expected: <= "
                + BIDIRECTIONAL_LSP_TLV + ".");
        }
        final int length = buffer.readUnsignedShort();
        if (length > buffer.readableBytes()) {
            throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                + buffer.readableBytes() + ".");
        }
        final BitArray flags = BitArray.valueOf(buffer, TLV_FLAGS_SIZE);
        return new BidirectionalLspBuilder()
            .setReverseLsp(flags.get(REVERSE_FLAG))
            .setCoRoutedPath(flags.get(CO_ROUTED_FLAG))
            .build();
    }

    private static SrPolicy parseSrPolicy(final ByteBuf buffer, List<Uint32> extendedId)
            throws PCEPDeserializerException {

        // First, check that Extended Association ID is present and contains Color + EndPoint
        if (extendedId == null) {
            throw new PCEPDeserializerException("Missing Extended Association ID TLV for SR Policy");
        }
        final PolicyIdBuilder policyBuilder = new PolicyIdBuilder();
        policyBuilder.setColor(extendedId.getFirst());
        final ByteBuf tlvBuf = Unpooled.buffer();
        if (extendedId.size() == IPV4_ENDPOINT_SIZE) {
            writeUint32(tlvBuf, extendedId.getLast());
            policyBuilder.setEndpoint(new IpAddressNoZone(Ipv4Util.addressForByteBuf(tlvBuf)));
        } else if (extendedId.size() == IPV6_ENDPOINT_SIZE) {
            for (int i = 1; i < extendedId.size(); i++) {
                writeUint32(tlvBuf, extendedId.get(i));
            }
            policyBuilder.setEndpoint(new IpAddressNoZone(Ipv6Util.addressForByteBuf(tlvBuf)));
        } else {
            throw new PCEPDeserializerException("Wrong Endpoint length specified. Passed: " + extendedId.size()
                    + "; Expected: " + IPV4_ENDPOINT_SIZE + " or " + IPV6_ENDPOINT_SIZE + ".");
        }

        // Then parse SR Policy TLVs
        final CpathBuilder cpathBuilder = new CpathBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            if (length > buffer.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                    + buffer.readableBytes() + ".");
            }
            switch (type) {
                case SR_POLICY_NAME -> policyBuilder.setName(ByteArray.readBytes(buffer, length));
                case SR_POLICY_CPATH_NAME -> cpathBuilder.setName(ByteArray.readBytes(buffer, length));
                case SR_POLICY_CPATH_PREF -> cpathBuilder.setPreference(readUint32(buffer));
                case SR_POLICY_CPATH_ID -> parseCpathID(buffer, cpathBuilder);
                default -> {
                    LOG.debug("Unsupported SR Policy Type: {}", type);
                }
            }
            // Skip padding if any
            buffer.skipBytes(TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
        return new SrPolicyBuilder().setPolicyId(policyBuilder.build()).setCpath(cpathBuilder.build()).build();
    }

    private static void parseCpathID(final ByteBuf buffer, final CpathBuilder cpathBuilder) {
        final var origin = ProtocolOrigin.forValue(readUint8(buffer).intValue());
        buffer.skipBytes(CPATH_RESERVED);
        final var asn = readUint32(buffer);
        final var originAddress = new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer));
        final var descriminator = readUint32(buffer);

        cpathBuilder.setOrigin(origin)
            .setOriginatorAsn(asn)
            .setOriginatorAddress(originAddress)
            .setDiscriminator(descriminator);
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof AssociationGroup,
            "Wrong instance of PCEPObject. Passed %s. Needed AssociationGroup.", object.getClass());
        final AssociationGroup assoc = (AssociationGroup) object;
        final ByteBuf body = Unpooled.buffer();

        body.writeZero(RESERVED);
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(R_FLAG, assoc.getRemovalFlag());
        bs.toByteBuf(body);
        body.writeShort(assoc.getAssociationType().getIntValue());
        writeUint16(body, assoc.getAssociationId());
        if (!serializeAssociationSource(assoc.getAssociationSource(), body)) {
            /*
             * Association Source is mandatory. Stop serialization if IPv4 respectively IPv6 is not set
             * according to the Association Group Type (IPv4 or IPv6).
             */
            LOG.warn("Missing mandatory Association Source");
            return;
        }
        // Serialize TLVs according to the Association Type
        if (assoc.getAssociationTlvs() != null) {
            serializeTlvs(assoc.getAssociationType(), assoc.getAssociationTlvs(), body);
        }
        ObjectUtil.formatSubobject(getObjectType(), getObjectClass(), object.getProcessingRule(),
                object.getIgnore(), body, buffer);
    }

    protected abstract boolean serializeAssociationSource(IpAddressNoZone source, ByteBuf buffer);

    private static void serializeTlvs(final AssociationType associationType, final AssociationTlvs tlvs,
            final ByteBuf buffer) {
        // First, serialize common Association TLVs
        if (tlvs.getGlobalAssociationSource() != null) {
            final var tlvBuf = Unpooled.buffer();
            writeUint32(tlvBuf, tlvs.getGlobalAssociationSource());
            TlvUtil.formatTlv(GLOBAL_ASSOCIATION_SOURCE_TLV, tlvBuf, buffer);
        } if (tlvs.getExtendedAssociationId() != null) {
            final var tlvBuf = Unpooled.buffer();
            tlvs.getExtendedAssociationId().forEach(id -> writeUint32(tlvBuf, id));
            TlvUtil.formatTlv(EXTENDED_ASSOCIATION_ID_TLV, tlvBuf, buffer);
        }
        // Then, serialize specific Association TLVs according to Association Type
        if (tlvs.getAssociationTypeTlvs() != null) {
            final var nextTlvs = tlvs.getAssociationTypeTlvs();
            switch (associationType) {
                case PathProtection -> serializePathProtection((PathProtection )nextTlvs, buffer);
                case Disjoint -> serializeDisjointness((Disjointness )nextTlvs, buffer);
                case Policy -> serializePolicy((Policy )nextTlvs, buffer);
                case SingleSideLsp, DoubleSideLsp -> serializeBidirectionalLSP((BidirectionalLsp )nextTlvs, buffer);
                case SrPolicy -> serializeSrPolicy((SrPolicy )nextTlvs, buffer);
                default -> {
                    LOG.debug("Unsupported Association Group Type: {}", associationType);
                }
            }
        }
    }

    private static void serializePathProtection(final PathProtection tlvs, final ByteBuf buffer) {
        final BitArray bs = new BitArray(TLV_FLAGS_SIZE);
        bs.set(PROTECTION_P_FLAG, tlvs.getProtecting());
        bs.set(PROTECTION_S_FLAG, tlvs.getSecondary());
        final var flags = tlvs.getProtectionType();
        if (flags != null) {
            switch (flags) {
                case FullRerouting -> bs.set(LSP_FLAG_FULL_REROUTING, true);
                case ReroutingWithoutExtraTraffic -> bs.set(LSP_FLAG_TRAFFIC_REROUTING,  true);
                case ProtectionWithExtraTraffic -> bs.set(LSP_FLAG_TRAFFIC_PROTECTION, true);
                case UnidirectionalProtection -> bs.set(LSP_FLAG_UNI_PROTECION, true);
                case BidirectionalProtection -> bs.set(LSP_FLAG_BIDIR_PROTECTION, true);
                default -> {
                    LOG.debug("Unknown Path Protection Flag: {}", flags);
                }
            }
        }
        final var tlvBuf = Unpooled.buffer();
        bs.toByteBuf(tlvBuf);
        TlvUtil.formatTlv(PATH_PROTECTION_TLV, tlvBuf, buffer);
    }

    private static void serializeDisjointness(final Disjointness tlvs, final ByteBuf buffer) {
        if (tlvs.getConfiguration() != null) {
            serializeDisjointFlags(tlvs.getConfiguration(), DISJOINTNESS_CONFIGURATION_TLV, buffer);
        }
        if (tlvs.getStatus() != null) {
            serializeDisjointFlags(tlvs.getStatus(), DISJOINTNESS_STATUS_TLV, buffer);
        }
        if (tlvs.getOfList() != null) {
            final ByteBuf tlvBuf = Unpooled.buffer();
            tlvs.getOfList().getCodes().forEach(id -> ByteBufUtils.write(tlvBuf, id.getValue()));
            TlvUtil.formatTlv(OFListTlvParser.TYPE, tlvBuf, buffer);
        }
        // Skip Vendor Information which is not used by ODL
    }

    private static void serializeDisjointFlags(final DisjointnessFlags flags, final int type, final ByteBuf buffer) {
        final BitArray bs = new BitArray(TLV_FLAGS_SIZE);
        bs.set(LINK_DIVERSE_FLAG, flags.getLinkDiverse());
        bs.set(NODE_DIVERSE_FLAG, flags.getNodeDiverse());
        bs.set(SRLG_DIVERSE_FLAG, flags.getSrlgDiverse());
        bs.set(SHORTEST_PATH_FLAG, flags.getShortestPath());
        bs.set(STRICT_DISJOINT_FLAG, flags.getStrictDisjointness());
        final var tlvBuf = Unpooled.buffer();
        bs.toByteBuf(tlvBuf);
        TlvUtil.formatTlv(type, tlvBuf, buffer);
    }

    private static void serializePolicy(final Policy tlvs, final ByteBuf buffer) {
        final var tlvBuf = Unpooled.buffer();
        tlvBuf.writeBytes(tlvs.getPolicyParameters());
        TlvUtil.formatTlv(POLICY_PARAMETERS_TLV, tlvBuf, buffer);
    }

    private static void serializeBidirectionalLSP(final BidirectionalLsp tlvs, final ByteBuf buffer) {
        final BitArray bs = new BitArray(TLV_FLAGS_SIZE);
        bs.set(REVERSE_FLAG, tlvs.getReverseLsp());
        bs.set(CO_ROUTED_FLAG, tlvs.getCoRoutedPath());
        final var tlvBuf = Unpooled.buffer();
        bs.toByteBuf(tlvBuf);
        TlvUtil.formatTlv(BIDIRECTIONAL_LSP_TLV, tlvBuf, buffer);
    }

    private static void serializeSrPolicy(final SrPolicy tlvs, final ByteBuf buffer) {
        final ByteBuf tlvBuf = Unpooled.buffer();
        if (tlvs.getPolicyId().getName() != null) {
            TlvUtil.formatTlv(SR_POLICY_NAME, Unpooled.copiedBuffer(tlvs.getPolicyId().getName()), buffer);
        }
        if (tlvs.getCpath().getOrigin() != null) {
            tlvBuf.clear();
            tlvBuf.writeByte(tlvs.getCpath().getOrigin().getIntValue());
            tlvBuf.writeZero(CPATH_RESERVED);
            writeUint32(tlvBuf, tlvs.getCpath().getOriginatorAsn());
            Ipv6Util.writeIpv6Address(tlvs.getCpath().getOriginatorAddress().getIpv6AddressNoZone(), tlvBuf);
            writeUint32(tlvBuf, tlvs.getCpath().getDiscriminator());
            TlvUtil.formatTlv(SR_POLICY_CPATH_ID, tlvBuf, buffer);
        }
        if (tlvs.getCpath().getName() != null) {
            TlvUtil.formatTlv(SR_POLICY_CPATH_NAME, Unpooled.copiedBuffer(tlvs.getCpath().getName()), buffer);
        }
        if (tlvs.getCpath().getPreference() != null) {
            tlvBuf.clear();
            writeUint32(tlvBuf, tlvs.getCpath().getPreference());
            TlvUtil.formatTlv(SR_POLICY_CPATH_PREF, tlvBuf, buffer);
        }
    }
}
