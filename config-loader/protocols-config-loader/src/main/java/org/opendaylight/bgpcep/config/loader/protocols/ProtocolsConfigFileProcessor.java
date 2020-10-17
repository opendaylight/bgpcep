/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.protocols;

import com.google.common.annotations.VisibleForTesting;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
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
public final class ProtocolsConfigFileProcessor extends AbstractConfigFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ProtocolsConfigFileProcessor.class);

    @VisibleForTesting
    static final InstanceIdentifier<Protocols> BGP_PROTOCOLS_IID =
            InstanceIdentifier.create(NetworkInstances.class)
                    .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp")).child(Protocols.class);
    private static final SchemaPath PROTOCOLS_SCHEMA_PATH = SchemaPath
            .create(true, NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME);

    private final YangInstanceIdentifier protocolYIId;

    @Inject
    public ProtocolsConfigFileProcessor(final ConfigLoader configLoader, final DataBroker dataBroker) {
        super("Protocols", configLoader, dataBroker);
        this.protocolYIId = configLoader.getBindingNormalizedNodeSerializer()
            .toYangInstanceIdentifier(BGP_PROTOCOLS_IID.child(Protocol.class));
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
        return PROTOCOLS_SCHEMA_PATH;
    }

    @Override
    protected void loadConfiguration(final DataBroker dataBroker, final BindingNormalizedNodeSerializer serializer,
            final NormalizedNode<?, ?> dto) {
        final ContainerNode protocolsContainer = (ContainerNode) dto;
        final MapNode protocolList = (MapNode) protocolsContainer
                .getChild(protocolYIId.getLastPathArgument()).get();
        final Collection<MapEntryNode> protocolsCollection = protocolList.getValue();
        final WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();

        for (final MapEntryNode protocolEntry : protocolsCollection) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> bi = serializer.fromNormalizedNode(protocolYIId,
                protocolEntry);
            if (bi != null) {
                final Protocol protocol = (Protocol) bi.getValue();
                processProtocol(protocol, wtx);
            }
        }
        try {
            wtx.commit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Protocol", e);
        }
    }

    private static void processProtocol(final Protocol protocol, final WriteTransaction wtx) {
        final KeyedInstanceIdentifier<Protocol, ProtocolKey> topologyIIdKeyed =
                BGP_PROTOCOLS_IID.child(Protocol.class, protocol.key());
        wtx.mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, topologyIIdKeyed, protocol);
    }
}
