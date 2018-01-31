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
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.CLUSTER;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.IPV4;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.LOCAL_AS;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.createClusterInput;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class PrefixMatchTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> basicStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.basicStatements = loadStatement("basic-statements-test");
        this.baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testPrefixRange() {
        //RANGE
        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("reject-prefix-test")).findFirst().get();
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());

        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "14.3.191.0/22")))
                .when(this.exportParameters).getRouteId();
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testPrefixExact() {
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("reject-prefix-test")).findFirst().get();

        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.192.0/21")))
                .when(this.exportParameters).getRouteId();
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());

        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "11.3.192.0/21")))
                .when(this.exportParameters).getRouteId();
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());
    }

    @Test
    public void testPrefixInverse() {
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());

        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "10.3.192.0/21")))
                .when(this.exportParameters).getRouteId();
        final Statement statement = this.basicStatements.stream()
                .filter(st -> st.getName().equals("reject-prefix-inverse-test")).findFirst().get();
        RouteAttributeContainer result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNotNull(result.getAttributes());

        doReturn(new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                ImmutableMap.of(QName.create(Ipv4Route.QNAME, "prefix").intern(), "11.3.192.0/21")))
                .when(this.exportParameters).getRouteId();
        result = this.statementRegistry.applyExportStatement(
                this.baseAttributes,
                this.exportParameters,
                attributeContainer,
                statement);
        assertNull(result.getAttributes());
    }
}
