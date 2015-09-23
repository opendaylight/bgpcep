/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.ToOpenConfigUtil.BGP_IID;

import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.util.ToOpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPGlobalProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPGlobalProviderImpl implements BGPGlobalProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPGlobalProviderImpl.class);
    private static final InstanceIdentifier<Global> GLOBAL_IID = BGP_IID.child(Global.class);

    private final BGPConfigHolder<String, Global> globalConfig;
    private final BindingTransactionChain txChain;

    public BGPGlobalProviderImpl(final BindingTransactionChain txChain, final BGPConfigHolder<String, Global> bgpConfigHolder) {
        this.txChain = txChain;
        this.globalConfig = bgpConfigHolder;
    }

    @Override
    public synchronized void writeGlobal(final String instanceName, final AsNumber localAs, final Ipv4Address bgpRibId, final Ipv4Address clusterId,
            final List<BgpTableType> bgpTables) {
        final Global newGlobal = ToOpenConfigUtil.toGlobalConfiguration(localAs, bgpRibId, clusterId, bgpTables);
        LOG.info("new Global configuration: {}", newGlobal);
        if (globalConfig.addOrUpdate(createModuleKey(instanceName), ToOpenConfigUtil.BGP_GLOBAL_ID, newGlobal)) {
            writeGlobalConfiguration(newGlobal);
            LOG.info("Writing new Global configuration: {}", newGlobal);
        }
    }

    @Override
    public synchronized void removeGlobal(final String instanceName) {
        LOG.info("remove Global configuration: {}", instanceName);
        if (globalConfig.remove(createModuleKey(instanceName))) {
            removeGlobalConfiguration();
            LOG.info("removing Global configuration: {}", instanceName);
        }
    }

    @Override
    public void close() throws Exception {
        // TODO
    }

    private void writeGlobalConfiguration(final Global global) {
        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, GLOBAL_IID, global);
        wTx.submit();
    }

    private void removeGlobalConfiguration() {
        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, GLOBAL_IID);
        wTx.submit();
    }

    private static ModuleKey createModuleKey(final String instanceName) {
        return new ModuleKey(instanceName, RibImpl.class);
    }

}
