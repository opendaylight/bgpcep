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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class VpnNonMemberHandlerTest extends AbstractStatementRegistryConsumerTest {
    private static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder()
            .setAsNumber(new AsNumber(Uint32.valueOf(20))).setLocalAdministrator(Uint16.valueOf(100)).build();

    private static final As4RouteTargetExtendedCommunity RT = new As4RouteTargetExtendedCommunityBuilder()
            .setAs4SpecificCommon(AS_COMMON).build();
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("vpn-non-member-test");
        this.baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testExtComAny() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("vpn-non-member-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder()
                        .setExtendedCommunities(Collections.singletonList(new ExtendedCommunitiesBuilder()
                                .setExtendedCommunity(new As4RouteTargetExtendedCommunityCaseBuilder()
                                        .setAs4RouteTargetExtendedCommunity(RT).build()).build())).build());

        doReturn(Collections.singletonList(RT)).when(this.exportParameters).getMemberships();

        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        doReturn(Collections.emptyList()).when(this.exportParameters).getMemberships();

        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}