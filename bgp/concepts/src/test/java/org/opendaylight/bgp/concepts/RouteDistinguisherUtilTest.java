package org.opendaylight.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.AdministratorSubfield;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.Ipv4CaseBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class RouteDistinguisherUtilTest {

    private static final Ipv4Address IP_ADDRESS = new Ipv4Address("1.2.3.4");
    private static final byte[] IP_BYTES = { 0, 1, 1, 2, 3, 4, 0, 10 };
    private static final byte[] AS_2B_BYTES = { 0, 0, 0, 33, 0, 0, 0, 9 };
    private static final byte[] AS_4B_BYTES = { 0, 2, 0,0, 0, 55, 0, 8 };

    @Test
    public void testIpv4RouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(1, 10L, new Ipv4CaseBuilder().setIpv4Address(IP_ADDRESS).build());
        final RouteDistinguisher parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(IP_BYTES));
        assertEquals(expected.getType(), parsed.getType());
        assertEquals(expected.getAdministratorSubfield(), parsed.getAdministratorSubfield());
        assertEquals(expected.getAssignedNumberSubfield(), parsed.getAssignedNumberSubfield());
        final ByteBuf byteAggregator = Unpooled.buffer(IP_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(IP_BYTES, byteAggregator.array());
    }

    @Test
    public void testAs2BRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(0, 9L, new AsNumberCaseBuilder().setAsNumber(new AsNumber(33L)).build());
        final RouteDistinguisher parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_2B_BYTES));
        assertEquals(expected.getType(), parsed.getType());
        assertEquals(expected.getAdministratorSubfield(), parsed.getAdministratorSubfield());
        assertEquals(expected.getAssignedNumberSubfield(), parsed.getAssignedNumberSubfield());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_2B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_2B_BYTES, byteAggregator.array());
    }

    @Test
    public void testAs4BRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(2, 8L, new AsNumberCaseBuilder().setAsNumber(new AsNumber(55L)).build());
        final RouteDistinguisher parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_4B_BYTES));
        assertEquals(expected.getType(), parsed.getType());
        assertEquals(expected.getAdministratorSubfield(), parsed.getAdministratorSubfield());
        assertEquals(expected.getAssignedNumberSubfield(), parsed.getAssignedNumberSubfield());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_4B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_4B_BYTES, byteAggregator.array());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<RouteDistinguisherUtil> c = RouteDistinguisherUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private RouteDistinguisher createRouteDistinguisher(final int type, final long assignedNumberSubfield, final AdministratorSubfield administratorSubfield) {
        return new RouteDistinguisher() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }
            @Override
            public Integer getType() {
                return type;
            }
            @Override
            public Long getAssignedNumberSubfield() {
                return assignedNumberSubfield;
            }
            @Override
            public AdministratorSubfield getAdministratorSubfield() {
                return administratorSubfield;
            }
        };
    }

}
