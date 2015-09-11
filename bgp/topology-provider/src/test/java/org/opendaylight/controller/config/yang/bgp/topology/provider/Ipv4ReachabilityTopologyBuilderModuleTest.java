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
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.reachability.ipv4.Ipv4ReachabilityTopologyBuilderModuleFactory;
import org.opendaylight.controller.config.yang.bgp.reachability.ipv4.Ipv4ReachabilityTopologyBuilderModuleMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModuleTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class Ipv4ReachabilityTopologyBuilderModuleTest extends AbstractRIBImplModuleTest {

    private static final String FACTORY_NAME = Ipv4ReachabilityTopologyBuilderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bgp-reachability-ipv4-instance";

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new Ipv4ReachabilityTopologyBuilderModuleFactory());
        return moduleFactories;
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/l3-unicast-igp-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/bgp-inet.yang");
        paths.add("/META-INF/yang/iana-afn-safi@2013-07-04.yang");
        paths.add("/META-INF/yang/bmp-monitor.yang");
        paths.add("/META-INF/yang/bmp-message.yang");
        paths.add("/META-INF/yang/ietf-yang-types.yang");
        return paths;
    }

    @Test
    public void testValidationExceptionTopologyIdNotSet() throws Exception {
        try {
            createIpv4ReachabilityTopoBuilderModuleInstance(null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createIpv4ReachabilityTopoBuilderModuleInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createIpv4ReachabilityTopoBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
    }

    @Test
    public void testReconfigure() throws Exception {
        createIpv4ReachabilityTopoBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final Ipv4ReachabilityTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME,
                INSTANCE_NAME), Ipv4ReachabilityTopologyBuilderModuleMXBean.class);
        mxBean.setTopologyId(new TopologyId("new-bgp-topology"));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 13);
    }

    private CommitStatus createIpv4ReachabilityTopoBuilderModuleInstance() throws Exception {
        return createIpv4ReachabilityTopoBuilderModuleInstance(new TopologyId("bgp-topology"));
    }

    private CommitStatus createIpv4ReachabilityTopoBuilderModuleInstance(final TopologyId topologyId) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName ipv4ReachabilityBuilderON = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final Ipv4ReachabilityTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(ipv4ReachabilityBuilderON,
                Ipv4ReachabilityTopologyBuilderModuleMXBean.class);
        final ObjectName dataBrokerON = createAsyncDataBrokerInstance(transaction);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setLocalRib(createRIBImplModuleInstance(transaction, dataBrokerON));
        mxBean.setTopologyId(topologyId);
        return transaction.commit();
    }
}
