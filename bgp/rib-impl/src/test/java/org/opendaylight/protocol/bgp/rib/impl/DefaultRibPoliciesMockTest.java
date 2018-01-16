/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.Collections;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.BGPOpenConfigRIBPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPOpenconfigRIBRoutingPolicyProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public class DefaultRibPoliciesMockTest extends AbstractConcurrentDataBrokerTest {
    private final int as = 64496;
    private final Ipv4Address bgpID = new BgpId(new Ipv4Address("127.0.0.1"));
    private final ClusterIdentifier ci = new ClusterIdentifier(new Ipv4Address("1.2.3.4"));
    protected BGPRibRoutingPolicy policies;
    @Mock
    private OpenconfigPolicyConsumer policyConsumer;
    @Mock
    private StatementRegistryConsumer statementRegistry;
    @Mock
    private Config config;
    protected BGPOpenconfigRIBRoutingPolicyProvider policyProvider;

    @Before
    public void setUp() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        doReturn(DefaultPolicyType.REJECTROUTE).when(this.config).getDefaultImportPolicy();
        doReturn(DefaultPolicyType.REJECTROUTE).when(this.config).getDefaultExportPolicy();
        doReturn(Collections.singletonList("default-odl-import-policy")).when(this.config).getExportPolicy();
        doReturn(Collections.singletonList("default-odl-export-policy")).when(this.config).getImportPolicy();

        doReturn(DefaultPolicyType.REJECTROUTE).when(this.policyConsumer).getPolicy(eq("default-odl-export-policy"));


        this.policyProvider = new BGPOpenConfigRIBPolicy(this.policyConsumer, this.statementRegistry);
        this.policies = policyProvider.buildBGPRibPolicy(this.as, this.bgpID, this.ci, this.config);

    }
}
