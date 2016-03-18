/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.odl_bgp_benchmark_cfg;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.bgp.benchmark.app.AppPeerBenchmark;
import org.osgi.framework.BundleContext;

public class AppPeerBenchmarkModuleTest extends AbstractDataBrokerTest {

    private static final String INSTANCE_NAME = "instance";

    @Mock
    private DependencyResolver dependencyResolver;
    @Mock
    private BundleContext bundleContextON;
    @Mock
    private ObjectName bindingDataBrokerON;
    @Mock
    private ObjectName rpcRegistryON;
    @Mock
    private RpcProviderRegistry rpcRegistry;

    @Before
    public void setUp() throws MalformedObjectNameException {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(this.rpcRegistry).when(this.dependencyResolver).resolveInstance(Matchers.eq(RpcProviderRegistry.class), Mockito.any(ObjectName.class), Mockito.any(JmxAttribute.class));
        Mockito.doReturn(getDataBroker()).when(this.dependencyResolver).resolveInstance(Matchers.eq(DataBroker.class), Mockito.any(ObjectName.class), Mockito.any(JmxAttribute.class));
    }

    @Test
    public void testCreateInstance() {
        final AppPeerBenchmarkModule module = (AppPeerBenchmarkModule) new AppPeerBenchmarkModuleFactory().createModule(INSTANCE_NAME,
                this.dependencyResolver, this.bundleContextON);
        module.setAppRibId("appRib");
        module.setBindingDataBroker(this.bindingDataBrokerON);
        module.setRpcRegistry(this.rpcRegistryON);
        module.resolveDependencies();
        module.customValidation();
        final AutoCloseable instance = module.createInstance();
        Assert.assertTrue(instance instanceof AppPeerBenchmark);
    }

    @Test(expected=JmxAttributeValidationException.class)
    public void testCustomValidation() {
        final AppPeerBenchmarkModule module = (AppPeerBenchmarkModule) new AppPeerBenchmarkModuleFactory().createModule(INSTANCE_NAME,
                this.dependencyResolver, this.bundleContextON);
        module.setAppRibId(null);
        module.customValidation();
    }

}
