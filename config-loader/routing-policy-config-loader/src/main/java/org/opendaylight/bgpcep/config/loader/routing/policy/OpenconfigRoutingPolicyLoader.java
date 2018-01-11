/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.routing.policy;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
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

public final class OpenconfigRoutingPolicyLoader implements ConfigFileProcessor, AutoCloseable {
    @VisibleForTesting
    static final InstanceIdentifier<PolicyDefinitions> POLICY_DEFINITIONS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(PolicyDefinitions.class);
    private static final InstanceIdentifier<PolicyDefinition> POLICY_DEFINITION_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(PolicyDefinitions.class)
            .child(PolicyDefinition.class);
    private static final Logger LOG = LoggerFactory.getLogger(OpenconfigRoutingPolicyLoader.class);
    private static final SchemaPath POLICY_SCHEMA_PATH = SchemaPath
            .create(true, RoutingPolicy.QNAME, PolicyDefinitions.QNAME);
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final ConfigLoader configLoader;
    private final DataBroker dataBroker;
    private final YangInstanceIdentifier policyDefinitionYIId;
    @GuardedBy("this")
    private AbstractRegistration registration;

    public OpenconfigRoutingPolicyLoader(final ConfigLoader configLoader, final DataBroker dataBroker) {
        requireNonNull(configLoader);
        this.configLoader = requireNonNull(configLoader);
        this.dataBroker = requireNonNull(dataBroker);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.policyDefinitionYIId = this.bindingSerializer.toYangInstanceIdentifier(POLICY_DEFINITION_IID);

    }

    public synchronized void init() {
        this.registration = this.configLoader.registerConfigFile(this);
        LOG.info("Routing Policy Config Loader service initiated");
    }

    @Override
    public void close() throws Exception {
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }

    @Nonnull
    @Override
    public SchemaPath getSchemaPath() {
        return POLICY_SCHEMA_PATH;
    }

    @Override
    public synchronized void loadConfiguration(@Nonnull final NormalizedNode<?, ?> dto) {
        final ContainerNode policiesConfigsContainer = (ContainerNode) dto;
        final MapNode policies = (MapNode) policiesConfigsContainer
                .getChild(policyDefinitionYIId.getLastPathArgument()).orElse(null);
        if (policies == null) {
            return;
        }

        final Collection<MapEntryNode> policiesConfig = policies.getValue();

        final WriteTransaction wtx = this.dataBroker.newWriteOnlyTransaction();
        processPoliciesDefinitions(policiesConfig, wtx);
        try {
            wtx.submit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Routing Policy config", e);
        }
    }

    private void processPoliciesDefinitions(final Collection<MapEntryNode> policies, final WriteTransaction wtx) {
        policies.stream().map(topology -> this.bindingSerializer
                .fromNormalizedNode(this.policyDefinitionYIId, topology))
                .filter(Objects::nonNull)
                .forEach(bi -> storePolicy((PolicyDefinition) bi.getValue(), wtx));
    }

    private synchronized void storePolicy(final PolicyDefinition policyDefinition, final WriteTransaction wtx) {
        final KeyedInstanceIdentifier<PolicyDefinition, PolicyDefinitionKey> policyDefinitionKIdd =
                POLICY_DEFINITIONS_IID.child(PolicyDefinition.class,
                        new PolicyDefinitionKey(policyDefinition.getName()));
        wtx.put(LogicalDatastoreType.CONFIGURATION, policyDefinitionKIdd, policyDefinition,
                WriteTransaction.CREATE_MISSING_PARENTS);
    }
}
