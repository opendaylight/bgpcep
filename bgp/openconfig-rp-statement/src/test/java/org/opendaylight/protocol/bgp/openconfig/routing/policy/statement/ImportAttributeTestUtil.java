/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

public final class ImportAttributeTestUtil {
    private ImportAttributeTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static Attributes createInput() {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        // local pref
        attBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());

        // cluster pref
        attBuilder.setClusterId(new ClusterIdBuilder()
                .setCluster(Collections.singletonList(new ClusterIdentifier("40.40.40.40"))).build());

        // c-next-hop pref
        attBuilder.setCNextHop(createNexHop());

        // originator pref
        attBuilder.setOriginatorId(new OriginatorIdBuilder()
                .setOriginator(new Ipv4Address("41.41.41.41")).build());

        // origin pref
        attBuilder.setOrigin(createOrigin());

        // multi-exit-disc pref
        attBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(0L).build());
        return attBuilder.build();
    }

    public static Attributes createOutput() {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        attBuilder.setCNextHop(createNexHop());
        attBuilder.setOrigin(createOrigin());
        return attBuilder.build();
    }

    private static Origin createOrigin() {
        return new OriginBuilder().setValue(BgpOrigin.Igp).build();
    }

    /**
     * c-next-hop pref.
     */
    private static Ipv4NextHopCase createNexHop() {
        return new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address("199.20.160.41")).build()).build();
    }
}
