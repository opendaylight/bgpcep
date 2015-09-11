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
import org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModuleTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class LinkstateTopologyBuilderModuleTest extends AbstractRIBImplModuleTest {

    private static final String FACTORY_NAME = LinkstateTopologyBuilderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bgp-linkstate-topology-instance";

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new LinkstateTopologyBuilderModuleFactory());
        return moduleFactories;
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/l3-unicast-igp-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/bgp-linkstate.yang");
        paths.add("/META-INF/yang/bgp-segment-routing.yang");
        paths.add("/META-INF/yang/rsvp.yang");
        paths.add("/META-INF/yang/iana.yang");
        paths.add("/META-INF/yang/bgp-epe.yang");
        paths.add("/META-INF/yang/iana-afn-safi@2013-07-04.yang");
        paths.add("/META-INF/yang/bmp-monitor.yang");
        paths.add("/META-INF/yang/bmp-message.yang");
        paths.add("/META-INF/yang/ietf-yang-types.yang");
        return paths;
    }

    @Test
    public void testValidationExceptionTopologyIdNotSet() throws Exception {
        try {
            createLinkstateTopologyBuilderModuleInstance(null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createLinkstateTopologyBuilderModuleInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createLinkstateTopologyBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
    }

    @Test
    public void testReconfigure() throws Exception {
        createLinkstateTopologyBuilderModuleInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final LinkstateTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME,
                INSTANCE_NAME), LinkstateTopologyBuilderModuleMXBean.class);
        mxBean.setTopologyId(new TopologyId("new-bgp-topology"));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 13);
    }

    private CommitStatus createLinkstateTopologyBuilderModuleInstance() throws Exception {
        return createLinkstateTopologyBuilderModuleInstance(new TopologyId("bgp-topology"));
    }

    private CommitStatus createLinkstateTopologyBuilderModuleInstance(final TopologyId topologyId) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName linkstateTopoBuilderON = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final LinkstateTopologyBuilderModuleMXBean mxBean = transaction.newMXBeanProxy(linkstateTopoBuilderON,
                LinkstateTopologyBuilderModuleMXBean.class);
        final ObjectName dataBrokerON = createAsyncDataBrokerInstance(transaction);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setLocalRib(createRIBImplModuleInstance(transaction, dataBrokerON));
        mxBean.setTopologyId(topologyId);
        return transaction.commit();
    }
}
