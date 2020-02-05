/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClusterIdAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterIdAttributeParser.class);

    public static final int TYPE = 10;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        if (errorHandling == RevisedErrorHandling.EXTERNAL) {
            // RFC7606 section 7.10
            LOG.debug("Discarded CLUSTER_LIST attribute from external peer");
            return;
        }

        final int readable = buffer.readableBytes();
        if (readable == 0 && errorHandling != RevisedErrorHandling.NONE) {
            throw new BGPTreatAsWithdrawException(BGPError.ATTR_LENGTH_ERROR, "Empty CLUSTER_LIST attribute");
        }

        if (readable % Ipv4Util.IP4_LENGTH != 0) {
            throw errorHandling.reportError(BGPError.ATTR_LENGTH_ERROR,
                "Length of CLUSTER_LIST should be a multiple of 4, but is %s", readable);
        }

        final int count = readable / Ipv4Util.IP4_LENGTH;
        final List<ClusterIdentifier> list = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            list.add(new ClusterIdentifier(Ipv4Util.addressForByteBuf(buffer)));
        }
        builder.setClusterId(new ClusterIdBuilder().setCluster(list).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final ClusterId cid = pathAttributes.getClusterId();
        if (cid != null) {
            final List<ClusterIdentifier> cluster = cid.getCluster();
            if (cluster != null && !cluster.isEmpty()) {
                final ByteBuf clusterIdBuffer = Unpooled.buffer();
                for (final ClusterIdentifier clusterIdentifier : cluster) {
                    clusterIdBuffer.writeBytes(Ipv4Util.bytesForAddress(clusterIdentifier));
                }
                AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, clusterIdBuffer, byteAggregator);
            }
        }
    }
}
