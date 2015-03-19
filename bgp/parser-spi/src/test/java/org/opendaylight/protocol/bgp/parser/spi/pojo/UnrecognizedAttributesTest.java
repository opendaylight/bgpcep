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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.UnrecognizedAttributesKey;

public class UnrecognizedAttributesTest {

    private final int UNRECOGNIZED_ATTRIBUTE_COUNT = 1;
    private final int FIRST_ATTRIBUTE = 0;
    private final short NON_EXISTENT_TYPE = 0;
    private final int NON_VALUE_BYTES = 3;

    private final SimpleAttributeRegistry simpleAttrReg = new SimpleAttributeRegistry();

    @Rule
    public ExpectedException expException = ExpectedException.none();

    @Test
    public void testUnrecognizedAttributesWithoutOptionalFlag() throws BGPDocumentedException, BGPParsingException {
        expException.expect(BGPDocumentedException.class);
        expException.expectMessage("Well known attribute not recognized.");
        simpleAttrReg.parseAttributes(Unpooled.wrappedBuffer(new byte[] { 0x03, 0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 }));
    }

    @Test
    public void testUnrecognizedAttributes() throws BGPDocumentedException, BGPParsingException {
        final byte[] attributeBytes = { (byte)0xe0, 0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 };
        final List<UnrecognizedAttributes> unrecogAttribs = simpleAttrReg.parseAttributes(Unpooled.wrappedBuffer(attributeBytes)).getUnrecognizedAttributes();
        assertEquals(UNRECOGNIZED_ATTRIBUTE_COUNT, unrecogAttribs.size());
        final UnrecognizedAttributes unrecogAttrib = unrecogAttribs.get(FIRST_ATTRIBUTE);
        final UnrecognizedAttributesKey EXPECTED_ATTRIB_KEY = new UnrecognizedAttributesKey(unrecogAttrib.getType().shortValue());

        assertTrue(unrecogAttrib.isPartial());
        assertTrue(unrecogAttrib.isTransitive());
        assertArrayEquals(ByteArray.cutBytes(attributeBytes, NON_VALUE_BYTES), unrecogAttrib.getValue());
        assertEquals(NON_EXISTENT_TYPE, unrecogAttrib.getType().shortValue());
        assertEquals(EXPECTED_ATTRIB_KEY, unrecogAttrib.getKey());
    }
}
