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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;

public class AsPathLengthTest extends AbstractStatementRegistryTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    @Mock
    private RouteEntryBaseAttributes baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        basicStatements = loadStatement("basic-statements-test");
        doReturn(CLUSTER).when(baseAttributes).getClusterId();
        doReturn(LOCAL_AS).when(baseAttributes).getLocalAs();
        doReturn(IPV4).when(baseAttributes).getOriginatorId();
    }

    @Test
    public void testASPathLengthEq() {
        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-eq-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }

    @Test
    public void testASPathLengthGe() {
        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-ge-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Collections.singletonList(
                        AsNumber.getDefaultInstance("3")))
                .build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testASPathLengthLe() {
        final AsPathBuilder asPath = new AsPathBuilder();
        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(
                        AsNumber.getDefaultInstance("1"),
                        AsNumber.getDefaultInstance("2"),
                        AsNumber.getDefaultInstance("3")))
                .build()));

        Statement statement = basicStatements.stream()
                .filter(st -> st.getName().equals("as-path-le-length-test")).findFirst().get();
        RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(new AttributesBuilder()
                .setAsPath(asPath.build())
                .build());

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        asPath.setSegments(Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Collections.singletonList(AsNumber.getDefaultInstance("3"))).build()));

        attributeContainer = routeAttributeContainerFalse(new AttributesBuilder().setAsPath(asPath.build()).build());
        result = statementRegistry.applyExportStatement(
                baseAttributes,
                IPV4UNICAST.VALUE,
                exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}
