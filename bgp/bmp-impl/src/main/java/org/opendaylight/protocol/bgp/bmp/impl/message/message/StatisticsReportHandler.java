package org.opendaylight.protocol.bgp.bmp.impl.message.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
    private static final short REJECT_PREFIX_TYPE = 0;
    private static final short DUPLICATE_PREFIX_ADV_TYPE = 1;
    private static final short DUPLICATE_WITHDRAWS_TYPE = 2;
    private static final short INVALIDATE_CLUSTER_TYPE = 3;
    private static final short AS_PATH_TYPE = 4;
    private static final short INVALIDATE_ORIGINATOR_ID_TYPE = 5;
    private static final short INVALIDATE_AS_CONFED_TYPE = 6;
    private static final short ADJ_RIBS_IN_ROUTES_TYPE = 7;
    private static final short LOC_RIB_ROUTES_TYPE = 8;

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
        //Long
        final Long rejectedPrefixes = statsReport.getRejectedPrefixes().getValue();
        if (rejectedPrefixes != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(REJECT_PREFIX_TYPE, rejectedPrefixes, bufLocal);
        }

        final Long duplicatePrefixAdv = statsReport.getDuplicatePrefixAdvertisements().getValue();
        if (duplicatePrefixAdv != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(DUPLICATE_PREFIX_ADV_TYPE, duplicatePrefixAdv, bufLocal);
        }

        final Long duplicateWithdraws = statsReport.getDuplicateWithdraws().getValue();
        if (duplicateWithdraws != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(DUPLICATE_WITHDRAWS_TYPE, duplicateWithdraws, bufLocal);
        }

        final Long invalidatedCluster = statsReport.getInvalidatedClusterListLoop().getValue();
        if (invalidatedCluster != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(INVALIDATE_CLUSTER_TYPE, invalidatedCluster, bufLocal);
        }

        final Long asPath = statsReport.getInvalidatedAsPathLoop().getValue();
        if (asPath != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(AS_PATH_TYPE, asPath, bufLocal);
        }

        final Long invalidatedOriginatorId = statsReport.getInvalidatedOriginatorId().getValue();
        if (invalidatedOriginatorId != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(INVALIDATE_ORIGINATOR_ID_TYPE, invalidatedOriginatorId, bufLocal);
        }

        final Long asConfed = statsReport.getInvalidatedAsConfedLoop().getValue();
        if (asConfed != null) {
            numTLvs++;
            TlvUtil.formatTlvCounter32(INVALIDATE_AS_CONFED_TYPE, asConfed, bufLocal);
        }

        final BigInteger adjRibsInRoutes = statsReport.getAdjRibsInRoutes().getValue();
        if (asConfed != null) {
            numTLvs++;
            TlvUtil.formatTlvGauge64(ADJ_RIBS_IN_ROUTES_TYPE, adjRibsInRoutes, bufLocal);
        }

        final BigInteger locRibRoutes = statsReport.getLocRibRoutes().getValue();
        if (locRibRoutes != null) {
            numTLvs++;
            TlvUtil.formatTlvGauge64(LOC_RIB_ROUTES_TYPE, locRibRoutes, bufLocal);
        }

        buffer.writeInt(numTLvs);
        buffer.writeBytes(bufLocal);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (PeerUp) message: {}", ByteBufUtil.hexDump(bytes));
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        StatsReportsMessageBuilder statReport = new StatsReportsMessageBuilder().setPeerHeader(header);

        final int numTlv = bytes.readInt();

        for (int i = 0; i < numTlv; i++) {
            final short type = bytes.readShort();
            final short lenght = bytes.readShort();
            ByteBuf value = bytes.readSlice(lenght);
            parseTlv(type, value, statReport);
        }

        return statReport.build();
    }

    private void parseTlv(short type, ByteBuf value, StatsReportsMessageBuilder statReport) {
        switch (type) {
        case REJECT_PREFIX_TYPE:
            statReport.setRejectedPrefixes(new Counter32(value.readLong()));
            break;
        case DUPLICATE_PREFIX_ADV_TYPE:
            statReport.setDuplicatePrefixAdvertisements(new Counter32(value.readLong()));
            break;
        case DUPLICATE_WITHDRAWS_TYPE:
            statReport.setDuplicateWithdraws(new Counter32(value.readLong()));
            break;
        case INVALIDATE_CLUSTER_TYPE:
            statReport.setInvalidatedClusterListLoop(new Counter32(value.readLong()));
            break;
        case AS_PATH_TYPE:
            statReport.setInvalidatedAsPathLoop(new Counter32(value.readLong()));
            break;
        case INVALIDATE_ORIGINATOR_ID_TYPE:
            statReport.setInvalidatedOriginatorId(new Counter32(value.readLong()));
            break;
        case INVALIDATE_AS_CONFED_TYPE:
            statReport.setInvalidatedAsConfedLoop(new Counter32(value.readLong()));
            break;
        case ADJ_RIBS_IN_ROUTES_TYPE:
            statReport.setAdjRibsInRoutes(new Gauge64(new BigInteger(value.readBytes(8).array())));
            break;
        case LOC_RIB_ROUTES_TYPE:
            statReport.setLocRibRoutes(new Gauge64(new BigInteger(value.readBytes(8).array())));
            break;
        }
    }

}
