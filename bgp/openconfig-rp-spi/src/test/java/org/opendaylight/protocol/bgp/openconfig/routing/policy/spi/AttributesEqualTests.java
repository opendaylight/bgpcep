/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class AttributesEqualTests extends AbstractStatementRegistryTest {
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
    public void testMedEq() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("med-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(200L).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(100L).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testOriginEq() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("origin-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testNextHopIn() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("nexthop-in-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                        .setGlobal(new Ipv6Address("2001:db8::1")).build()).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(new Ipv4Address("42.42.42.42")).build()).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testLocalPref() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("local-pref-eq-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(350L).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(100L).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testAfiSafiIn() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("afi-safi-in-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(350L).build())
                .build());

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV6UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(100L).build())
                .build());
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}
