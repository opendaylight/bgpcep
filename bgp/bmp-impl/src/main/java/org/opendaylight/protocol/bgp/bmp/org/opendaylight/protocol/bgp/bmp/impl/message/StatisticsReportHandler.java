package org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
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

    public StatisticsReportHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof StatsReportsMessage,
            "BMP Notification message cannot be null");
        final StatsReportsMessage statsReport = (StatsReportsMessage) message;
        this.serializePerPeerHeader(statsReport.getPeerHeader(), buffer);

        int numTLvs = 0;

        final ByteBuf bufLocal = Unpooled.buffer();

        final Long rejectedPrefixes = statsReport.getRejectedPrefixes().getValue();

        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.REJECT_PREFIX_TYPE.ordinal(), rejectedPrefixes, bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.DUPLICATE_PREFIX_ADV_TYPE.ordinal(), statsReport.getDuplicatePrefixAdvertisements().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.DUPLICATE_WITHDRAWS_TYPE.ordinal(), statsReport.getDuplicateWithdraws().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.INVALIDATE_CLUSTER_TYPE.ordinal(), statsReport.getInvalidatedClusterListLoop().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.AS_PATH_TYPE.ordinal(), statsReport.getInvalidatedAsPathLoop().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.INVALIDATE_ORIGINATOR_ID_TYPE.ordinal(), statsReport.getInvalidatedOriginatorId().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvCounter32(Type.INVALIDATE_AS_CONFED_TYPE.ordinal(), statsReport.getInvalidatedAsConfedLoop().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvGauge64(Type.ADJ_RIBS_IN_ROUTES_TYPE.ordinal(), statsReport.getAdjRibsInRoutes().getValue(), bufLocal);
        numTLvs = numTLvs + TlvUtil.formatTlvGauge64(Type.LOC_RIB_ROUTES_TYPE.ordinal(), statsReport.getLocRibRoutes().getValue(), bufLocal);


        buffer.writeInt(numTLvs);
        buffer.writeBytes(bufLocal);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        this.checkByteBufMotNull(bytes);
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        StatsReportsMessageBuilder statReport = new StatsReportsMessageBuilder().setPeerHeader(header);

        final int numTlv = bytes.readInt();

        for (int i = 0; i < numTlv; i++) {
            final int type = bytes.readUnsignedShort();
            final int length = bytes.readUnsignedShort();
            ByteBuf value = bytes.readSlice(length);
            parseTlv(type, value, statReport);
        }

        return statReport.build();
    }

    private void parseTlv(final int type, final ByteBuf value, final StatsReportsMessageBuilder statReport) {
        switch (Type.values()[type]) {
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
            statReport.setAdjRibsInRoutes(new Gauge64(new BigInteger(value.readBytes(TlvUtil.Gauge64).array())));
            break;
        case LOC_RIB_ROUTES_TYPE:
            statReport.setLocRibRoutes(new Gauge64(new BigInteger(value.readBytes(TlvUtil.Gauge64).array())));
            break;
        }
    }

    enum Type {
        REJECT_PREFIX_TYPE(0), DUPLICATE_PREFIX_ADV_TYPE(1), DUPLICATE_WITHDRAWS_TYPE(2),
        INVALIDATE_CLUSTER_TYPE(3), AS_PATH_TYPE(4), INVALIDATE_ORIGINATOR_ID_TYPE(5),
        INVALIDATE_AS_CONFED_TYPE(6), ADJ_RIBS_IN_ROUTES_TYPE(7), LOC_RIB_ROUTES_TYPE(8);
        public final int value;

        Type(final int value) {
            this.value = value;
        }
    }

}
