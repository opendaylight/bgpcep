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
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ImportAttributeTestUtil.AS;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;

public class ImportDefaultStatementTest extends AbstractStatementRegistryConsumerTest {
    @Mock
    private BGPRouteEntryImportParameters importParameters;
    private List<Statement> defaultImportStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        defaultImportStatements = loadStatement("default-odl-import-policy");
        baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testFromEbgp() {
        final Statement statement = getStatement("from-external");

        final RouteAttributeContainer attributeContainer
                = routeAttributeContainerFalse(ImportAttributeTestUtil.createInput());

        assertApplyImportStatement(statement, PeerRole.Ebgp, attributeContainer,
                ImportAttributeTestUtil.createOutput());
    }

    @Test
    public void testFromNonExternal() {
        final Statement statement = getStatement("from-non-external");

        final Attributes expected = ImportAttributeTestUtil.createInput();
        final RouteAttributeContainer attributeContainer
                = routeAttributeContainerFalse(expected);

        assertApplyImportStatement(statement, PeerRole.Ibgp, attributeContainer, expected);
        assertApplyImportStatement(statement, PeerRole.RrClient, attributeContainer, expected);
        assertApplyImportStatement(statement, PeerRole.Internal, attributeContainer, expected);
    }

    private Statement getStatement(final String statementName) {
        return defaultImportStatements.stream()
                .filter(st -> st.getName().equals(statementName)).findFirst().get();
    }

    private void assertApplyImportStatement(
            final Statement statement,
            final PeerRole fromPeerRole,
            final RouteAttributeContainer attInput,
            final Attributes attExpected) {

        doReturn(fromPeerRole).when(importParameters).getFromPeerRole();
        doReturn(AS).when(importParameters).getFromPeerLocalAs();

        RouteAttributeContainer result = statementRegistry.applyImportStatement(
                baseAttributes, IPV4UNICAST.VALUE, importParameters, attInput, statement);
        assertEquals(attExpected, result.getAttributes());
    }
}