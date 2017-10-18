/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import java.util.List;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.es.route.EsRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.ethernet.a.d.route.EthernetADRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.MacIpAdvRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.inc.multi.ethernet.tag.res.IncMultiEthernetTagRes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.mac.ip.adv.route.MacIpAdvRoute;

public final class NlriActivator {
    private NlriActivator() {
        throw new UnsupportedOperationException();
    }

    public static void registerNlriParsers(final List<AutoCloseable> regs) {
        final SimpleEvpnNlriRegistry evpnNlriRegistry = SimpleEvpnNlriRegistry.getInstance();

        final EthADRParser ethADR = new EthADRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(ethADR.getType(), ethADR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(EthernetADRouteCase.class, ethADR));
        regs.add(evpnNlriRegistry.registerNlriModelSerializer(EthernetADRoute.QNAME, ethADR));

        final MACIpAdvRParser macIpAR = new MACIpAdvRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(macIpAR.getType(), macIpAR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(MacIpAdvRouteCase.class, macIpAR));
        regs.add(evpnNlriRegistry.registerNlriModelSerializer(MacIpAdvRoute.QNAME, macIpAR));

        final IncMultEthTagRParser incMultETR = new IncMultEthTagRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(incMultETR.getType(), incMultETR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(IncMultiEthernetTagResCase.class, incMultETR));
        regs.add(evpnNlriRegistry.registerNlriModelSerializer(IncMultiEthernetTagRes.QNAME, incMultETR));

        final EthSegRParser ethSR = new EthSegRParser();
        regs.add(evpnNlriRegistry.registerNlriParser(ethSR.getType(), ethSR));
        regs.add(evpnNlriRegistry.registerNlriSerializer(EsRouteCase.class, ethSR));
        regs.add(evpnNlriRegistry.registerNlriModelSerializer(EsRoute.QNAME, ethSR));
    }
}
