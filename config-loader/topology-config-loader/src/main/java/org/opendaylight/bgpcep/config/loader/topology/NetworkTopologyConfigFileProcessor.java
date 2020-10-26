/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.topology;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.config.loader.spi.AbstractConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NetworkTopologyConfigFileProcessor extends AbstractConfigFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyConfigFileProcessor.class);

    private static final SchemaPath TOPOLOGY_SCHEMA_PATH = SchemaPath.create(true, NetworkTopology.QNAME);
    private static final InstanceIdentifier<Topology> TOPOLOGY_IID =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class);

    private final YangInstanceIdentifier topologyYii;

    @Inject
    public NetworkTopologyConfigFileProcessor(final ConfigLoader configLoader, final DataBroker dataBroker) {
        super("Network Topology", configLoader, dataBroker);
        this.topologyYii = configLoader.getBindingNormalizedNodeSerializer().toYangInstanceIdentifier(TOPOLOGY_IID);
    }

    @PostConstruct
    public void init() {
        start();
    }

    @PreDestroy
    @Override
    public void close() {
        stop();
    }

    @Override
    public SchemaPath getSchemaPath() {
        return TOPOLOGY_SCHEMA_PATH;
    }

    @Override
    protected void loadConfiguration(final DataBroker dataBroker, final BindingNormalizedNodeSerializer serializer,
            final NormalizedNode<?, ?> dto) {
        final ContainerNode networkTopologyContainer = (ContainerNode) dto;
        final MapNode topologyList = (MapNode) networkTopologyContainer.getChild(
                this.topologyYii.getLastPathArgument()).get();
        final Collection<MapEntryNode> networkTopology = topologyList.getValue();
        if (networkTopology.isEmpty()) {
            return;
        }
        final WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();

        for (final MapEntryNode topologyEntry : networkTopology) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> bi =
                    serializer.fromNormalizedNode(topologyYii, topologyEntry);
            if (bi != null) {
                processTopology((Topology) bi.getValue(), wtx);
            }
        }
        try {
            wtx.commit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Network Topologies", e);
        }
    }

    private static void processTopology(final Topology topology, final WriteTransaction wtx) {
        LOG.info("Storing Topology {}", topology);
        final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIIdKeyed =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topology.key());
        wtx.mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, topologyIIdKeyed, topology);
    }
}
