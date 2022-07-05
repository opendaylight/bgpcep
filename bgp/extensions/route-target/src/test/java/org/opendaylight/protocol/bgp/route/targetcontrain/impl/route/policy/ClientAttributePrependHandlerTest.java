/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.route.policy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.AbstractStatementRegistryTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.StatementActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ClientAttributePrependHandlerTest extends AbstractStatementRegistryTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    @Mock
    private RouteEntryBaseAttributes baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final StatementActivator act = new StatementActivator();
        act.start(statementRegistry);
        basicStatements = loadStatement("basic-statements-test");
        doReturn(CLUSTER).when(baseAttributes).getClusterId();
        doReturn(LOCAL_AS).when(baseAttributes).getLocalAs();
        doReturn(IPV4).when(baseAttributes).getOriginatorId();
    }


    @Test
    public void testPreprendClientAttribute() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("client-attribute-append-test")).findFirst().get();
        final Attributes att = new AttributesBuilder()
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(IPV4).build()).build())
                .build();
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(att);
        doReturn(Collections.emptyList()).when(exportParameters).getClientRouteTargetContrainCache();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertEquals(att, result.getAttributes());

        final Attributes expected = new AttributesBuilder()
                .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(new Ipv4AddressNoZone("2.2.2.2")).build()).build())
                .build();
        final String rk = "testRoute";
        final Route rtRoute = new RouteTargetConstrainRouteBuilder()
                .setRouteKey(rk)
                .setPathId(new PathId(Uint32.ZERO))
                .setAttributes(expected)
                .build();
        doReturn(Collections.singletonList(rtRoute)).when(exportParameters).getClientRouteTargetContrainCache();
        doReturn(rk).when(exportParameters).getRouteKey();

        result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertEquals(expected, result.getAttributes());
    }

}