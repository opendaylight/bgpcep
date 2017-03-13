/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.Ibgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.path.selection.mode.rev160301.AdvertiseAllPaths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.path.selection.mode.rev160301.AdvertiseNPaths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.path.selection.mode.rev160301.PathSelectionModeFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.BgpPathSelectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.BgpPsmImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ModuleType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yangtools.yang.binding.ChildOf;

@SuppressWarnings("ALL")
final class PathSelectionModeFunction {

    private static final Map<String, Class<? extends ModuleType>> PATH_SELECTION_MODULE_TYPES;

    static {
        final Builder<String, Class<? extends ModuleType>> builder = ImmutableMap.builder();
        builder.put(AdvertiseNPaths.QNAME.getLocalName(), AdvertiseNPaths.class);
        builder.put(AdvertiseAllPaths.QNAME.getLocalName(), AdvertiseAllPaths.class);
        PATH_SELECTION_MODULE_TYPES = builder.build();
    }

    private PathSelectionModeFunction() {
        throw new UnsupportedOperationException();
    }

    public static <T extends ServiceRef & ChildOf<Module>> List<T> getPathSelectionMode(final ReadOnlyTransaction rTx, final BGPConfigModuleProvider
            configModuleWriter, final Function<String, T> function, final List<AfiSafi> afiSafis) {
        final ImmutableList<AfiSafi> afiSafisMultipath = FluentIterable.from(afiSafis).filter(new Predicate<AfiSafi>() {
            @Override
            public boolean apply(final AfiSafi afisafi) {
                final AfiSafi1 afiSafi1 = afisafi.getAugmentation(AfiSafi1.class);
                if (afiSafi1 != null) {
                    final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.UseMultiplePaths useMultiplePaths = afiSafi1.getUseMultiplePaths();
                    if (useMultiplePaths != null) {
                        final Config useMultiplePathsConfig = useMultiplePaths.getConfig();
                        if (useMultiplePathsConfig != null && useMultiplePathsConfig.isEnabled() != null) {
                            return useMultiplePathsConfig.isEnabled();
                        }
                    }
                }
                return false;
            }}).toList();
        try {
            final Optional<Service> maybeService = configModuleWriter.readConfigService(new ServiceKey(BgpPathSelectionMode.class), rTx);
            if (maybeService.isPresent()) {
                final Service service = maybeService.get();
                final List<Module> modules = new ArrayList<>();
                final Map<String, String> moduleNameToService = new HashMap<>();
                for (final Instance instance : service.getInstance()) {
                    final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                    final ModuleKey moduleKey = new ModuleKey(moduleName, BgpPsmImpl.class);
                    final Optional<Module> moduleConfig = configModuleWriter.readModuleConfiguration(moduleKey, rTx);
                    if (moduleConfig.isPresent()) {
                        modules.add(moduleConfig.get());
                    }
                    moduleNameToService.put(moduleName, instance.getName());
                }

                return TableTypesFunction.toServices(function, afiSafisMultipath, afiSafiToModuleName(afiSafisMultipath, modules, configModuleWriter, rTx), moduleNameToService);
            }
            return Collections.emptyList();
        } catch (final ReadFailedException e) {
            throw new IllegalStateException(OpenConfigUtil.FAILED_TO_READ_SERVICE, e);
        }
    }

    private static Class<? extends ModuleType> getModuleTypeClass(final String moduleType) {
        return PATH_SELECTION_MODULE_TYPES.get(moduleType);
    }

    private static Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName(final List<AfiSafi> afiSafis, final List<Module> modules, final BGPConfigModuleProvider configModuleWriter, final ReadTransaction rTx) throws ReadFailedException {
        final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName = new HashMap<>(afiSafis.size());
        for (final Module module : modules) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPsmImpl config =
                    ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPsmImpl) module.getConfiguration());

            final Optional<AfiSafi> tryFind = Iterables.tryFind(afiSafis, new Predicate<AfiSafi>() {
                @Override
                public boolean apply(final AfiSafi input) {
                    final Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(input.getAfiSafiName());
                    if (bgpTableType.isPresent() && AddPathFunction.tableTypeExists(configModuleWriter, rTx,
                            (String) config.getPathAddressFamily().getName(), bgpTableType.get())) {
                        final Ibgp ibgp = input.getAugmentation(AfiSafi1.class).getUseMultiplePaths().getIbgp();
                        return pathSelectionModeExists(configModuleWriter, rTx, (String) config.getPathSelectionMode().getName(), ibgp);
                    }
                    return false;
                }
            });
            if (tryFind.isPresent()) {
                afiSafiToModuleName.put(tryFind.get().getAfiSafiName(), module.getName());
            }
        }
        return afiSafiToModuleName;
    }

    public static boolean pathSelectionModeExists(final BGPConfigModuleProvider configModuleWriter, final ReadTransaction rTx,
            final String instanceName, final Ibgp ibgp) {

        try {
            final Optional<Service> maybeService = configModuleWriter.readConfigService(new ServiceKey(PathSelectionModeFactory.class), rTx);
            if (maybeService.isPresent()) {
                for (final Instance instance : maybeService.get().getInstance()) {
                    final String provider = instance.getProvider();
                    final String moduleName = OpenConfigUtil.getModuleName(provider);
                    final String moduleType = OpenConfigUtil.getModuleType(provider);
                    if (moduleName.equals(instanceName)) {
                        final ModuleKey moduleKey = new ModuleKey(moduleName, getModuleTypeClass(moduleType));
                        final Optional<Module> moduleConfig = configModuleWriter.readModuleConfiguration(moduleKey, rTx);
                        if (moduleConfig.isPresent()) {
                            if (ibgp != null && ibgp.getConfig() != null && ibgp.getConfig().getMaximumPaths() != null) {
                                final long maxPaths = ibgp.getConfig().getMaximumPaths();
                                if (moduleConfig.get().getType().equals(AdvertiseNPaths.class)) {
                                    final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.path.selection.mode.rev160301.modules.module.configuration.AdvertiseNPaths config =
                                            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.path.selection.mode.rev160301.modules.module.configuration.AdvertiseNPaths)moduleConfig.get().getConfiguration();
                                    if (maxPaths == config.getNBestPaths().longValue()) {
                                        return true;
                                    }
                                }
                            } else {
                                if (moduleConfig.get().getType().equals(AdvertiseAllPaths.class)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        } catch (final ReadFailedException e) {
            throw new IllegalStateException(OpenConfigUtil.FAILED_TO_READ_SERVICE, e);
        }
    }

}
