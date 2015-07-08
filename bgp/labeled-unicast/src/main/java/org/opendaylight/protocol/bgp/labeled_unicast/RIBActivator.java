package org.opendaylight.protocol.bgp.labeled_unicast;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public class RIBActivator extends AbstractRIBExtensionProviderActivator{

    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(
            RIBExtensionProviderContext context) {
        List<AutoCloseable> regs = new ArrayList<>();
        regs.add((AutoCloseable)context.registerRIBSupport(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, LabeledUnicastRIBSupport.getInstance()));
        regs.add((AutoCloseable)context.registerRIBSupport(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, LabeledUnicastRIBSupport.getInstance()));
        return regs;
    }

}
