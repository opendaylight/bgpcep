/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.topology.provider;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.function.Function;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.reachability.ipv6.Ipv6ReachabilityTopologyBuilderModuleFactory;
import org.opendaylight.controller.config.yang.bgp.reachability.ipv6.Ipv6ReachabilityTopologyBuilderModuleMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModuleTest;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("ALL")
public class Ipv6ReachabilityTopologyBuilderModuleTest extends AbstractRIBImplModuleTest {

    private static final String FACTORY_NAME = Ipv6ReachabilityTopologyBuilderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bgp-reachability-ipv6-instance";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setupMockService(TopologyReference.class, new TopologyReference() {
            @Override
            public InstanceIdentifier<Topology> getInstanceIdentifier() {
                return null;
            }
        });
        setupMockService(BgpTopologyDeployer.class, new BgpTopologyDeployer() {
            @Override
            public AbstractRegistration registerTopologyProvider(final BgpTopologyProvider topologyBuilder) {
                return null;
            }
            @Override
            public DataBroker getDataBroker() {
                return null;
            }
            @Override
            public AbstractRegistration registerService(final TopologyReferenceSingletonService topologyProviderService) {
                return null;
            }
            @Override
            public void createInstance(final Topology topology, final Function<Topology, Void> writeFunction) {
                return;
            }

            @Override
            public void removeInstance(final Topology topology) {
                return;
            }
        });
    }

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new Ipv6ReachabilityTopologyBuilderModuleFactory());
        return moduleFactories;
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/l3-unicast-igp-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/bgp-inet.yang");
        paths.add("/META-INF/yang/bmp-monitor.yang");
        paths.add("/META-INF/yang/bmp-message.yang");
        paths.add("/META-INF/yang/ietf-yang-types@2013-07-15.yang");
        paths.add("/META-INF/yang/odl-bgp-topology-types.yang");
        paths.add("/META-INF/yang/odl-bgp-topology-config.yang");
        return paths;
    }

    @Test
    public void testValidationExceptionTopology() throws Exception {
        try {
            createIpv6ReachabilityTopoBuilderModuleInstance(null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createIpv6ReachabilityTopoBuilderModuleInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 8, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createIpv6ReachabilityTopoBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 8);
    }

    @Test
    public void testReconfigure() throws Exception {
        createIpv6ReachabilityTopoBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final Ipv6ReachabilityTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME,
                INSTANCE_NAME), Ipv6ReachabilityTopologyBuilderModuleMXBean.class);
        mxBean.setTopologyId(new TopologyId("new-bgp-topology"));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 7);
    }

    private CommitStatus createIpv6ReachabilityTopoBuilderModuleInstance() throws Exception {
        return createIpv6ReachabilityTopoBuilderModuleInstance(new TopologyId("bgp-topology"));
    }

    private CommitStatus createIpv6ReachabilityTopoBuilderModuleInstance(final TopologyId topologyId) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName ipv6ReachabilityBuilderON = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final Ipv6ReachabilityTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(ipv6ReachabilityBuilderON,
                Ipv6ReachabilityTopologyBuilderModuleMXBean.class);
        final ObjectName dataBrokerON = createAsyncDataBrokerInstance(transaction);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setLocalRib(createRIBImplModuleInstance(transaction, dataBrokerON));
        mxBean.setTopologyId(topologyId);
        return transaction.commit();
    }
}
