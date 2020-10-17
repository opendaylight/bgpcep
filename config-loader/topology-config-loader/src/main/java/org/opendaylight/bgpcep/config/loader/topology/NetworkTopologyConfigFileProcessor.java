/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.topology;

import com.google.common.util.concurrent.FluentFuture;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.config.loader.spi.AbstractConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NetworkTopologyConfigFileProcessor extends AbstractConfigFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTopologyConfigFileProcessor.class);
    private static final SchemaPath TOPOLOGY_SCHEMA_PATH = SchemaPath.create(true, NetworkTopology.QNAME);

    @Inject
    public NetworkTopologyConfigFileProcessor(final ConfigLoader configLoader, final DOMDataBroker dataBroker) {
        super("Network Topology", configLoader, dataBroker);
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
    protected FluentFuture<? extends CommitInfo> loadConfiguration(final DOMDataBroker dataBroker,
            final NormalizedNode<?, ?> dto) {
        final ContainerNode networkTopology = (ContainerNode) dto;
        final MapNode topologies = (MapNode) networkTopology.getChild(new NodeIdentifier(Topology.QNAME)).orElse(null);
        if (networkTopology == null) {
            return CommitInfo.emptyFluentFuture();
        }

        final DOMDataTreeWriteTransaction wtx = dataBroker.newWriteOnlyTransaction();

        LOG.info("Storing Topologies {}", topologies.getValue().stream()
            .map(topo -> topo.getIdentifier().asMap()).collect(Collectors.toList()));
        wtx.merge(LogicalDatastoreType.CONFIGURATION,
            YangInstanceIdentifier.create(new NodeIdentifier(NetworkTopology.QNAME), topologies.getIdentifier()),
            topologies);

        return wtx.commit();
    }
}
