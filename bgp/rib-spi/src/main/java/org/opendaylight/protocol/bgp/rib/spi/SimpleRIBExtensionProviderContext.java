/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleRIBExtensionProviderContext.class);

    private final ConcurrentMap<TablesKey, RIBSupport<?, ?, ?, ?>> supports = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeIdentifierWithPredicates, RIBSupport<?, ?, ?, ?>> domSupports =
            new ConcurrentHashMap<>();

    private final ModuleInfoBackedContext classLoadingStrategy = ModuleInfoBackedContext.create();

    @Override
    public <T extends RIBSupport<?, ?, ?, ?>> RIBSupportRegistration<T> registerRIBSupport(
            final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
            final T support) {
        final TablesKey key = new TablesKey(afi, safi);
        final RIBSupport<?, ?, ?, ?> prev = this.supports.putIfAbsent(key, support);
        Preconditions.checkArgument(prev == null, "AFI %s SAFI %s is already registered with %s",
                afi, safi, prev);
        this.domSupports.put(RibSupportUtils.toYangTablesKey(afi, safi), support);
        addClassLoadingSupport(afi, safi, support);
        return new AbstractRIBSupportRegistration<T>(support) {
            @Override
            protected void removeRegistration() {
                SimpleRIBExtensionProviderContext.this.supports.remove(key);
            }
        };
    }

    private void addClassLoadingSupport(final Class<?> afi, final Class<?> safi, final RIBSupport<?, ?, ?, ?> support) {
        final Set<YangModuleInfo> moduleInfos = getModuleInfos(afi, safi, support.routesListClass(),
                support.routesContainerClass(), support.routesCaseClass());
        if (!moduleInfos.isEmpty()) {
            this.classLoadingStrategy.addModuleInfos(moduleInfos);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static Set<YangModuleInfo> getModuleInfos(final Class<?>... clazzes) {
        final Set<YangModuleInfo> moduleInfos = new HashSet<>();
        for (final Class<?> clz : clazzes) {
            try {
                moduleInfos.add(BindingReflections.getModuleInfo(clz));
            } catch (final Exception e) {
                LOG.debug("Could not find module info for class {}", clz, e);
            }
        }
        return moduleInfos;
    }

    @Override
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>,
        R extends Route & ChildOf<S> & Identifiable<I>, I extends Identifier<R>> RIBSupport<C, S, R, I> getRIBSupport(
            final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        return getRIBSupport(new TablesKey(afi, safi));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>,
        R extends Route & ChildOf<S> & Identifiable<I>, I extends Identifier<R>> RIBSupport<C, S, R, I> getRIBSupport(
            final TablesKey key) {
        return (RIBSupport<C, S, R, I>) this.supports.get(requireNonNull(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>,
        R extends Route & ChildOf<S> & Identifiable<I>, I extends Identifier<R>> RIBSupport<C, S, R, I> getRIBSupport(
            final NodeIdentifierWithPredicates key) {
        return (RIBSupport<C, S, R, I>) this.domSupports.get(key);
    }

    @Override
    public GeneratedClassLoadingStrategy getClassLoadingStrategy() {
        return this.classLoadingStrategy;
    }
}
