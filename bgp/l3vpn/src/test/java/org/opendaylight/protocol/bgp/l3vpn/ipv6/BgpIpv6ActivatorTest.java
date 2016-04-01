package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import static org.junit.Assert.*;
import org.junit.Test;
import org.opendaylight.protocol.bgp.l3vpn.ipv4.VpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;

/**
 * @author Kevin Wang
 */
public class BgpIpv6ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BgpIpv6Activator act = new BgpIpv6Activator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertFalse(context.getNlriRegistry().getSerializers().iterator().hasNext());
        act.start(context);
        assertTrue(context.getNlriRegistry().getSerializers().iterator().next() instanceof VpnIpv6NlriParser);
        act.close();
    }
}