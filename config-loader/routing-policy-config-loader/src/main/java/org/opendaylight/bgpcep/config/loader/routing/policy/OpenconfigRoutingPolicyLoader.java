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
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenconfigRoutingPolicyLoader implements ConfigFileProcessor, AutoCloseable {
    public static final InstanceIdentifier<RoutingPolicy> ROUTING_POLICY_IID
            = InstanceIdentifier.create(RoutingPolicy.class);
    private static final Logger LOG = LoggerFactory.getLogger(OpenconfigRoutingPolicyLoader.class);
    private static final SchemaPath POLICY_SCHEMA_PATH = SchemaPath.create(true, RoutingPolicy.QNAME);
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final ConfigLoader configLoader;
    private final DataBroker dataBroker;
    private final YangInstanceIdentifier routingPolicyYiid;
    @GuardedBy("this")
    private AbstractRegistration registration;

    public OpenconfigRoutingPolicyLoader(final ConfigLoader configLoader, final DataBroker dataBroker) {
        requireNonNull(configLoader);
        this.configLoader = requireNonNull(configLoader);
        this.dataBroker = requireNonNull(dataBroker);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.routingPolicyYiid = this.bindingSerializer.toYangInstanceIdentifier(ROUTING_POLICY_IID);
    }

    public synchronized void init() {
        this.registration = this.configLoader.registerConfigFile(this);
        LOG.info("Routing Policy Config Loader service initiated");
    }

    @Override
    public synchronized void close() {
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }

    @Override
    public SchemaPath getSchemaPath() {
        return POLICY_SCHEMA_PATH;
    }

    @Override
    public void loadConfiguration(final NormalizedNode<?, ?> dto) {
        final RoutingPolicy routingPolicy = (RoutingPolicy) this.bindingSerializer
                .fromNormalizedNode(this.routingPolicyYiid, dto).getValue();
        final WriteTransaction wtx = this.dataBroker.newWriteOnlyTransaction();
        wtx.merge(LogicalDatastoreType.CONFIGURATION, ROUTING_POLICY_IID, routingPolicy);

        try {
            wtx.commit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Routing Policy config", e);
        }
    }
}
