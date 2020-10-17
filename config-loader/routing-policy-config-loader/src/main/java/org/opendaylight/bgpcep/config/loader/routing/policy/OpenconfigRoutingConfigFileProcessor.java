/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.routing.policy;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.config.loader.spi.AbstractConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class OpenconfigRoutingConfigFileProcessor extends AbstractConfigFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(OpenconfigRoutingConfigFileProcessor.class);
    private static final SchemaPath POLICY_SCHEMA_PATH = SchemaPath.create(true, RoutingPolicy.QNAME);

    private final DataBroker dataBroker;

    @Inject
    public OpenconfigRoutingConfigFileProcessor(final ConfigLoader configLoader, final DataBroker dataBroker) {
        super("Routing Policy", configLoader);
        this.dataBroker = requireNonNull(dataBroker);
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
        return POLICY_SCHEMA_PATH;
    }

    @Override
    public void loadConfiguration(final NormalizedNode<?, ?> dto) {
        final RoutingPolicy routingPolicy = (RoutingPolicy) configLoader.getBindingNormalizedNodeSerializer()
            .fromNormalizedNode(YangInstanceIdentifier.create(new NodeIdentifier(RoutingPolicy.QNAME)), dto).getValue();
        final WriteTransaction wtx = this.dataBroker.newWriteOnlyTransaction();
        // FIXME: this is a top-level container, we should be able to just put()
        wtx.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(RoutingPolicy.class), routingPolicy);

        try {
            wtx.commit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Routing Policy config", e);
        }
    }
}
