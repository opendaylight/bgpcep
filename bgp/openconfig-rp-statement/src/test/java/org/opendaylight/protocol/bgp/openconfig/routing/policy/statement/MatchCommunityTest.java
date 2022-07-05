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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

public class MatchCommunityTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("community-statements-test");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testComAny() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("community-any-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setCommunities(
                List.of(new CommunitiesBuilder()
                        .setAsNumber(AsNumber.getDefaultInstance("65"))
                        .setSemantics(Uint16.TEN)
                        .build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testComInvert() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("community-invert-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setCommunities(
                List.of(new CommunitiesBuilder()
                        .setAsNumber(AsNumber.getDefaultInstance("65"))
                        .setSemantics(Uint16.TEN)
                        .build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testComAll() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("community-all-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setCommunities(List.of(
                new CommunitiesBuilder().setAsNumber(AsNumber.getDefaultInstance("65"))
                        .setSemantics(Uint16.TEN).build(),
                new CommunitiesBuilder().setAsNumber(AsNumber.getDefaultInstance("66"))
                        .setSemantics(Uint16.valueOf(11)).build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }
}
