/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import java.util.List;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.ArbitraryCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.AsGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.LacpAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.LanAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.MacAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCase;

public final class ESIActivator {
    private ESIActivator() {
        throw new UnsupportedOperationException();
    }

    public static void registerEsiTypeParsers(final List<AutoCloseable> regs) {
        final SimpleEsiTypeRegistry esiRegistry = SimpleEsiTypeRegistry.getInstance();

        final ArbitraryParser t0Parser = new ArbitraryParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.Arbitrary, t0Parser));
        regs.add(esiRegistry.registerEsiSerializer(ArbitraryCase.class, t0Parser));

        final LacpParser t1Parser = new LacpParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.LacpAutoGenerated, t1Parser));
        regs.add(esiRegistry.registerEsiSerializer(LacpAutoGeneratedCase.class, t1Parser));

        final LanParser t2Parser = new LanParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.LanAutoGenerated, t2Parser));
        regs.add(esiRegistry.registerEsiSerializer(LanAutoGeneratedCase.class, t2Parser));

        final MacParser t3Parser = new MacParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.MacAutoGenerated, t3Parser));
        regs.add(esiRegistry.registerEsiSerializer(MacAutoGeneratedCase.class, t3Parser));

        final RouterIdParser t4Parser = new RouterIdParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.RouterIdGenerated, t4Parser));
        regs.add(esiRegistry.registerEsiSerializer(RouterIdGeneratedCase.class, t4Parser));

        final ASGenParser t5Parser = new ASGenParser();
        regs.add(esiRegistry.registerEsiParser(EsiType.AsGenerated, t5Parser));
        regs.add(esiRegistry.registerEsiSerializer(AsGeneratedCase.class, t5Parser));
    }
}
