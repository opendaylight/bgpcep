/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.INT_BYTES_LENGTH;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.AdjRibsInRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicatePrefixAdvertisementsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateUpdatesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateWithdrawsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsConfedLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsPathLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedClusterListLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedOriginatorIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.LocRibRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiAdjRibInTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiLocRibTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PrefixesTreatedAsWithdrawTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.RejectedPrefixesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.UpdatesTreatedAsWithdrawTlv;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class StatisticsReportHandler extends AbstractBmpPerPeerMessageParser<TlvsBuilder> {

    private static final int MESSAGE_TYPE = 1;

    public StatisticsReportHandler(final MessageRegistry bgpMssageRegistry, final BmpTlvRegistry tlvRegistry) {
        super(bgpMssageRegistry, tlvRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof StatsReportsMessage, "An instance of Statistics Reports message is required");
        final StatsReportsMessage statsReport = (StatsReportsMessage) message;
        serializeTlvs(statsReport.getTlvs(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final StatsReportsMessageBuilder statReport = new StatsReportsMessageBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        bytes.skipBytes(INT_BYTES_LENGTH);
        parseTlvs(tlvsBuilder, bytes);

        return statReport.setTlvs(tlvsBuilder.build()).build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    protected void serializeTlvs(final Tlvs tlvs, final ByteBuf output) {
        final AtomicInteger counter = new AtomicInteger(0);
        final ByteBuf tlvsBuffer = Unpooled.buffer();
        serializeStatTlv(tlvs.getRejectedPrefixesTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getDuplicatePrefixAdvertisementsTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getDuplicateWithdrawsTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getInvalidatedClusterListLoopTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getInvalidatedAsPathLoopTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getInvalidatedOriginatorIdTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getInvalidatedAsConfedLoopTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getAdjRibsInRoutesTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getLocRibRoutesTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getPerAfiSafiAdjRibInTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getPerAfiSafiLocRibTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getUpdatesTreatedAsWithdrawTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getPrefixesTreatedAsWithdrawTlv(), tlvsBuffer, counter);
        serializeStatTlv(tlvs.getDuplicateUpdatesTlv(), tlvsBuffer, counter);

        writeUnsignedInt(counter.longValue(), output);
        output.writeBytes(tlvsBuffer);
    }

    private void serializeStatTlv(final Tlv tlv, final ByteBuf tlvsBuffer, final AtomicInteger counter) {
        if (tlv != null) {
            counter.incrementAndGet();
            serializeTlv(tlv, tlvsBuffer);
        }
    }

    @Override
    protected void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof AdjRibsInRoutesTlv) {
            builder.setAdjRibsInRoutesTlv((AdjRibsInRoutesTlv) tlv);
        } else if (tlv instanceof DuplicatePrefixAdvertisementsTlv) {
            builder.setDuplicatePrefixAdvertisementsTlv((DuplicatePrefixAdvertisementsTlv) tlv);
        } else if (tlv instanceof DuplicateWithdrawsTlv) {
            builder.setDuplicateWithdrawsTlv((DuplicateWithdrawsTlv) tlv);
        } else if (tlv instanceof InvalidatedAsConfedLoopTlv) {
            builder.setInvalidatedAsConfedLoopTlv((InvalidatedAsConfedLoopTlv) tlv);
        } else if (tlv instanceof InvalidatedAsPathLoopTlv) {
            builder.setInvalidatedAsPathLoopTlv((InvalidatedAsPathLoopTlv) tlv);
        } else if (tlv instanceof InvalidatedClusterListLoopTlv) {
            builder.setInvalidatedClusterListLoopTlv((InvalidatedClusterListLoopTlv) tlv);
        } else if (tlv instanceof InvalidatedOriginatorIdTlv) {
            builder.setInvalidatedOriginatorIdTlv((InvalidatedOriginatorIdTlv) tlv);
        } else if (tlv instanceof LocRibRoutesTlv) {
            builder.setLocRibRoutesTlv((LocRibRoutesTlv) tlv);
        } else if (tlv instanceof RejectedPrefixesTlv) {
            builder.setRejectedPrefixesTlv((RejectedPrefixesTlv) tlv);
        } else if (tlv instanceof PerAfiSafiAdjRibInTlv) {
            builder.setPerAfiSafiAdjRibInTlv((PerAfiSafiAdjRibInTlv) tlv);
        } else if (tlv instanceof PerAfiSafiLocRibTlv) {
            builder.setPerAfiSafiLocRibTlv((PerAfiSafiLocRibTlv) tlv);
        } else if (tlv instanceof UpdatesTreatedAsWithdrawTlv) {
            builder.setUpdatesTreatedAsWithdrawTlv((UpdatesTreatedAsWithdrawTlv) tlv);
        } else if (tlv instanceof PrefixesTreatedAsWithdrawTlv) {
            builder.setPrefixesTreatedAsWithdrawTlv((PrefixesTreatedAsWithdrawTlv) tlv);
        } else if (tlv instanceof DuplicateUpdatesTlv) {
            builder.setDuplicateUpdatesTlv((DuplicateUpdatesTlv) tlv);
        }
    }
}
