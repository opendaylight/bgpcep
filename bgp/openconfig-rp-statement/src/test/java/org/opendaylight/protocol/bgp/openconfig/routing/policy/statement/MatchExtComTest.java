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
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.EncapsulationTunnelType.Vxlan;

import java.util.Arrays;
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.EncapsulationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunityBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

public class MatchExtComTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("ext-community-statements-test");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testExtComAny() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-any-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setExtendedCommunities(Collections.singletonList(new ExtendedCommunitiesBuilder()
                        .setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                                .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(Uint16.valueOf(123))
                                                .build()).build()).build()).build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testExtComAll() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-all-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setExtendedCommunities(Arrays.asList(
                new ExtendedCommunitiesBuilder().setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                        .setAs4RouteOriginExtendedCommunity(
                                new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(Uint16.valueOf(123))
                                                .build()).build()).build()).build(),
                new ExtendedCommunitiesBuilder().setExtendedCommunity(new EncapsulationCaseBuilder()
                        .setEncapsulationExtendedCommunity(new EncapsulationExtendedCommunityBuilder()
                                .setTunnelType(Vxlan).build()).build()).build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testExtComInvert() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("ext-community-invert-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setExtendedCommunities(Collections.singletonList(new ExtendedCommunitiesBuilder()
                        .setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                                .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                                        .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                                .setAsNumber(AsNumber.getDefaultInstance("65000"))
                                                .setLocalAdministrator(Uint16.valueOf(123))
                                                .build()).build()).build()).build())).build());

        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());
    }
}