/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.bgpcep.config.loader.protocols.ProtocolsConfigFileProcessor.BGP_PROTOCOLS_IID;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ProtocolsConfigFileProcessorTest extends AbstractConfigLoader {
    @Override
    protected void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception {
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(NetworkInstance.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocols.class));
    }

    @Test
    public void configFileTest() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), BGP_PROTOCOLS_IID);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/protocols-config.xml"));
        final ProtocolsConfigFileProcessor processor = new ProtocolsConfigFileProcessor(this.configLoader,
                getDataBroker());
        processor.init();
        checkPresentConfiguration(getDataBroker(), BGP_PROTOCOLS_IID);

        assertEquals(SchemaPath.create(true, NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME),
                processor.getSchemaPath());
        processor.close();
    }
}