/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.ModuleTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPApplicationPeerClusterSingletonService implements ClusterSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPApplicationPeerClusterSingletonService.class);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    private final ApplicationPeer appPeer;
    private final ServiceGroupIdentifier serviceGroupIdentifier;
    private final ApplicationRibId appRibId;
    private final YangInstanceIdentifier appYangId;
    private final DOMDataTreeChangeService domDataTreeChangeService;
    private final RIB rib;
    private final BgpId bgpPeerId;
    private final BGPPeerRegistry bgpPeerRegistry;
    private final String instanceName;
    private ClusterSingletonServiceRegistration registration;
    private ListenerRegistration<ApplicationPeer> listenerRegistration;

    public BGPApplicationPeerClusterSingletonService(final ApplicationRibId applicationRibId,
        final DOMDataBroker dataBroker, final RIB rib, final BgpId bgpPeerId, final BGPPeerRegistry bgpPeerRegistry, final String instanceName) {
        this.rib = rib;
        this.appRibId = applicationRibId;
        this.appYangId = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME).nodeWithKey(ApplicationRib.QNAME,
            APP_ID_QNAME, this.appRibId.getValue()).node(Tables.QNAME).node(Tables.QNAME).build();
        this.domDataTreeChangeService = (DOMDataTreeChangeService) dataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);
        this.bgpPeerId = bgpPeerId;
        this.bgpPeerRegistry = bgpPeerRegistry;
        this.instanceName = instanceName;

        final Optional<BGPOpenConfigProvider> openConfigProvider = this.rib.getOpenConfigProvider();
       BGPOpenconfigMapper<BGPAppPeerInstanceConfiguration> appProvider = null;
       if (openConfigProvider.isPresent()) {
               appProvider = openConfigProvider.get().getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class);
           }
       final InstanceConfigurationIdentifier identifier = new InstanceConfigurationIdentifier(instanceName);
       final BGPAppPeerInstanceConfiguration bgpAppPeerInsConf = new BGPAppPeerInstanceConfiguration(identifier,
           applicationRibId.getValue(), bgpPeerId);
       final ModuleTracker<BGPAppPeerInstanceConfiguration> appPeerMT = new ModuleTracker<>(appProvider, bgpAppPeerInsConf);

        this.appPeer = new ApplicationPeer(this.appRibId, this.bgpPeerId, this.rib, appPeerMT);
        this.serviceGroupIdentifier = this.rib.getRibIServiceGroupIdentifier();
        LOG.info("Application Peer Singleton Service {} registered", getIdentifier());
        this.registration = this.rib.registerClusterSingletonService(this);
        addToPeerRegistry();
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Application Peer Singleton Service {} instantiated", getIdentifier());
        this.appPeer.instantiateServiceInstance();
        this.listenerRegistration = this.domDataTreeChangeService.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, this.appYangId), appPeer);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Application Peer Singleton Service {} instance closed", getIdentifier());
        this.listenerRegistration.close();
        this.appPeer.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.serviceGroupIdentifier;
    }

    @Override
    public void close() throws Exception {
        removeFromPeerRegistry();
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    /**
     * add to peer-registry to catch any conflicting peer addresses
     */
    private void addToPeerRegistry() {
        final IpAddress bgpPeerId = new IpAddress(this.bgpPeerId);
        final BGPPeer bgpClientPeer = new BGPPeer(bgpPeerId.getIpv4Address().getValue(), this.rib, PeerRole.Internal, SimpleRoutingPolicy.AnnounceNone, null);

        final AsNumber as = this.rib.getLocalAs();
        final BGPSessionPreferences prefs = new BGPSessionPreferences(as, 0, this.rib.getBgpIdentifier(), as, Collections.emptyList(), Optional.absent());
        final BGPPeerRegistry peerRegistry = getPeerRegistryBackwards();
        peerRegistry.addPeer(bgpPeerId, bgpClientPeer, prefs);
    }

    private void removeFromPeerRegistry() {
        final IpAddress bgpPeerId = new IpAddress(this.bgpPeerId);
        final BGPPeerRegistry peerRegistry = getPeerRegistryBackwards();
        peerRegistry.removePeer(bgpPeerId);
    }

    private BGPPeerRegistry getPeerRegistryBackwards() {
        return this.bgpPeerRegistry == null ? StrictBGPPeerRegistry.GLOBAL : this.bgpPeerRegistry;
    }
}
