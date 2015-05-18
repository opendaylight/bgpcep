/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.ADJ_RIBS_IN_ROUTES_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.AS_PATH_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.DUPLICATE_PREFIX_ADV_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.DUPLICATE_WITHDRAWS_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.INVALIDATE_AS_CONFED_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.INVALIDATE_CLUSTER_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.INVALIDATE_ORIGINATOR_ID_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.LOC_RIB_ROUTES_TYPE;
import static org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler.TlvType.REJECT_PREFIX_TYPE;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.LONG_BYTES_LENGTH;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.util.Map;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.parser.BMPDocumentedException;
import org.opendaylight.protocol.bmp.parser.BMPError;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class StatisticsReportHandler extends AbstractBmpPerPeerMessageParser {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsReportHandler.class);

    private static final int MESSAGE_TYPE = 1;

    public StatisticsReportHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof StatsReportsMessage, "An instance of Statistics Reports message is required");
        final StatsReportsMessage statsReport = (StatsReportsMessage) message;

        int tlvsCount = 0;
        final ByteBuf tlvsBuf = Unpooled.buffer();

        tlvsCount += TlvUtil.formatTlvCounter32(REJECT_PREFIX_TYPE.getValue(), statsReport.getRejectedPrefixes(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(DUPLICATE_PREFIX_ADV_TYPE.getValue(), statsReport.getDuplicatePrefixAdvertisements(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(DUPLICATE_WITHDRAWS_TYPE.getValue(), statsReport.getDuplicateWithdraws(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(INVALIDATE_CLUSTER_TYPE.getValue(), statsReport.getInvalidatedClusterListLoop(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(AS_PATH_TYPE.getValue(), statsReport.getInvalidatedAsPathLoop(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(INVALIDATE_ORIGINATOR_ID_TYPE.getValue(), statsReport.getInvalidatedOriginatorId(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvCounter32(INVALIDATE_AS_CONFED_TYPE.getValue(), statsReport.getInvalidatedAsConfedLoop(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvGauge64(ADJ_RIBS_IN_ROUTES_TYPE.getValue(), statsReport.getAdjRibsInRoutes(), tlvsBuf);
        tlvsCount += TlvUtil.formatTlvGauge64(LOC_RIB_ROUTES_TYPE.getValue(), statsReport.getLocRibRoutes(), tlvsBuf);

        ByteBufWriteUtil.writeUnsignedShort(tlvsCount, buffer);
        buffer.writeBytes(tlvsBuf);

        LOG.trace("Statistics Reports message serialized to: {}", ByteBufUtil.hexDump(buffer));
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BMPDocumentedException {
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        StatsReportsMessageBuilder statReport = new StatsReportsMessageBuilder().setPeerHeader(header);

        final int tlvsCount = bytes.readInt();

        for (int i = 0; i < tlvsCount; i++) {
            final TlvUtil.Tlv tlv = TlvUtil.Tlv.fromByteBuf(bytes);
            parseTlv(tlv.getType(), tlv.getValue(), statReport);
        }

        LOG.debug("Peer Up notification was parsed: err = {}, data = {}.", statReport.getAdjRibsInRoutes(),
            statReport.getDuplicatePrefixAdvertisements(), statReport.getDuplicateWithdraws(), statReport
                .getInvalidatedAsConfedLoop(), statReport.getInvalidatedClusterListLoop(), statReport
                .getInvalidatedOriginatorId(), statReport.getRejectedPrefixes(), statReport.getLocRibRoutes());

        return statReport.build();
    }

    private void parseTlv(final int type, final ByteBuf value, final StatsReportsMessageBuilder statReport) throws BMPDocumentedException {
        switch (TlvType.forValue(type)) {
        case REJECT_PREFIX_TYPE:
            statReport.setRejectedPrefixes(new Counter32(value.readUnsignedInt()));
            break;
        case DUPLICATE_PREFIX_ADV_TYPE:
            statReport.setDuplicatePrefixAdvertisements(new Counter32(value.readUnsignedInt()));
            break;
        case DUPLICATE_WITHDRAWS_TYPE:
            statReport.setDuplicateWithdraws(new Counter32(value.readUnsignedInt()));
            break;
        case INVALIDATE_CLUSTER_TYPE:
            statReport.setInvalidatedClusterListLoop(new Counter32(value.readUnsignedInt()));
            break;
        case AS_PATH_TYPE:
            statReport.setInvalidatedAsPathLoop(new Counter32(value.readUnsignedInt()));
            break;
        case INVALIDATE_ORIGINATOR_ID_TYPE:
            statReport.setInvalidatedOriginatorId(new Counter32(value.readUnsignedInt()));
            break;
        case INVALIDATE_AS_CONFED_TYPE:
            statReport.setInvalidatedAsConfedLoop(new Counter32(value.readUnsignedInt()));
            break;
        case ADJ_RIBS_IN_ROUTES_TYPE:
            statReport.setAdjRibsInRoutes(new Gauge64(new BigInteger(value.readBytes(LONG_BYTES_LENGTH).array())));
            break;
        case LOC_RIB_ROUTES_TYPE:
            statReport.setLocRibRoutes(new Gauge64(new BigInteger(value.readBytes(LONG_BYTES_LENGTH).array())));
            break;
        default:
            throw new BMPDocumentedException("Could not parse TLV Type on Statistis Report Message", BMPError.OPT_ATTR_ERROR);
        }
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    enum TlvType {
        REJECT_PREFIX_TYPE(0), DUPLICATE_PREFIX_ADV_TYPE(1), DUPLICATE_WITHDRAWS_TYPE(2),
        INVALIDATE_CLUSTER_TYPE(3), AS_PATH_TYPE(4), INVALIDATE_ORIGINATOR_ID_TYPE(5),
        INVALIDATE_AS_CONFED_TYPE(6), ADJ_RIBS_IN_ROUTES_TYPE(7), LOC_RIB_ROUTES_TYPE(8);

        private static final Map<Integer, TlvType> VALUE_MAP;

        static {
            final ImmutableMap.Builder<Integer, TlvType> b = ImmutableMap.builder();
            for (final TlvType enumItem : TlvType.values()) {
                b.put(enumItem.getValue(), enumItem);
            }
            VALUE_MAP = b.build();
        }

        private final int value;

        TlvType(final int value) {
            this.value = value;
        }

        public static TlvType forValue(final int value) {
            return VALUE_MAP.get(value);
        }

        public int getValue() {
            return this.value;
        }
    }

}
