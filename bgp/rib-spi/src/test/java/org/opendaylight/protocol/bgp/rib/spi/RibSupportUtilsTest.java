package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class RibSupportUtilsTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<RibSupportUtils> c = RibSupportUtils.class.getDeclaredConstructor(null);
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void test() {
        final Class afi = Ipv4AddressFamily.class;
        final Class safi = UnicastSubsequentAddressFamily.class;
        final TablesKey k = new TablesKey(afi, safi);
        final NodeIdentifierWithPredicates p = RibSupportUtils.toYangTablesKey(k);
        final Map<QName, Object> m = p.getKeyValues();
        assertFalse(m.isEmpty());
        assertTrue(m.containsValue(BindingReflections.findQName(afi)));
        assertTrue(m.containsValue(BindingReflections.findQName(safi)));
    }
}
