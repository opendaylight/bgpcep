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
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoaderTest;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.OpenconfigNetworkInstanceData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class ProtocolsConfigFileProcessorTest extends AbstractConfigLoaderTest {
    @VisibleForTesting
    static final InstanceIdentifier<Protocols> BGP_PROTOCOLS_IID =
        InstanceIdentifier.builderOfInherited(OpenconfigNetworkInstanceData.class, NetworkInstances.class).build()
        .child(NetworkInstance.class, new NetworkInstanceKey(ProtocolsConfigFileProcessor.GLOBAL_BGP_NAME))
        .child(Protocols.class);

    @Test
    public void configFileTest() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), BGP_PROTOCOLS_IID);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/protocols-config.xml"));
        final ProtocolsConfigFileProcessor processor = new ProtocolsConfigFileProcessor(this.configLoader,
                getDomBroker());
        processor.init();
        checkPresentConfiguration(getDataBroker(), BGP_PROTOCOLS_IID);

        assertEquals(Absolute.of(NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME),
                processor.fileRootSchema());
        processor.close();
    }
}
