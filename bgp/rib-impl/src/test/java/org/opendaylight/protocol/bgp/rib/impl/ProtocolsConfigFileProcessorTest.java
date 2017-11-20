/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.protocol.bgp.rib.impl.ProtocolsConfigFileProcessor.BGP_PROTOCOLS_IID;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.protocol.bgp.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ProtocolsConfigFileProcessorTest extends AbstractConfigLoader {
    @Override
    protected void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception {
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(NetworkInstances.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocols.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocol.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocol1.class));
    }

    @Override
    protected List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList(
                "/META-INF/yang/openconfig-extensions.yang",
                "/META-INF/yang/bgp-openconfig-extensions.yang",
                "/META-INF/yang/ietf-interfaces.yang",
                "/META-INF/yang/openconfig-network-instance-types.yang",
                "/META-INF/yang/openconfig-interfaces.yang",
                "/META-INF/yang/openconfig-network-instance.yang",
                "/META-INF/yang/openconfig-routing-policy.yang",
                "/META-INF/yang/openconfig-policy-types.yang",
                "/META-INF/yang/openconfig-local-routing.yang",
                "/META-INF/yang/openconfig-bgp-operational.yang",
                "/META-INF/yang/openconfig-bgp-types.yang",
                "/META-INF/yang/openconfig-types.yang",
                "/META-INF/yang/openconfig-bgp-multiprotocol.yang",
                "/META-INF/yang/openconfig-bgp.yang",
                "/META-INF/yang/bgp-rib.yang",
                "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
                "/META-INF/yang/bgp-message.yang",
                "/META-INF/yang/bgp-multiprotocol.yang",
                "/META-INF/yang/bgp-types.yang",
                "/META-INF/yang/network-concepts.yang",
                "/META-INF/yang/ieee754.yang",
                "/META-INF/yang/ietf-yang-types@2013-07-15.yang",
                "/META-INF/yang/yang-ext.yang"
        );
        return paths;
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