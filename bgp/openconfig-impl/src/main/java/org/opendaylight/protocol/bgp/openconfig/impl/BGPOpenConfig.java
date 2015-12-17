/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.MountPointService.MountPointListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig.BGPOpenConfigListener;
import org.opendaylight.protocol.bgp.openconfig.impl.openconfig.BGPConfigModuleMapperProvider;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPOpenConfig implements BindingAwareConsumer, AutoCloseable, BGPOpenConfigProvider, MountPointListener {

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenConfig.class);

    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY = InstanceIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).build();

    private static final NodeKey CONFIG_NODE_KEY = new NodeKey(new NodeId("controller-config"));

    private static final InstanceIdentifier<Node> CONTROLLER_CONFIG_IID = NETCONF_TOPOLOGY
            .child(Node.class, CONFIG_NODE_KEY);

    private final BGPConfigStateStore configStateHolders;
    private BGPConfigModuleMapperProvider configModuleListener;
    @GuardedBy("this")
    private BGPOpenConfigListener openConfigListener;

    private MountPointService mountService;
    private DataBroker dataBroker;

    private ListenerRegistration<BGPOpenConfig> mpListenerRegistration;

    public BGPOpenConfig() {
        configStateHolders = new BGPConfigStateStoreImpl();
        configStateHolders.registerBGPConfigHolder(Bgp.class);
        configStateHolders.registerBGPConfigHolder(Neighbor.class);
    }

    @Override
    public void onSessionInitialized(final ConsumerContext session) {
        dataBroker = session.getSALService(DataBroker.class);
        configModuleListener = new BGPConfigModuleMapperProvider(dataBroker, configStateHolders);
        mountService = session.getSALService(MountPointService.class);
        mpListenerRegistration = mountService.registerListener(CONTROLLER_CONFIG_IID, this);
    }

    @Override
    public void close() {
        closeConfigModuleListener();
        closeOpenConfigListener();
        if (mpListenerRegistration != null) {
            mpListenerRegistration.close();
            mpListenerRegistration = null;
        }
    }

    private void closeOpenConfigListener() {
        if (openConfigListener != null) {
            try {
                openConfigListener.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close OpenConfigProperly properly.", e);
            } finally {
                openConfigListener = null;
            }
        }
    }

    private void closeConfigModuleListener() {
        if (configModuleListener != null) {
            try {
                configModuleListener.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close ConfigModuleListener properly.", e);
            } finally {
                configModuleListener = null;
            }
        }
    }

    @Override
    public <T extends InstanceConfiguration> BGPOpenconfigMapper<T> getOpenConfigMapper(final Class<T> clazz) {
        return configModuleListener.getOpenConfigMapper(clazz);
    }

    @Override
    public void onMountPointCreated(final InstanceIdentifier<?> path) {
        LOG.debug("Created mountpoint {}.", path);
        if (openConfigListener == null) {
            openConfigListener = new BGPOpenConfigListener(dataBroker, mountService.getMountPoint(path).get(), configStateHolders);
        }
    }

    @Override
    public void onMountPointRemoved(final InstanceIdentifier<?> path) {
        LOG.debug("Removed mountpoint {}.", path);
        closeOpenConfigListener();
    }

}
