/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

public class MatchBgpNeighborSetTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("bgp-neighbor-statements-test");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testMatchFromBgpNeighborAny() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-from-neighbor-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());

        doReturn(new PeerId("bgp://42.42.42.42")).when(exportParameters).getFromPeerId();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        doReturn(new PeerId("bgp://127.0.0.1")).when(exportParameters).getFromPeerId();
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testMatchFromBgpNeighborInvert() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-from-neighbor-invert-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());

        doReturn(new PeerId("bgp://42.42.42.42")).when(exportParameters).getFromPeerId();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());

        doReturn(new PeerId("bgp://127.0.0.1")).when(exportParameters).getFromPeerId();
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testMatchToBgpNeighborAny() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-to-neighbor-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());

        doReturn(new PeerId("bgp://127.0.0.2")).when(exportParameters).getFromPeerId();
        doReturn(new PeerId("bgp://42.42.42.42")).when(exportParameters).getToPeerId();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        doReturn(new PeerId("bgp://127.0.0.1")).when(exportParameters).getToPeerId();
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testMatchToBgpNeighborInvert() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-to-neighbor-invert-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());

        doReturn(new PeerId("bgp://127.0.0.2")).when(exportParameters).getFromPeerId();
        doReturn(new PeerId("bgp://42.42.42.42")).when(exportParameters).getToPeerId();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());

        doReturn(new PeerId("bgp://127.0.0.1")).when(exportParameters).getToPeerId();
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());
    }
}
