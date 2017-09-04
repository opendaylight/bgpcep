/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class ProtocolsConfigFileProcessor implements ConfigFileProcessor, AutoCloseable {
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final BgpDeployer bgpDeployer;
    private final AbstractRegistration registration;
    private final InstanceIdentifier<Protocols> protocolsIID;
    private final YangInstanceIdentifier protocolYIId;
    private static final SchemaPath PROTOCOLS_SCHEMA_PATH = SchemaPath.create(true, NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME);

    public ProtocolsConfigFileProcessor(final ConfigLoader configLoader, final BgpDeployer bgpDeployer) {
        requireNonNull(configLoader);
        this.bgpDeployer = requireNonNull(bgpDeployer);
        this.protocolsIID = this.bgpDeployer.getInstanceIdentifier().child(Protocols.class);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.protocolYIId = this.bindingSerializer.toYangInstanceIdentifier(this.protocolsIID.child(Protocol.class));
        this.registration = configLoader.registerConfigFile(this);
    }

    @Override
    public SchemaPath getSchemaPath() {
        return PROTOCOLS_SCHEMA_PATH;
    }

    @Override
    public void loadConfiguration(@Nonnull final NormalizedNode<?, ?> dto) {
        final ContainerNode protocolsContainer = (ContainerNode) dto;
        final MapNode protocolList = (MapNode) protocolsContainer.getChild(protocolYIId.getLastPathArgument()).get();
        final Collection<MapEntryNode> protocolsCollection = protocolList.getValue();
        for (final MapEntryNode protocolEntry : protocolsCollection) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> bi = this.bindingSerializer.fromNormalizedNode(this.protocolYIId, protocolEntry);
            if (bi != null) {
                notifyDeployer((Protocol) bi.getValue());
            }
        }
    }

    private void notifyDeployer(final Protocol protocol) {
        final Protocol1 bgp = protocol.getAugmentation(Protocol1.class);
        if (bgp != null) {
            final InstanceIdentifier<Bgp> bgpIID = this.protocolsIID.child(Protocol.class, new ProtocolKey(BGP.class, protocol.getName()))
                .augmentation(Protocol1.class).child(Bgp.class);

            final Global global = bgp.getBgp().getGlobal();
            if (global != null) {
                this.bgpDeployer.onGlobalModified(bgpIID, global, () -> this.bgpDeployer.writeConfiguration(global, bgpIID.child(Global.class)));
            }

            final Neighbors neighbors = bgp.getBgp().getNeighbors();
            if (neighbors != null) {
                final List<Neighbor> neighborsList = neighbors.getNeighbor();
                final InstanceIdentifier<Neighbors> neighborsIID = bgpIID.child(Neighbors.class);
                neighborsList.forEach(neighbor -> this.bgpDeployer.onNeighborModified(bgpIID, neighbor,
                    () -> this.bgpDeployer.writeConfiguration(neighbor, neighborsIID.child(Neighbor.class, neighbor.getKey()))));
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
    }
}
