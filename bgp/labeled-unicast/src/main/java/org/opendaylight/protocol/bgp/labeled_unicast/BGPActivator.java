package org.opendaylight.protocol.bgp.labeled_unicast;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator{

    private static final int LABELED_UNICAST_SAFI = 4;
    @Override
    protected List<AutoCloseable> startImpl(BGPExtensionProviderContext context) {
        // TODO Auto-generated method stub
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerSubsequentAddressFamily(LabeledUnicastSubsequentAddressFamily.class, LABELED_UNICAST_SAFI));

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, new LUNlriParser()));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, new LUNlriParser()));

        regs.add(context.registerNlriSerializer(LabeledUnicastRoutes.class, new LUNlriParser()));


        return regs;
    }

}
