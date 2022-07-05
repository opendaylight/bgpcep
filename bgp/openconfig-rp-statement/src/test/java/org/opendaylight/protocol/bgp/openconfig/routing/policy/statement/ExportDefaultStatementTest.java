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
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.createClusterInput;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.createInputWithOriginator;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.createPathInput;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ExportAttributeTestUtil.createPathInputWithAs;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ImportAttributeTestUtil.AS;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;

public class ExportDefaultStatementTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryExportParameters exportParameters;
    private List<Statement> defaultExportStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        defaultExportStatements = loadStatement("default-odl-export-policy");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testToEbgp() {
        final Statement statement = getStatementAndSetToRole("to-external", PeerRole.Ebgp);

        final Attributes expectedOutput = createPathInputWithAs();
        final RouteAttributeContainer attributeContainer
                = routeAttributeContainerFalse(createPathInput(List.of()));

        assertApplyExportStatement(statement, PeerRole.Ebgp, attributeContainer, expectedOutput);
        assertApplyExportStatement(statement, PeerRole.Ibgp, attributeContainer, expectedOutput);
        assertApplyExportStatement(statement, PeerRole.Internal, attributeContainer, expectedOutput);
        assertApplyExportStatement(statement, PeerRole.RrClient, attributeContainer, expectedOutput);
    }

    @Test
    public void testFromInternalToInternal() {
        final Statement statement = getStatementAndSetToRole("from-internal-to-internal", PeerRole.Ibgp);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.Ibgp, attributeContainer, null);
    }

    @Test
    public void testFromExternalToInternal() {
        final Statement statement = getStatementAndSetToRole("from-external-to-internal", PeerRole.Ibgp);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.Ebgp, attributeContainer, attributeContainer.getAttributes());
    }

    @Test
    public void testFromOdlInternalToInternal() {
        final Statement statement = getStatementAndSetToRole("from-odl-internal-to-internal-or-rr-client",
                PeerRole.Ibgp);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.Internal, attributeContainer, createClusterInput());
    }

    @Test
    public void testFromRRclientToInternal() {
        final Statement statement = getStatementAndSetToRole("from-rr-client-to-internal", PeerRole.Ibgp);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.RrClient, attributeContainer, createInputWithOriginator());
    }

    @Test
    public void testOdlInternal() {
        final Statement statement = getStatementAndSetToRole("to-odl-internal", PeerRole.Internal);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());

        assertApplyExportStatement(statement, PeerRole.Ebgp, attributeContainer, null);
        assertApplyExportStatement(statement, PeerRole.Ibgp, attributeContainer, null);
        assertApplyExportStatement(statement, PeerRole.Internal, attributeContainer, null);
        assertApplyExportStatement(statement, PeerRole.RrClient, attributeContainer, null);
    }

    @Test
    public void testFromExternalToRRClient() {
        final Statement statement = getStatementAndSetToRole("from-external-to-route-reflector", PeerRole.RrClient);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.Ebgp, attributeContainer, attributeContainer.getAttributes());
    }

    @Test
    public void testFromInternalOrRRClientToRRClient() {
        final Statement statement = getStatementAndSetToRole("from-internal-or-rr-client-to-route-reflector",
                PeerRole.RrClient);
        final Attributes expectedOutput = createInputWithOriginator();
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());

        assertApplyExportStatement(statement, PeerRole.Ibgp, attributeContainer, expectedOutput);
        assertApplyExportStatement(statement, PeerRole.RrClient, attributeContainer, expectedOutput);
    }

    @Test
    public void testFromOdlInternalToRRClient() {
        final Statement statement = getStatementAndSetToRole("from-odl-internal-to-internal-or-rr-client",
                PeerRole.RrClient);
        final RouteAttributeContainer attributeContainer = routeAttributeContainerFalse(createClusterInput());
        assertApplyExportStatement(statement, PeerRole.Internal, attributeContainer, createClusterInput());
    }

    private Statement getStatementAndSetToRole(final String statementName, final PeerRole toPeerRole) {
        doReturn(toPeerRole).when(exportParameters).getToPeerRole();
        doReturn(AS).when(exportParameters).getToPeerLocalAs();
        return defaultExportStatements.stream()
                .filter(st -> st.getName().equals(statementName)).findFirst().get();
    }

    private void assertApplyExportStatement(
            final Statement statement, final PeerRole fromPeerRole,
            final RouteAttributeContainer attInput,
            final Attributes attExpected) {
        doReturn(fromPeerRole).when(exportParameters).getFromPeerRole();

        RouteAttributeContainer result = statementRegistry.applyExportStatement(
                baseAttributes, IPV4UNICAST.VALUE, exportParameters, attInput, statement);
        assertEquals(attExpected, result.getAttributes());
    }
}