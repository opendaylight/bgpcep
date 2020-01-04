/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class ImportAttributeTestUtil {
    static final AsNumber AS = new AsNumber(Uint32.valueOf(65));

    private ImportAttributeTestUtil() {
        // Hidden on purpose
    }

    public static Attributes createInput() {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        // local pref
        attBuilder.setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build());

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

        // as path
        attBuilder.setAsPath(new AsPathBuilder().build());

        // multi-exit-disc pref
        attBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build());
        return attBuilder.build();
    }

    public static Attributes createOutput() {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        attBuilder.setCNextHop(createNexHop());
        attBuilder.setOrigin(createOrigin());
        attBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Collections.singletonList(AS)).build())).build());
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
