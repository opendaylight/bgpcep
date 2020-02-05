/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for AttributeParsers which are aware of {@link RevisedErrorHandling}. Inheriting from this class
 * requires that {@link #parseAttribute(ByteBuf, AttributesBuilder, RevisedErrorHandling, PeerSpecificParserConstraint)}
 * is provided and the simplified {@link #parseAttribute(ByteBuf, AttributesBuilder, PeerSpecificParserConstraint)}
 * is implemented by this class.
 *
 * @author Robert Varga
 */
public abstract class AbstractAttributeParser implements AttributeParser {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAttributeParser.class);

    @Override
    public final void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException, BGPParsingException {
        try {
            parseAttribute(buffer, builder, RevisedErrorHandling.NONE, constraint);
        } catch (BGPTreatAsWithdrawException e) {
            LOG.warn("Encountered misreported error", e);
            throw new BGPDocumentedException(e.getMessage(), e.getError(), e);
        }
    }

    @Override
    public abstract void parseAttribute(ByteBuf buffer, AttributesBuilder builder, RevisedErrorHandling errorHandling,
            PeerSpecificParserConstraint constraint) throws BGPDocumentedException, BGPParsingException,
            BGPTreatAsWithdrawException;
}
