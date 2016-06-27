/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Optional;
import java.util.Collections;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Application peer handler which handles translation from custom RIB into local RIB
 */
public class BGPApplicationPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPApplicationPeerModule {

    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();

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
        // add to peer-registry to catch any conflicting peer addresses
        addToPeerRegistry();

        final YangInstanceIdentifier id = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME).nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, getApplicationRibId().getValue()).node(Tables.QNAME).node(Tables.QNAME).build();
        final DOMDataTreeChangeService service = (DOMDataTreeChangeService) getDataBrokerDependency().getSupportedExtensions().get(DOMDataTreeChangeService.class);
        final ListenerRegistration<ApplicationPeer> listenerRegistration = service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), new ApplicationPeer(getApplicationRibId(), getBgpPeerId(), (RIBImpl) getTargetRibDependency(), new AppPeerModuleTracker(getTargetRibDependency().getOpenConfigProvider())));

        return new CloseableNoEx() {
            @Override
            public void close() {
                listenerRegistration.close();
                removeFromPeerRegistry();
            }
        };
    }

    private interface CloseableNoEx extends AutoCloseable {
        @Override
        void close();
    }

    private void addToPeerRegistry() {
        final RIB r = getTargetRibDependency();

        final IpAddress bgpPeerId = new IpAddress(getBgpPeerId());
        final BGPPeer bgpClientPeer = new BGPPeer(bgpPeerId.getIpv4Address().getValue(), r, PeerRole.Internal, SimpleRoutingPolicy.AnnounceNone, null);

        final BGPSessionPreferences prefs = new BGPSessionPreferences(r.getLocalAs(), 0, r.getBgpIdentifier(),
            r.getLocalAs(), Collections.emptyList());

        final BGPPeerRegistry peerRegistry = getPeerRegistryBackwards();
        peerRegistry.addPeer(bgpPeerId, bgpClientPeer, prefs);
    }

    private void removeFromPeerRegistry() {
        final IpAddress bgpPeerId = new IpAddress(getBgpPeerId());
        final BGPPeerRegistry peerRegistry = getPeerRegistryBackwards();
        peerRegistry.removePeer(bgpPeerId);
    }

    private BGPPeerRegistry getPeerRegistryBackwards() {
        return getBgpPeerRegistryDependency() == null ? StrictBGPPeerRegistry.GLOBAL : getBgpPeerRegistryDependency();
    }

    private final class AppPeerModuleTracker implements BGPConfigModuleTracker {

        private final BGPOpenconfigMapper<BGPAppPeerInstanceConfiguration> appProvider;
        private final BGPAppPeerInstanceConfiguration bgpAppPeerInstanceConfiguration;

        public AppPeerModuleTracker(final Optional<BGPOpenConfigProvider> openConfigProvider) {
            if (openConfigProvider.isPresent()) {
                this.appProvider = openConfigProvider.get().getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class);
            } else {
                this.appProvider = null;
            }
            final InstanceConfigurationIdentifier identifier = new InstanceConfigurationIdentifier(getIdentifier().getInstanceName());
            this.bgpAppPeerInstanceConfiguration = new BGPAppPeerInstanceConfiguration(identifier, getApplicationRibId().getValue(),
                    Rev130715Util.getIpv4Address(getBgpPeerId()));
        }

        @Override
        public void onInstanceCreate() {
            if (this.appProvider != null) {
                this.appProvider.writeConfiguration(this.bgpAppPeerInstanceConfiguration);
            }
        }

        @Override
        public void onInstanceClose() {
            if (this.appProvider != null) {
                this.appProvider.removeConfiguration(this.bgpAppPeerInstanceConfiguration);
            }
        }

    }
}
