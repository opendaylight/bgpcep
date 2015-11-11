/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig.BGPOpenConfigListener;
import org.opendaylight.protocol.bgp.openconfig.impl.openconfig.BGPConfigModuleMapperProvider;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
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

public final class BGPOpenConfig implements BindingAwareConsumer, AutoCloseable, DataTreeChangeListener<Node>, BGPOpenConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenConfig.class);

    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY = InstanceIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).build();

    private static final NodeKey CONFIG_NODE_KEY = new NodeKey(new NodeId("controller-config"));

    private static final InstanceIdentifier<Node> CONTROLLER_CONFIG_IID = NETCONF_TOPOLOGY
            .child(Node.class, CONFIG_NODE_KEY);

    private final BGPConfigStateStore configStateHolders;
    private BGPConfigModuleMapperProvider configModuleListener;
    private BGPOpenConfigListener openConfigListener;
    private ListenerRegistration<BGPOpenConfig> registration;
    private MountPointService mountService;
    private DataBroker dataBroker;

    public BGPOpenConfig() {
        configStateHolders = new BGPConfigStateStoreImpl();
        configStateHolders.registerBGPConfigHolder(Global.class, new BGPConfigHolderImpl<Global>());
        configStateHolders.registerBGPConfigHolder(Neighbor.class, new BGPConfigHolderImpl<Neighbor>());
    }

    @Override
    public void onSessionInitialized(final ConsumerContext session) {
        dataBroker = session.getSALService(DataBroker.class);
        try {
            configModuleListener = new BGPConfigModuleMapperProvider(dataBroker, configStateHolders);
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Failed to initialize application.", e);
            throw new IllegalStateException(e);
        }
        mountService = session.getSALService(MountPointService.class);
        registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                NETCONF_TOPOLOGY.child(Node.class)), this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        for (final DataTreeModification<Node> dataTreeModification : changes) {
            final DataObjectModification<Node> rootNode = dataTreeModification.getRootNode();
            if (dataTreeModification.getRootPath().getRootIdentifier().firstKeyOf(Node.class).equals(CONFIG_NODE_KEY)) {
                switch (rootNode.getModificationType()) {
                case DELETE:
                    closeOpenConfigListener();
                    break;
                case SUBTREE_MODIFIED:
                    closeOpenConfigListener();
                    getConfigMountPoint();
                    break;
                case WRITE:
                    getConfigMountPoint();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + rootNode.getModificationType());
                }
            }
        }
    }

    private void getConfigMountPoint() {
        final Callable<MountPoint> callable = new Callable<MountPoint>() {
            @Override
            public MountPoint call() {
                Optional<MountPoint> mp;
                do {
                    mp = mountService.getMountPoint(CONTROLLER_CONFIG_IID);
                } while (!mp.isPresent());
                return mp.get();
            }
        };
        openConfigListener = new BGPOpenConfigListener(dataBroker, callable, configStateHolders);
    }

    @Override
    public void close() {
        closeConfigModuleListener();
        closeOpenConfigListener();
        if (registration != null) {
            registration.close();
            registration = null;
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

}
