/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractBGPOpenConfigMapper<F extends InstanceConfiguration, T extends DataObject> implements BGPOpenconfigMapper<F>, Function<F, T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBGPOpenConfigMapper.class);

    private final BGPConfigHolder<T> configHolder;
    private final BindingTransactionChain txChain;

    protected AbstractBGPOpenConfigMapper(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders, final Class<T> clazz) {
        this.txChain = Preconditions.checkNotNull(txChain);
        this.configHolder = Preconditions.checkNotNull(stateHolders.getBGPConfigHolder(clazz));
    }

    @Override
    public final void writeConfiguration(final F instanceConfiguration) {
        final T openConfig = apply(instanceConfiguration);
        LOG.debug("Update configuration candidate: {} mapped to: {}", instanceConfiguration, openConfig);
        final Identifier key = keyForConfiguration(openConfig);
        final ModuleKey moduleKey = createModuleKey(instanceConfiguration.getIdentifier().getName());
        if (configHolder.addOrUpdate(moduleKey, key, openConfig)) {
            writeConfiguration(key, openConfig);
            LOG.debug("Configuration [{} <-> {}] updated.", key, moduleKey);
        }
    }

    @Override
    public final void removeConfiguration(final F instanceConfiguration) {
        final T openConfig = apply(instanceConfiguration);
        LOG.debug("Remove configuration candidate: {} mapped to: {}", instanceConfiguration, openConfig);
        final ModuleKey moduleKey = createModuleKey(instanceConfiguration.getIdentifier().getName());
        final Identifier key = configHolder.getKey(moduleKey);
        if (configHolder.remove(moduleKey, openConfig)) {
            removeConfiguration(key);
            LOG.debug("Configuration [{} <-> {}] removed", key, moduleKey);
        }
    }

    private void writeConfiguration(final Identifier key, final T openConfig) {
        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, getInstanceIdentifier(key), openConfig);
        wTx.submit();
    }

    private void removeConfiguration(final Identifier key) {
        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, getInstanceIdentifier(key));
        wTx.submit();
    }

    protected abstract ModuleKey createModuleKey(String instanceName);

    protected abstract InstanceIdentifier<T> getInstanceIdentifier(Identifier key);

    protected abstract Identifier keyForConfiguration(final T openConfig);

    protected abstract Class<F> getInstanceConfigurationType();

}
