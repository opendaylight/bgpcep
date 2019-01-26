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
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.ImportAttributeTestUtil.AS;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.PolicyRIBBaseParametersImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeImportPolicyContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public class ImportDefaultStatementTest extends AbstractStatementRegistryConsumerTest {
    private static final InstanceIdentifier<Attributes> ATTRIBUTES_IID = InstanceIdentifier.create(Update.class)
        .child(Attributes.class);
    @Mock
    private BGPRouteEntryImportParameters importParameters;
    private List<Statement> defaultImportStatements;
    private PolicyRIBBaseParametersImpl baseAttributes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.defaultImportStatements = loadStatement("default-odl-import-policy");
        this.baseAttributes = new PolicyRIBBaseParametersImpl(LOCAL_AS, IPV4, CLUSTER);
    }

    @Test
    public void testFromEbgp() {
        final Statement statement = getStatement("from-external");
        final ContainerNode input = (ContainerNode) this.mappingService.toNormalizedNode(
            ATTRIBUTES_IID, ImportAttributeTestUtil.createInput()).getValue();

        final RouteAttributeImportPolicyContainer attributeContainer
            = RouteAttributeImportPolicyContainer.routeAttributeContainerFalse(input);

        final ContainerNode output = (ContainerNode) this.mappingService.toNormalizedNode(
            ATTRIBUTES_IID, ImportAttributeTestUtil.createOutput()).getValue();

        assertApplyImportStatement(statement, PeerRole.Ebgp, attributeContainer, output);
    }

    @Test
    public void testFromNonExternal() {
        final Statement statement = getStatement("from-non-external");

        final ContainerNode expected = (ContainerNode)
            this.mappingService.toNormalizedNode(ATTRIBUTES_IID, ImportAttributeTestUtil.createInput());
        final RouteAttributeImportPolicyContainer attributeContainer
            = RouteAttributeImportPolicyContainer.routeAttributeContainerFalse(expected);

        assertApplyImportStatement(statement, PeerRole.Ibgp, attributeContainer, expected);
        assertApplyImportStatement(statement, PeerRole.RrClient, attributeContainer, expected);
        assertApplyImportStatement(statement, PeerRole.Internal, attributeContainer, expected);
    }

    private Statement getStatement(final String statementName) {
        return this.defaultImportStatements.stream()
            .filter(st -> st.getName().equals(statementName)).findFirst().get();
    }

    private void assertApplyImportStatement(
        final Statement statement,
        final PeerRole fromPeerRole,
        final RouteAttributeImportPolicyContainer attInput,
        final ContainerNode attExpected) {

        doReturn(fromPeerRole).when(this.importParameters).getFromPeerRole();
        doReturn(AS).when(this.importParameters).getFromPeerLocalAs();

        RouteAttributeImportPolicyContainer result = this.statementRegistry.applyImportStatement(
            this.baseAttributes,
            IPV4UNICAST.class,
            this.importParameters,
            attInput,
            statement);
        assertEquals(attExpected, result.getAttributes());
    }
}