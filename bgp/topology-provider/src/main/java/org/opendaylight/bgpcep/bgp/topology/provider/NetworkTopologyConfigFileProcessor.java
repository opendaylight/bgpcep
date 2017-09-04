/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class NetworkTopologyConfigFileProcessor implements ConfigFileProcessor, AutoCloseable {
    private static final SchemaPath TOPOLOGY_SCHEMA_PATH = SchemaPath.create(true, NetworkTopology.QNAME);
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final BgpTopologyDeployer deployer;
    private final AbstractRegistration registration;
    private final YangInstanceIdentifier topologyYii;

    public NetworkTopologyConfigFileProcessor(final ConfigLoader configLoader, final BgpTopologyDeployer deployer) {
        requireNonNull(configLoader);
        this.deployer = requireNonNull(deployer);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.topologyYii = this.bindingSerializer.toYangInstanceIdentifier(deployer.getInstanceIdentifier());
        this.registration = configLoader.registerConfigFile(this);
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
    }

    @Nonnull
    @Override
    public SchemaPath getSchemaPath() {
        return TOPOLOGY_SCHEMA_PATH;
    }

    @Override
    public void loadConfiguration(@Nonnull final NormalizedNode<?, ?> dto) {
        final ContainerNode networkTopologyContainer = (ContainerNode) dto;
        final MapNode topologyList = (MapNode) networkTopologyContainer.getChild(
                topologyYii.getLastPathArgument()).get();
        final Collection<MapEntryNode> networkTopology = topologyList.getValue();
        for (final MapEntryNode topology : networkTopology) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> bi = this.bindingSerializer.fromNormalizedNode(this.topologyYii , topology);
            if (bi != null) {
                this.deployer.createInstance((Topology) bi.getValue());
            }
        }
    }
}
