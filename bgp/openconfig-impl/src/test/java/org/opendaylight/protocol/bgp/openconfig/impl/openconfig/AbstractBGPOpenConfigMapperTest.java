/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractBGPOpenConfigMapperTest {

    private static final InstanceConfigurationIdentifier IDENTIFIER = new InstanceConfigurationIdentifier("instanceName");

    private static final InstanceConfiguration INSTANCE_CONFIGURATION = new InstanceConfiguration() {
        @Override
        public InstanceConfigurationIdentifier getIdentifier() {
            return IDENTIFIER;
        }
    };

    private AbstractBGPOpenConfigMapper<InstanceConfiguration, DataObject> mapper;
    @SuppressWarnings("unchecked")
    private final BGPConfigHolder<Neighbor> configHolder = Mockito.mock(BGPConfigHolder.class);
    final WriteTransaction wTx = Mockito.mock(WriteTransaction.class);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setUp() throws Exception {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        Mockito.doNothing().when(wTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doNothing().when(wTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(null).when(wTx).submit();
        Mockito.doReturn(wTx).when(txChain).newWriteOnlyTransaction();
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        Mockito.doReturn(null).when(configHolder).getKey(Mockito.any(ModuleKey.class));
        mapper = new AbstractBGPOpenConfigMapper(txChain, stateHolders, DataObject.class) {
            @Override
            public ModuleKey createModuleKey(final String instanceName) {
                return null;
            }
            @Override
            public Class<InstanceConfiguration> getInstanceConfigurationType() {
                return null;
            }
            @Override
            public Object apply(final Object input) {
                return null;
            }
            @Override
            protected org.opendaylight.yangtools.concepts.Identifier keyForConfiguration(final DataObject openConfig) {
                return null;
            }
            @Override
            protected InstanceIdentifier getInstanceIdentifier(final Identifier key) {
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteConfiguration() {
        Mockito.doReturn(true).when(this.configHolder).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(NeighborKey.class), Mockito.any(Neighbor.class));
        mapper.writeConfiguration(INSTANCE_CONFIGURATION);
        Mockito.verify(wTx, Mockito.times(1)).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));

        Mockito.doReturn(false).when(this.configHolder).addOrUpdate(Mockito.any(ModuleKey.class), Mockito.any(NeighborKey.class), Mockito.any(Neighbor.class));
        mapper.writeConfiguration(INSTANCE_CONFIGURATION);
        Mockito.verify(wTx, Mockito.times(1)).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
    }

    @Test
    public void testRemoveConfiguration() {
        Mockito.doReturn(true).when(this.configHolder).remove(Mockito.any(ModuleKey.class));
        mapper.removeConfiguration(IDENTIFIER);
        Mockito.verify(wTx, Mockito.times(1)).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));

        Mockito.doReturn(false).when(this.configHolder).remove(Mockito.any(ModuleKey.class));
        mapper.removeConfiguration(IDENTIFIER);
        Mockito.verify(wTx, Mockito.times(1)).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));

    }

}
