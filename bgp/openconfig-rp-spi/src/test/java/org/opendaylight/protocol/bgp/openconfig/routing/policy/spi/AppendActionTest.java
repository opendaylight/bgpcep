/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

public class AppendActionTest extends AbstractStatementRegistryTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    @Mock
    private RouteEntryBaseAttributes baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("basic-statements-test");
        doReturn(CLUSTER).when(this.baseAttributes).getClusterId();
        doReturn(LOCAL_AS).when(this.baseAttributes).getLocalAs();
        doReturn(IPV4).when(this.baseAttributes).getOriginatorId();
    }


    @Test
    public void testMultipleAppend() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("multiple-append-test")).findFirst().get();
        final RouteAttributeContainer attributeContainer
                = routeAttributeContainerFalse(new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        final Attributes expected = new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(new Ipv4Address("4.5.6.7")).build()).build())
                .setLocalPref(new LocalPrefBuilder().setPref(100L).build())
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(15L).build())
                .build();
        assertEquals(expected, result.getAttributes());
    }

    @Test
    public void testNextHopSelf() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("next-hop-self-append-test")).findFirst().get();
        final RouteAttributeContainer attributeContainer
                = routeAttributeContainerFalse(new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        final Attributes expected = new AttributesBuilder()
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(IPV4).build()).build())
                .build();
        assertEquals(expected, result.getAttributes());
    }
}
