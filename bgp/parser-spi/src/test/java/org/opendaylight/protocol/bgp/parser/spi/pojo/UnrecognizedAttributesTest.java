/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.UnrecognizedAttributesKey;

public class UnrecognizedAttributesTest {

    private static final int UNRECOGNIZED_ATTRIBUTE_COUNT = 1;
    private static final int FIRST_ATTRIBUTE = 0;
    private static final short NON_EXISTENT_TYPE = 0;
    private static final int NON_VALUE_BYTES = 3;

    private static final SimpleAttributeRegistry simpleAttrReg = new SimpleAttributeRegistry();

    @Rule
    public ExpectedException expException = ExpectedException.none();

    @Test
    public void testUnrecognizedAttributesWithoutOptionalFlag() throws BGPDocumentedException, BGPParsingException {
        this.expException.expect(BGPDocumentedException.class);
        this.expException.expectMessage("Well known attribute not recognized.");
        simpleAttrReg.parseAttributes(
            Unpooled.wrappedBuffer(new byte[] { 0x03, 0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 }), null);
    }

    @Test
    public void testUnrecognizedAttributes() throws BGPDocumentedException, BGPParsingException {
        final byte[] attributeBytes = { (byte)0xe0, 0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 };
        final List<UnrecognizedAttributes> unrecogAttribs = simpleAttrReg
            .parseAttributes(Unpooled.wrappedBuffer(attributeBytes), null).getUnrecognizedAttributes();
        assertEquals(UNRECOGNIZED_ATTRIBUTE_COUNT, unrecogAttribs.size());
        final UnrecognizedAttributes unrecogAttrib = unrecogAttribs.get(FIRST_ATTRIBUTE);
        final UnrecognizedAttributesKey expectedAttribKey =
            new UnrecognizedAttributesKey(unrecogAttrib.getType());

        assertTrue(unrecogAttrib.isPartial());
        assertTrue(unrecogAttrib.isTransitive());
        assertArrayEquals(ByteArray.cutBytes(attributeBytes, NON_VALUE_BYTES), unrecogAttrib.getValue());
        assertEquals(NON_EXISTENT_TYPE, unrecogAttrib.getType().shortValue());
        assertEquals(expectedAttribKey, unrecogAttrib.getKey());
    }
}
