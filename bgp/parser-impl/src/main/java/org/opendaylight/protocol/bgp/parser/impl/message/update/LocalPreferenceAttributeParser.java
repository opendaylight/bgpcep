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
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalPreferenceAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(LocalPreferenceAttributeParser.class);

    public static final int TYPE = 5;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        if (errorHandling == RevisedErrorHandling.EXTERNAL) {
            // RFC7606 section 7.7
            LOG.debug("Discarded LOCAL_PREF attribute from external peer");
            return;
        }

        final int readable = buffer.readableBytes();
        if (readable != 4) {
            throw errorHandling.reportError(BGPError.ATTR_LENGTH_ERROR, "Expected 4 bytes, have %s", readable);
        }

        builder.setLocalPref(new LocalPrefBuilder().setPref(ByteBufUtils.readUint32(buffer)).build());
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final LocalPref lp = attribute.getLocalPref();
        if (lp != null) {
            final Uint32 pref = lp.getPref();
            if (pref != null) {
                AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, Unpooled.copyInt(pref.intValue()),
                    byteAggregator);
            }
        }
    }
}
