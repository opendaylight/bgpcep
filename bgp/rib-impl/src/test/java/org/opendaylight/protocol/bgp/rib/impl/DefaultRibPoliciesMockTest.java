/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.BGPRibRoutingPolicyFactoryImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractStatementRegistryConsumerTest;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;

public class DefaultRibPoliciesMockTest extends AbstractStatementRegistryConsumerTest {
    protected static final long AS = 64496;
    private final BgpId bgpID = new BgpId(new Ipv4AddressNoZone("127.0.0.1"));
    private final ClusterIdentifier ci = new ClusterIdentifier(new Ipv4AddressNoZone("127.0.0.1"));
    protected BGPRibRoutingPolicy policies;
    protected BGPRibRoutingPolicyFactory policyProvider;
    @Mock
    protected BGPTableTypeRegistryConsumer tableRegistry;
    @Mock
    private Config config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(DefaultPolicyType.REJECTROUTE).when(this.config).getDefaultImportPolicy();
        doReturn(DefaultPolicyType.REJECTROUTE).when(this.config).getDefaultExportPolicy();
        doReturn(Collections.singletonList("default-odl-import-policy")).when(this.config).getImportPolicy();
        doReturn(Collections.singletonList("default-odl-export-policy")).when(this.config).getExportPolicy();
        doReturn(Optional.of(IPV4UNICAST.class)).when(this.tableRegistry).getAfiSafiType(any(TablesKey.class));

        this.policyProvider = new BGPRibRoutingPolicyFactoryImpl(getDataBroker(), this.statementRegistry);
        this.policies = this.policyProvider.buildBGPRibPolicy(AS, this.bgpID, this.ci, this.config);
    }
}
