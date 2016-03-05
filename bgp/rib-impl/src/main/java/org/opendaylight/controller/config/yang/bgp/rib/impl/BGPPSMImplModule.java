/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPBestPathSelection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class BGPPSMImplModule extends AbstractBGPPSMImplModule {
    public BGPPSMImplModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPPSMImplModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver, BGPPSMImplModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final class AutoCloseableBestPathSelectionStrategy implements BGPBestPathSelection {

        private final BgpTableType pathFamilyDependency;
        private final PathSelectionMode strategyFactory;

        public AutoCloseableBestPathSelectionStrategy(final BgpTableType pathFamilyDependency, final PathSelectionMode strategyFactory) {
            this.pathFamilyDependency = Preconditions.checkNotNull(pathFamilyDependency);
            this.strategyFactory = Preconditions.checkNotNull(strategyFactory);
        }

        @Override
        public void close() throws Exception {
            //no op
        }

        @Override
        public Class<? extends AddressFamily> getAfi() {
            return this.pathFamilyDependency.getAfi();
        }

        @Override
        public Class<? extends SubsequentAddressFamily> getSafi() {
            return this.pathFamilyDependency.getSafi();
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return BGPBestPathSelection.class;
        }

        @Override
        public PathSelectionMode getStrategy() {
            return this.strategyFactory;
        }
    }

    @Override
    public void customValidation() {
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AutoCloseableBestPathSelectionStrategy(getPathAddressFamilyDependency(), getPathSelectionModeDependency());
    }
}
