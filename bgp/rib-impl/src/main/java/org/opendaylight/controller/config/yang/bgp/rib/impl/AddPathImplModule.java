/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class AddPathImplModule extends AbstractAddPathImplModule {
    public AddPathImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public AddPathImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.AddPathImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getSendReceive(), "value is not set.", sendReceiveJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getAddressFamily(), "value is not set.", addressFamilyJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AutoCloseableAddPath(getAddressFamilyDependency(), getSendReceive());
    }

    private static final class AutoCloseableAddPath implements AutoCloseable, AddressFamilies {
        private final BgpTableType family;
        private final SendReceive sendReceiveMode;

        public AutoCloseableAddPath(final BgpTableType addressFamilyDependency, final SendReceive sendReceive) {
            this.family = Preconditions.checkNotNull(addressFamilyDependency);
            this.sendReceiveMode = Preconditions.checkNotNull(sendReceive);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("AutoCloseableAddPath [family=");
            builder.append(this.family.toString());
            builder.append(", sendReceiveMode=");
            builder.append(this.sendReceiveMode);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return AddressFamilies.class;
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public <E extends Augmentation<AddressFamilies>> E getAugmentation(final Class<E> arg0) {
            return null;
        }

        @Override
        public Class<? extends AddressFamily> getAfi() {
            return this.family.getAfi();
        }

        @Override
        public Class<? extends SubsequentAddressFamily> getSafi() {
            return this.family.getSafi();
        }

        @Override
        public SendReceive getSendReceive() {
            return this.sendReceiveMode;
        }
    }

}
