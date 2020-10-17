/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.protocols;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.config.loader.spi.AbstractConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@Singleton
public final class ProtocolsConfigFileProcessor extends AbstractConfigFileProcessor {
    private static final QName NAME = QName.create(NetworkInstance.QNAME, "name").intern();

    // TODO: isn't there a better place where we can get this?
    @VisibleForTesting
    static final String GLOBAL_BGP_NAME = "global-bgp";

    private static final @NonNull NodeIdentifierWithPredicates GLOBAL_BGP =
        NodeIdentifierWithPredicates.of(NetworkInstance.QNAME, NAME, GLOBAL_BGP_NAME);
    private static final @NonNull YangInstanceIdentifier GLOBAL_BGP_PATH = YangInstanceIdentifier.create(
        NodeIdentifier.create(NetworkInstances.QNAME), NodeIdentifier.create(NetworkInstance.QNAME), GLOBAL_BGP);

    @Inject
    public ProtocolsConfigFileProcessor(final ConfigLoader configLoader, final DOMDataBroker dataBroker) {
        super("Protocols", configLoader, dataBroker);
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
    public Absolute fileRootSchema() {
        return Absolute.of(NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME);
    }

    @Override
    protected FluentFuture<? extends CommitInfo> loadConfiguration(final DOMDataBroker dataBroker,
            final NormalizedNode<?, ?> dto) {
        final ContainerNode protocolsContainer = (ContainerNode) dto;
        final MapNode protocols = (MapNode) protocolsContainer.getChild(new NodeIdentifier(Protocol.QNAME))
            .orElse(null);
        if (protocols == null) {
            return CommitInfo.emptyFluentFuture();
        }

        final DOMDataTreeWriteTransaction wtx = dataBroker.newWriteOnlyTransaction();

        // Ensure global-bgp exists
        wtx.merge(LogicalDatastoreType.CONFIGURATION, GLOBAL_BGP_PATH, Builders.mapEntryBuilder()
            .withNodeIdentifier(GLOBAL_BGP)
            .withChild(ImmutableNodes.leafNode(NAME, GLOBAL_BGP_NAME))
            .build());
        wtx.merge(LogicalDatastoreType.CONFIGURATION,
            GLOBAL_BGP_PATH.node(Protocols.QNAME).node(protocols.getIdentifier()), protocols);
        return wtx.commit();
    }
}
