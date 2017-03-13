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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yangtools.yang.binding.ChildOf;

@SuppressWarnings("ALL")
final class TableTypesFunction {
    private TableTypesFunction() {
        throw new UnsupportedOperationException();
    }

    public static <T extends ServiceRef & ChildOf<Module>> List<T> getLocalTables(final ReadOnlyTransaction rTx, final BGPConfigModuleProvider
        configModuleWriter, final Function<String, T> function, final List<AfiSafi> afiSafis) {
        try {
            final Optional<Service> maybeService = configModuleWriter.readConfigService(new ServiceKey(BgpTableType.class), rTx);
            if (maybeService.isPresent()) {
                final Service service = maybeService.get();
                final List<Optional<Module>> maybeModules = new ArrayList<>();
                final Map<String, String> moduleNameToService = new HashMap<>();
                for (final Instance instance : service.getInstance()) {
                    final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                    final ModuleKey moduleKey = new ModuleKey(moduleName, BgpTableTypeImpl.class);
                    final Optional<Module> moduleConfig = configModuleWriter.readModuleConfiguration(moduleKey, rTx);
                    maybeModules.add(moduleConfig);
                    moduleNameToService.put(moduleName, instance.getName());
                }

                final ImmutableList<Module> modules = FluentIterable.from(maybeModules)
                    .filter(Optional::isPresent).transform(Optional::get).toList();

                return toServices(function, afiSafis, afiSafiToModuleName(afiSafis, modules), moduleNameToService);
            }
            throw new IllegalStateException("No BgpTableType service present in configuration.");
        } catch (final ReadFailedException e) {
            throw new IllegalStateException("Failed to read service.", e);
        }
    }

    public static <T extends ServiceRef & ChildOf<Module>> List<T> toServices(final Function<String, T> function, final List<AfiSafi> afiSafis,
        final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName, final Map<String, String> moduleNameToService) {
        final List<T> serives = new ArrayList<>(afiSafis.size());
        for (final AfiSafi afiSafi : afiSafis) {
            final String moduleName = afiSafiToModuleName.get(afiSafi.getAfiSafiName());
            if (moduleName != null) {
                serives.add(function.apply(moduleNameToService.get(moduleName)));
            }
        }
        return serives;
    }

    private static Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName(final List<AfiSafi> afiSafis, final List<Module> modules) {
        final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName = new HashMap<>(afiSafis.size());
        for (final Module module : modules) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpTableTypeImpl config =
                ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpTableTypeImpl) module.getConfiguration());
            final Optional<AfiSafi> afiSafi = OpenConfigUtil.toAfiSafi(new org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl(config.getAfi(), config.getSafi()));
            if (afiSafi.isPresent()) {
                afiSafiToModuleName.put(afiSafi.get().getAfiSafiName(), module.getName());
            }
        }
        return afiSafiToModuleName;
    }
}
