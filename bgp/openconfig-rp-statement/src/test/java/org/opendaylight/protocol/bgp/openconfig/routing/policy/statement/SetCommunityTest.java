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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;

public class SetCommunityTest extends AbstractStatementRegistryConsumerTest {
    private final Attributes multipleCom = new AttributesBuilder().setCommunities(Arrays.asList(
            new CommunitiesBuilder().setAsNumber(AsNumber.getDefaultInstance("65")).setSemantics(10).build(),
            new CommunitiesBuilder().setAsNumber(AsNumber.getDefaultInstance("66")).setSemantics(11).build()
    )).build();
    private final Attributes emptyCom = new AttributesBuilder().setCommunities(Collections.emptyList()).build();
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("set-community-statements-test");
        this.baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
        doReturn(PeerRole.Ibgp).when(this.exportParameters).getFromPeerRole();
    }

    @Test
    public void testInlineAdd() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-inline-add-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.multipleCom, result.getAttributes());
    }

    @Test
    public void testInlineReplace() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-inline-replace-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.multipleCom, result.getAttributes());
    }

    @Test
    public void testInlineRemove() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-inline-remove-test")).findFirst().get();

        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(this.multipleCom);
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.emptyCom, result.getAttributes());
    }

    @Test
    public void testReferenceAdd() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-reference-add-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.multipleCom, result.getAttributes());
    }

    @Test
    public void testReferenceReplace() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-reference-replace-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.multipleCom, result.getAttributes());
    }

    @Test
    public void testReferenceRemove() {
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("set-community-reference-remove-test")).findFirst().get();

        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(this.multipleCom);
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                IPV4UNICAST.class,
                this.exportParameters,
                attributeContainer,
                statement);

        assertEquals(this.emptyCom, result.getAttributes());
    }
}
