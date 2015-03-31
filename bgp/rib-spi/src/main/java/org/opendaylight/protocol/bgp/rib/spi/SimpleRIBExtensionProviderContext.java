/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;


import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleRIBExtensionProviderContext.class);

    private final ConcurrentMap<TablesKey, AdjRIBsFactory> factories = new ConcurrentHashMap<>();
    private final ConcurrentMap<TablesKey, RIBSupport> supports = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeIdentifierWithPredicates, RIBSupport> domSupports = new ConcurrentHashMap<>();

    private final ModuleInfoBackedContext classLoadingStrategy = ModuleInfoBackedContext.create();


    @Override
    public final synchronized AbstractRegistration registerAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final AdjRIBsFactory factory) {
        final TablesKey key = new TablesKey(afi, safi);

        if (this.factories.containsKey(key)) {
            throw new IllegalArgumentException("Specified AFI/SAFI combination is already registered");
        }

        this.factories.put(key, factory);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleRIBExtensionProviderContext.this) {
                    SimpleRIBExtensionProviderContext.this.factories.remove(key);
                }
            }
        };
    }

    @Override
    public final synchronized AdjRIBsFactory getAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return this.factories.get(new TablesKey(afi, safi));
    }

    @Override
    public <T extends RIBSupport> RIBSupportRegistration<T> registerRIBSupport(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final T support) {
        final TablesKey key = new TablesKey(afi, safi);
        final RIBSupport prev = this.supports.putIfAbsent(key, support);
        Preconditions.checkArgument(prev == null, "AFI %s SAFI %s is already registered with %s", afi, safi, prev);
        this.domSupports.put(RibSupportUtils.toYangTablesKey(afi,safi), support);
        addClassLoadingSupport(afi, safi, support);
        return new AbstractRIBSupportRegistration<T>(support) {
            @Override
            protected void removeRegistration() {
                SimpleRIBExtensionProviderContext.this.supports.remove(key);
            }
        };
    }

    private void addClassLoadingSupport(final Class<?> afi, final Class<?> safi, final RIBSupport s) {
        final Set<YangModuleInfo> moduleInfos =
                getModuleInfos(afi, safi, s.routesListClass(), s.routesContainerClass(), s.routesCaseClass());
        if(!moduleInfos.isEmpty()) {
            classLoadingStrategy.addModuleInfos(moduleInfos);
        }
    }

    private static Set<YangModuleInfo> getModuleInfos(final Class<?>... clazzes) {
        final Set<YangModuleInfo> moduleInfos = new HashSet<>();
        for(final Class<?> clz : clazzes) {
            try {
                moduleInfos.add(BindingReflections.getModuleInfo(clz));
            } catch (final Exception e) {
                LOG.debug("Could not find module info for class {}", clz, e);
            }
        }
        return moduleInfos;
    }

    @Override
    public RIBSupport getRIBSupport(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        return getRIBSupport(new TablesKey(afi, safi));
    }

    @Override
    public RIBSupport getRIBSupport(final TablesKey key) {
        return this.supports.get(Preconditions.checkNotNull(key));
    }

    @Override
    public GeneratedClassLoadingStrategy getClassLoadingStrategy() {
        return classLoadingStrategy;
    }

    @Override
    public RIBSupport getRIBSupport(final NodeIdentifierWithPredicates key) {
        return domSupports.get(key);
    }
}
