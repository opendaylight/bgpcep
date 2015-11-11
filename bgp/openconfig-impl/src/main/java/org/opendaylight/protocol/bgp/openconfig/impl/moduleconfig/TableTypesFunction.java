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
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yangtools.yang.binding.ChildOf;

final class TableTypesFunction<T extends ServiceRef & ChildOf<Module>> implements AsyncFunction<List<AfiSafi>, List<T>> {

    private final ReadTransaction rTx;
    private final BGPConfigModuleProvider configModuleWriter;
    private final Function<String, T> function;

    public TableTypesFunction(final ReadTransaction rTx, final BGPConfigModuleProvider configModuleWriter, final Function<String, T> function) {
        this.rTx = Preconditions.checkNotNull(rTx);
        this.configModuleWriter = Preconditions.checkNotNull(configModuleWriter);
        this.function = Preconditions.checkNotNull(function);
    }

    @Override
    public ListenableFuture<List<T>> apply(final List<AfiSafi> afiSafis) {
        final ListenableFuture<Optional<Service>> readFuture = configModuleWriter.readConfigService(new ServiceKey(BgpTableType.class), rTx);
        return Futures.transform(readFuture, new AsyncFunction<Optional<Service>, List<T>>() {

            @Override
            public ListenableFuture<List<T>> apply(final Optional<Service> maybeService) {
                if (maybeService.isPresent()) {
                    final Service service = maybeService.get();
                    final List<ListenableFuture<Optional<Module>>> modulesFuture = new ArrayList<>();
                    final Map<String, String> moduleNameToService = new HashMap<>();
                    for (final Instance instance : service.getInstance()) {
                        final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                        modulesFuture.add(configModuleWriter.readModuleConfiguration(new ModuleKey(moduleName, BgpTableTypeImpl.class), rTx));
                        moduleNameToService.put(moduleName, instance.getName());
                    }
                    return Futures.transform(Futures.successfulAsList(modulesFuture), new ModulesToLocalTablesFunction(afiSafis, moduleNameToService));
                }
                return Futures.immediateFailedFuture(new IllegalStateException("No BgpTableType service present in configuration."));
            }

        });
    }

    private final class ModulesToLocalTablesFunction implements Function<List<Optional<Module>>, List<T>> {

        private final List<AfiSafi> afiSafis;
        private final Map<String, String> moduleNameToService;

        public ModulesToLocalTablesFunction(final List<AfiSafi> afiSafis, final Map<String, String> moduleNameToService) {
            this.afiSafis = afiSafis;
            this.moduleNameToService = moduleNameToService;
        }

        @Override
        public List<T> apply(final List<Optional<Module>> maybeModules) {
            final ImmutableList<Module> modules = FluentIterable.from(maybeModules)
                    .filter(new Predicate<Optional<Module>>() {
                        @Override
                        public boolean apply(final Optional<Module> input) {
                            return input.isPresent();
                        }
                    }).transform(new Function<Optional<Module>, Module>() {
                        @Override
                        public Module apply(final Optional<Module> input) {
                            return input.get();
                        }
                    }).toList();

            return toTableTypes(afiSafiToModuleName(modules));
        }

        private Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName(final List<Module> modules) {
            final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName = new HashMap<>(afiSafis.size());
            for (final Module module : modules) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpTableTypeImpl config =
                        ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpTableTypeImpl) module.getConfiguration());
                final Optional<AfiSafi> afiSafi = OpenConfigUtil.toAfiSafi(new org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl(config.getAfi(), config.getSafi()));
                if (afiSafi.isPresent()) {
                    afiSafiToModuleName.put(afiSafi.get().getAfiSafiName(), module.getName());
                }
            }
            return afiSafiToModuleName;
        }

        private List<T> toTableTypes(final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName) {
            final List<T> tableTypes = new ArrayList<>(afiSafis.size());
            for (final AfiSafi afiSafi : afiSafis) {
                final String moduleName = afiSafiToModuleName.get(afiSafi.getAfiSafiName());
                if (moduleName != null) {
                    tableTypes.add(function.apply(moduleNameToService.get(moduleName)));
                }
            }
            return tableTypes;
        }

    }
}
