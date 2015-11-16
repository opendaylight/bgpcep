/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;

public class ExtendedCommunitiesAttributeParserTest {

    private ExtendedCommunitiesAttributeParser handler;

    @Before
    public void setUp() {
        handler = new ExtendedCommunitiesAttributeParser(NoopReferenceCache.getInstance());
    }

    @Test
    public void testEmptyListExtendedCommunityAttributeParser() throws BGPDocumentedException, BGPParsingException {
        final List<ExtendedCommunities> extendedCommunitiesList = new ArrayList<>();
        final AttributesBuilder attBuilder = new AttributesBuilder().setExtendedCommunities(extendedCommunitiesList);
        final ByteBuf output = Unpooled.buffer();

        handler.serializeAttribute(attBuilder.build(), output);
        Assert.assertEquals(output, output);
    }

    @Test
    public void testEmptyExtendedCommunityAttributeParser() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf output = Unpooled.buffer();
        handler.serializeAttribute(new AttributesBuilder().build(), output);
        Assert.assertEquals( Unpooled.buffer(), output);
    }
}
