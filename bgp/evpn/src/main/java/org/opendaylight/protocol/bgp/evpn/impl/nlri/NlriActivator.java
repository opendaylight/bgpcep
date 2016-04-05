package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import java.util.List;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.MacIpAdvRouteCase;

public final class NlriActivator {
    private NlriActivator() {
        throw new UnsupportedOperationException();
    }

    public static void registerNlriParsers(final List<AutoCloseable> regs) {
        final SimpleEvpnNlriRegistry evpnNlriRegistry = SimpleEvpnNlriRegistry.getInstance();

        final EthADRParser ethADR = new EthADRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(NlriType.EthADDisc, ethADR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(EthernetADRouteCase.class, ethADR));

        final MACIpAdvRParser macIpAR = new MACIpAdvRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(NlriType.MacIpAdv, macIpAR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(MacIpAdvRouteCase.class, macIpAR));

        final IncMultEthTagRParser incMultETR = new IncMultEthTagRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(NlriType.IncMultEthTag, incMultETR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(IncMultiEthernetTagResCase.class, incMultETR));

        final EthSegRParser ethSR = new EthSegRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(NlriType.EthSeg, ethSR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(EsRouteCase.class, ethSR));
    }
}
