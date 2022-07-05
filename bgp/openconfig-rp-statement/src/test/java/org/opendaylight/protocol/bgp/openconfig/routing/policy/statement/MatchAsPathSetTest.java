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
import java.util.Set;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;

public class MatchAsPathSetTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("match-as-path-set-test");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }


    @Test
    public void testMatchAsPathAny() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-match-as-path-any-set")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder().build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder()
                        .setAsPath(new AsPathBuilder().setSegments(List.of(
                                new SegmentsBuilder().setAsSequence(List.of(
                                        AsNumber.getDefaultInstance("65"))).build())).build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testMatchAsPathAll() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-match-as-path-all-set")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(new AsPathBuilder().setSegments(List.of(
                        new SegmentsBuilder().setAsSequence(List.of(
                                AsNumber.getDefaultInstance("65"))).build())).build()).build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());

        attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder()
                        .setAsPath(new AsPathBuilder().setSegments(List.of(
                                new SegmentsBuilder().setAsSet(Set.of(
                                        AsNumber.getDefaultInstance("65"),
                                        AsNumber.getDefaultInstance("64")
                                )).build(),
                                new SegmentsBuilder().setAsSet(Set.of(
                                        AsNumber.getDefaultInstance("63")
                                )).build()
                        )).build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testMatchAsPathInverse() {
        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("reject-match-as-path-inverse-set")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder()
                        .setAsPath(new AsPathBuilder().setSegments(List.of(
                                new SegmentsBuilder().setAsSequence(List.of(
                                        AsNumber.getDefaultInstance("65"))).build())).build()).build());
        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNotNull(result.getAttributes());


        attributeContainer = routeAttributeContainerFalse(
                new AttributesBuilder()
                        .setAsPath(new AsPathBuilder().setSegments(List.of(
                                new SegmentsBuilder().setAsSequence(List.of(
                                        AsNumber.getDefaultInstance("200"))).build())).build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attributeContainer, statement);
        assertNull(result.getAttributes());
    }
}
