package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.MultiProtocolCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class MultiProtocolCapabilityHandlerTest {

    private final static Class afi = Ipv6AddressFamily.class;
    @Mock private AddressFamilyRegistry afir;
    private final static Class safi = UnicastSubsequentAddressFamily.class;
    @Mock private SubsequentAddressFamilyRegistry safir;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(260).when(this.afir).numberForClass(afi);
        Mockito.doReturn(this.afi).when(this.afir).classForFamily(260);
        Mockito.doReturn(4).when(this.safir).numberForClass(safi);
        Mockito.doReturn(this.safi).when(this.safir).classForFamily(4);
    }

    @Test
    public void testCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(this.afi).setSafi(this.safi).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
        final CParameters newCaps = handler.parseCapability(bytes);

        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }
}
