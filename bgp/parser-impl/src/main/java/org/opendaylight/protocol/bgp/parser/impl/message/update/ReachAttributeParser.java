/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;

/**
 * Abstract base class for reachability attribute parsers, {@link MPReachAttributeParser} and
 * {@link MPUnreachAttributeParser}. These attributes share their RFC7606 handling in that failure to parse them
 * cannot lead to a treat-as-withdraw handling of the message. Individual parser may need access to
 * {@link RevisedErrorHandling} for AFI/SAFI-specific parsing.
 */
public abstract class ReachAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    @Override
    public boolean ignoreDuplicates(final RevisedErrorHandling errorHandling) {
        return errorHandling == RevisedErrorHandling.NONE;
    }

    @Override
    public abstract void parseAttribute(ByteBuf buffer, AttributesBuilder builder, RevisedErrorHandling errorHandling,
            PeerSpecificParserConstraint constraint) throws BGPDocumentedException;
}
