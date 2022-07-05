/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.EncapsulationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunityBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SetExtCommunityTest extends AbstractStatementRegistryConsumerTest {
    private final Attributes multipleExtCom = new AttributesBuilder().setExtendedCommunities(Arrays.asList(
            new ExtendedCommunitiesBuilder().setExtendedCommunity(new EncapsulationCaseBuilder()
                    .setEncapsulationExtendedCommunity(new EncapsulationExtendedCommunityBuilder()
                            .setTunnelType(Vxlan).build()).build()).build(),
            new ExtendedCommunitiesBuilder().setExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder()
                    .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                            .setAs4SpecificCommon(new As4SpecificCommonBuilder()
                                    .setLocalAdministrator(Uint16.valueOf(123))
                                    .setAsNumber(new AsNumber(Uint32.valueOf(65000))).build())
                            .build()).build()).build())).build();
    private final Attributes emptyExtCom = new AttributesBuilder()
            .setExtendedCommunities(Collections.emptyList()).build();
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("set-ext-community-statements-test");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
        doReturn(PeerRole.Ibgp).when(exportParameters).getFromPeerRole();
    }

    @Test
    public void testInlineAdd() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-inline-add-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(multipleExtCom, result.getAttributes());
    }

    @Test
    public void testInlineReplace() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-inline-replace-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(multipleExtCom, result.getAttributes());
    }

    @Test
    public void testInlineRemove() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-inline-remove-test")).findFirst().get();

        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(multipleExtCom);
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(emptyExtCom, result.getAttributes());
    }

    @Test
    public void testReferenceAdd() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-reference-add-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(multipleExtCom, result.getAttributes());
    }

    @Test
    public void testReferenceReplace() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-reference-replace-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(multipleExtCom, result.getAttributes());
    }

    @Test
    public void testReferenceRemove() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("set-ext-community-reference-remove-test")).findFirst().get();

        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(multipleExtCom);
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);

        assertEquals(emptyExtCom, result.getAttributes());
    }
}
