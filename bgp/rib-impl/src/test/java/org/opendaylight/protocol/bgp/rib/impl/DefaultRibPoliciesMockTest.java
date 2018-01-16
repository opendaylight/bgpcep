/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.BGPOpenConfigRIBPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public class DefaultRibPoliciesMockTest extends AbstractConcurrentDataBrokerTest {
    private final int as = 64496;
    private final Ipv4Address bgpID = new BgpId(new Ipv4Address("127.0.0.1"));
    private final ClusterIdentifier ci = new ClusterIdentifier(new Ipv4Address("1.2.3.4"));
    @Mock
    private OpenconfigPolicyConsumer policyConsumer;
    @Mock
    private StatementRegistryConsumer statementRegistry;
    @Mock
    private Config config;
    protected BGPRibRoutingPolicy policies;

    @Before
    public void setUp() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        final BGPOpenConfigRIBPolicy policyProvider
                = new BGPOpenConfigRIBPolicy(this.policyConsumer, this.statementRegistry);
        this.policies = policyProvider
                .buildBGPRibPolicy(this.as, this.bgpID, this.ci, this.config);

    }
}
