/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Application peer handler which handles translation from custom RIB into local RIB
 */
public class BGPApplicationPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPApplicationPeerModule {

    private static final QName APP_ID_QNAME = QName.cachedReference(QName.create(ApplicationRib.QNAME, "id"));

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.BGPApplicationPeerModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final YangInstanceIdentifier id = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME).nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, getApplicationRibId().getValue()).node(Tables.QNAME).node(Tables.QNAME).build();
        final DOMDataTreeChangeService service = (DOMDataTreeChangeService) getDataBrokerDependency().getSupportedExtensions().get(DOMDataTreeChangeService.class);
        return service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), new ApplicationPeer(getApplicationRibId(), getBgpPeerId(), (RIBImpl) getTargetRibDependency(), new AppPeerModuleTracker(getTargetRibDependency().getOpenConfigProvider())));
    }

    private final class AppPeerModuleTracker implements BGPConfigModuleTracker {

        private final BGPOpenconfigMapper<BGPAppPeerInstanceConfiguration> appProvider;
        private final InstanceConfigurationIdentifier identifier;

        public AppPeerModuleTracker(final Optional<BGPOpenConfigProvider> openConfigProvider) {
            if (openConfigProvider.isPresent()) {
                appProvider = openConfigProvider.get().getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class);
            } else {
                appProvider = null;
            }
            identifier = new InstanceConfigurationIdentifier(getIdentifier().getInstanceName());
        }

        @Override
        public void onInstanceCreate() {
            if (appProvider != null) {
                appProvider.writeConfiguration(new BGPAppPeerInstanceConfiguration(identifier, getApplicationRibId().getValue(),
                    Rev130715Uitl.getIpv4Address(getBgpPeerId())));
            }
        }

        @Override
        public void onInstanceClose() {
            if (appProvider != null) {
                appProvider.removeConfiguration(identifier);
            }
        }

    }
}
