/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.ToConfigModuleUtil.isSame;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigModuleProvider;
import org.opendaylight.protocol.bgp.openconfig.impl.util.ToConfigModuleUtil;
import org.opendaylight.protocol.bgp.openconfig.impl.util.ToOpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.rib.impl.LocalTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.rib.impl.LocalTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPOpenConfigListener implements DataTreeChangeListener<Bgp>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenConfigListener.class);

    private final BGPConfigModuleProvider configModuleWriter;
    private final ListenerRegistration<BGPOpenConfigListener> registerDataTreeChangeListener;
    private final DataBroker mpDataBroker;
    private final BGPConfigHolder<String, Global> globalState;

    public BGPOpenConfigListener(final DataBroker dataBroker, final DataBroker mpDataBroker, final BGPConfigStateHolders configStateHolders) {
        this.configModuleWriter = new BGPConfigModuleProviderImpl();
        this.registerDataTreeChangeListener = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, ToOpenConfigUtil.BGP_IID), this);
        this.mpDataBroker = mpDataBroker;
        this.globalState = configStateHolders.getGlobalConfigHolder();
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        for (final DataTreeModification<Bgp> dataTreeModification : changes) {
            final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
            final Collection<DataObjectModification<? extends DataObject>> modifiedChildren = rootNode.getModifiedChildren();
            for (final DataObjectModification<? extends DataObject> dataObjectModification : modifiedChildren) {
                switch (dataObjectModification.getModificationType()) {
                case DELETE:
                    onOpenConfigRemoved(dataObjectModification.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    onOpenConfigModified(dataObjectModification.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + rootNode.getModificationType());
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        registerDataTreeChangeListener.close();
    }

    private void onOpenConfigRemoved(final DataObject removedData) {
        if (removedData instanceof Global) {
            onGlobalRemoved((Global) removedData);
        }
    }

    private synchronized void onGlobalRemoved(final Global removedGlobal) {
        final ReadWriteTransaction rwTx = this.mpDataBroker.newReadWriteTransaction();
        final ModuleKey moduleKey = globalState.getModuleKey(ToOpenConfigUtil.BGP_GLOBAL_ID);
        if (moduleKey != null) {
            final ListenableFuture<Optional<Module>> readFuture = configModuleWriter.readModuleConfiguration(moduleKey, rwTx);
            Futures.addCallback(readFuture, new FutureCallback<Optional<Module>>() {
                @Override
                public void onSuccess(final Optional<Module> result) {
                    if (result.isPresent()) {
                        configModuleWriter.removeModuleConfiguration(moduleKey, rwTx);
                        globalState.remove(moduleKey);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private void onOpenConfigModified(final DataObject modifiedData) {
        LOG.info("modified data: {}", modifiedData);
        if (modifiedData instanceof Global) {
            onGlobalModified((Global) modifiedData);
        }
    }

    private synchronized void onGlobalModified(final Global modifiedGlobal) {
        final ReadWriteTransaction rwTx = this.mpDataBroker.newReadWriteTransaction();
        final ModuleKey moduleKey = globalState.getModuleKey(ToOpenConfigUtil.BGP_GLOBAL_ID);
        if (moduleKey != null) {
            final ListenableFuture<Optional<Module>> readFuture = configModuleWriter.readModuleConfiguration(moduleKey, rwTx);
            Futures.addCallback(readFuture, new FutureCallback<Optional<Module>>() {
                @Override
                public void onSuccess(final Optional<Module> result) {
                    if (result.isPresent()) {
                        final Module currentModule = result.get();
                        final AsyncFunction<List<AfiSafi>, List<LocalTable>> getAfiSafi = new TableTypesFunction(rwTx);
                        try {
                            Futures.addCallback(getAfiSafi.apply(modifiedGlobal.getAfiSafis().getAfiSafi()), new FutureCallback<List<LocalTable>>() {
                                @Override
                                public void onSuccess(final List<LocalTable> localTables) {
                                    final Module newModule = ToConfigModuleUtil.globalConfigToRibImplConfigModule(modifiedGlobal, currentModule, localTables);
                                    LOG.info("New module: {}", newModule.getConfiguration());
                                    LOG.info("current module: {}", currentModule.getConfiguration());
                                    //FIXME compare new global with current module, before converting modified global to the module
                                    if (!isSame((RibImpl) currentModule.getConfiguration(), (RibImpl) newModule.getConfiguration())) {
                                        configModuleWriter.writeModuleConfiguration(newModule, rwTx);
                                        globalState.addOrUpdate(moduleKey, ToOpenConfigUtil.BGP_GLOBAL_ID, modifiedGlobal);
                                    }
                                }
                                @Override
                                public void onFailure(final Throwable t) {
                                    LOG.warn("Failed to read config modules", t);
                                }
                            });
                        } catch (final Exception e) {
                            LOG.warn("Failed to read config module data", e);
                        }
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.warn("Failed to read config module data", t);
                }
            });
        } else {
            //TODO what to do?, not able to create a new RIBImpl from OpenConfig
        }
    }

    private final class TableTypesFunction implements AsyncFunction<List<AfiSafi>, List<LocalTable>> {

        private final ReadTransaction rTx;

        public TableTypesFunction(final ReadTransaction rTx) {
            this.rTx = rTx;
        }

        @Override
        public ListenableFuture<List<LocalTable>> apply(final List<AfiSafi> afiSafis) throws Exception {
            final ListenableFuture<Optional<Service>> readFuture = configModuleWriter.readConfigService(new ServiceKey(BgpTableType.class), rTx);
            return Futures.transform(readFuture, new AsyncFunction<Optional<Service>, List<LocalTable>>() {

                @Override
                public ListenableFuture<List<LocalTable>> apply(final Optional<Service> maybeService) {
                    if (maybeService.isPresent()) {
                        final Service service = maybeService.get();
                        final List<ListenableFuture<Optional<Module>>> modulesFuture = new ArrayList<>();
                        final Map<String, String> moduleNameToService = new HashMap<>();
                        for (final Instance instance : service.getInstance()) {
                            final String moduleName = ToConfigModuleUtil.getModuleName(instance.getProvider());
                            LOG.info("read module: {}", new ModuleKey(moduleName, BgpTableTypeImpl.class));
                            modulesFuture.add(configModuleWriter.readModuleConfiguration(new ModuleKey(moduleName, BgpTableTypeImpl.class), rTx));
                            moduleNameToService.put(moduleName, instance.getName());
                        }
                        return Futures.transform(Futures.successfulAsList(modulesFuture), new Function<List<Optional<Module>>, List<LocalTable>>() {
                            @Override
                            public List<LocalTable> apply(final List<Optional<Module>> maybeModules) {
                                final List<LocalTable> localTables = new ArrayList<>();
                                for (final Optional<Module> moduleOpt : maybeModules) {
                                    if (moduleOpt.isPresent()) {
                                        final Module module = moduleOpt.get();
                                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpTableTypeImpl config =
                                                ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpTableTypeImpl) module.getConfiguration());
                                        final AfiSafi afiSafi = ToOpenConfigUtil.toAfiSafi(new org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl(config.getAfi(), config.getSafi()));
                                        if (afiSafis.contains(afiSafi)) {
                                            localTables.add(new LocalTableBuilder().setName(moduleNameToService.get(module.getName())).setType(BgpTableType.class).build());
                                        }
                                    }
                                }
                                return localTables;
                            }
                        });
                    }
                    return Futures.immediateFuture(Collections.<LocalTable>emptyList());
                }

            });
        }
    }
}
