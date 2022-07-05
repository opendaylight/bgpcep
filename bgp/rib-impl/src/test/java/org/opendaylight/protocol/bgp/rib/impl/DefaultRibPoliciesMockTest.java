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

import java.util.List;
import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.DefaultBGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractStatementRegistryConsumerTest;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;

public class DefaultRibPoliciesMockTest extends AbstractStatementRegistryConsumerTest {
    protected static final long AS = 64496;
    private final BgpId bgpID = new BgpId(new Ipv4AddressNoZone("127.0.0.1"));
    private final ClusterIdentifier ci = new ClusterIdentifier(new Ipv4AddressNoZone("127.0.0.1"));
    protected BGPRibRoutingPolicy policies;
    protected BGPRibRoutingPolicyFactory policyProvider;
    protected AdapterContext mappingService;
    @Mock
    protected BGPTableTypeRegistryConsumer tableRegistry;
    @Mock
    private Config config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(DefaultPolicyType.REJECTROUTE).when(config).getDefaultImportPolicy();
        doReturn(DefaultPolicyType.REJECTROUTE).when(config).getDefaultExportPolicy();
        doReturn(List.of("default-odl-import-policy")).when(config).getImportPolicy();
        doReturn(List.of("default-odl-export-policy")).when(config).getExportPolicy();
        doReturn(IPV4UNICAST.VALUE).when(tableRegistry).getAfiSafiType(any(TablesKey.class));

        policyProvider = new DefaultBGPRibRoutingPolicyFactory(getDataBroker(), statementRegistry);
        policies = policyProvider.buildBGPRibPolicy(AS, bgpID, ci, config);
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        mappingService = customizer.getAdapterContext();
        return customizer;
    }
}
