/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImplBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.rib.impl.LocalTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.rib.impl.LocalTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPRibImplProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BGPRibImplProvider.class);

    private static final Function<String, LocalTable> LOCAL_TABLE_FUNCTION = new Function<String, LocalTable>() {
        @Override
        public LocalTable apply(final String input) {
            return new LocalTableBuilder().setName(input).setType(BgpTableType.class).build();
        }
    };

    private final BGPConfigHolder<Global> globalState;
    private final BGPConfigModuleProvider configModuleWriter;
    private final DataBroker dataBroker;

    public BGPRibImplProvider(final BGPConfigStateStore configHolders, final BGPConfigModuleProvider configModuleWriter, final DataBroker dataBroker) {
        this.globalState = Preconditions.checkNotNull(configHolders.getBGPConfigHolder(Global.class));
        this.configModuleWriter = Preconditions.checkNotNull(configModuleWriter);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    public void onGlobalRemoved(final Global removedGlobal) {
        final ModuleKey moduleKey = this.globalState.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER);
        if (moduleKey != null) {
            try {
                //this.globalState.remove(moduleKey);
                final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
                final Optional<Module> maybeModule = this.configModuleWriter.readModuleConfiguration(moduleKey, rwTx).get();
                if (maybeModule.isPresent() && globalState.remove(moduleKey, removedGlobal)) {
                    this.configModuleWriter.removeModuleConfiguration(moduleKey, rwTx);
                }
            } catch (final Exception e) {
                LOG.error("Failed to remove a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    public void onGlobalModified(final Global modifiedGlobal) {
        final ModuleKey moduleKey = this.globalState.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER);
        if (moduleKey != null && this.globalState.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, modifiedGlobal)) {
            final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            try {
                final Optional<Module> maybeModule = this.configModuleWriter.readModuleConfiguration(moduleKey, rTx).get();
                if (maybeModule.isPresent()) {
                    final ListenableFuture<List<LocalTable>> localTablesFuture = new TableTypesFunction<LocalTable>(rTx, this.configModuleWriter, LOCAL_TABLE_FUNCTION).apply(modifiedGlobal.getAfiSafis().getAfiSafi());
                    final Module newModule = toRibImplConfigModule(modifiedGlobal, maybeModule.get(), localTablesFuture.get());
                    this.configModuleWriter.putModuleConfiguration(newModule, dataBroker.newWriteOnlyTransaction());
                }
            } catch (final Exception e) {
                LOG.error("Failed to update a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    private static Module toRibImplConfigModule(final Global globalConfig, final Module module, final List<LocalTable> tableTypes) {
        final RibImpl ribImpl = (RibImpl) module.getConfiguration();
        final RibImplBuilder ribImplBuilder = new RibImplBuilder();
        if (globalConfig.getConfig() != null) {
            ribImplBuilder.setBgpRibId(new Ipv4Address(globalConfig.getConfig().getRouterId().getValue()));
            ribImplBuilder.setLocalAs(globalConfig.getConfig().getAs().getValue());
        }
        ribImplBuilder.setLocalTable(tableTypes);
        ribImplBuilder.setBgpDispatcher(ribImpl.getBgpDispatcher());
        ribImplBuilder.setClusterId(ribImpl.getClusterId());
        ribImplBuilder.setCodecTreeFactory(ribImpl.getCodecTreeFactory());
        ribImplBuilder.setDataProvider(ribImpl.getDataProvider());
        ribImplBuilder.setDomDataProvider(ribImpl.getDomDataProvider());
        ribImplBuilder.setExtensions(ribImpl.getExtensions());
        ribImplBuilder.setRibId(ribImpl.getRibId());
        ribImplBuilder.setSessionReconnectStrategy(ribImpl.getSessionReconnectStrategy());
        ribImplBuilder.setTcpReconnectStrategy(ribImpl.getTcpReconnectStrategy());
        ribImplBuilder.setOpenconfigProvider(ribImpl.getOpenconfigProvider());

        final ModuleBuilder mBuilder = new ModuleBuilder(module);
        mBuilder.setConfiguration(ribImplBuilder.build());
        return mBuilder.build();
    }

}
