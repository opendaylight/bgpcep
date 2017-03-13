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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.neighbor.UseMultiplePaths;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.neighbor.use.multiple.paths.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi2;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.AddPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.AddPathImpl;
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
final class AddPathFunction {

    private AddPathFunction() {
        throw new UnsupportedOperationException();
    }

    public static <T extends ServiceRef & ChildOf<Module>> List<T> getAddPath(final ReadOnlyTransaction rTx, final BGPConfigModuleProvider
            configModuleWriter, final Function<String, T> function, final List<AfiSafi> afiSafis) {
        final ImmutableList<AfiSafi> afiSafisMultipath = FluentIterable.from(afiSafis).filter(new Predicate<AfiSafi>() {
            @Override
            public boolean apply(final AfiSafi afisafi) {
                final AfiSafi2 afiSafi2 = afisafi.getAugmentation(AfiSafi2.class);
                if (afiSafi2 != null) {
                    final UseMultiplePaths useMultiplePaths = afiSafi2.getUseMultiplePaths();
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
            final Optional<Service> maybeService = configModuleWriter.readConfigService(new ServiceKey(AddPath.class), rTx);
            if (maybeService.isPresent()) {
                final Service service = maybeService.get();
                final List<Module> modules = new ArrayList<>();
                final Map<String, String> moduleNameToService = new HashMap<>();
                for (final Instance instance : service.getInstance()) {
                    final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                    final ModuleKey moduleKey = new ModuleKey(moduleName, AddPathImpl.class);
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

    private static Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName(final List<AfiSafi> afiSafis, final List<Module> modules, final BGPConfigModuleProvider configModuleWriter, final ReadTransaction rTx) throws ReadFailedException {
        final Map<Class<? extends AfiSafiType>, String> afiSafiToModuleName = new HashMap<>(afiSafis.size());
        for (final Module module : modules) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.AddPathImpl config =
                ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.AddPathImpl) module.getConfiguration());
            if (config.getSendReceive() == SendReceive.Both) {
                final Optional<AfiSafi> tryFind = Iterables.tryFind(afiSafis, new Predicate<AfiSafi>() {
                    @Override
                    public boolean apply(final AfiSafi input) {
                        final Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(input.getAfiSafiName());
                        return bgpTableType.isPresent() && tableTypeExists(configModuleWriter, rTx,
                                (String) config.getAddressFamily().getName(), bgpTableType.get());
                    }
                });
                if (tryFind.isPresent()) {
                    afiSafiToModuleName.put(tryFind.get().getAfiSafiName(), module.getName());
                }
            }
        }
        return afiSafiToModuleName;
    }

    public static boolean tableTypeExists(final BGPConfigModuleProvider configModuleWriter, final ReadTransaction rTx,
            final String instanceName, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType tableType) {
        try {
            final Optional<Service> maybeService = configModuleWriter.readConfigService(new ServiceKey(BgpTableType.class), rTx);
            if (maybeService.isPresent()) {
                for (final Instance instance : maybeService.get().getInstance()) {
                    final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                    if (moduleName.equals(instanceName)) {
                        final ModuleKey moduleKey = new ModuleKey(moduleName, BgpTableTypeImpl.class);
                        final Optional<Module> moduleConfig = configModuleWriter.readModuleConfiguration(moduleKey, rTx);
                        if (moduleConfig.isPresent()) {
                            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpTableTypeImpl tableTypeImpl = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpTableTypeImpl)moduleConfig.get().getConfiguration();
                            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType moduleTableType = new org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl(tableTypeImpl.getAfi(), tableTypeImpl.getSafi());
                            if (moduleTableType.equals(tableType)) {
                                return true;
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
