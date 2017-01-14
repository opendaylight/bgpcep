/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import com.google.common.base.Preconditions;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenconfigRoutingPolicyLoader implements TransactionChainListener, ConfigFileProcessor,
    AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OpenconfigRoutingPolicyLoader.class);

    private static final SchemaPath POLICY_SCHEMA_PATH = SchemaPath.create(true, RoutingPolicy.QNAME);
    private final AbstractRegistration registration;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final YangInstanceIdentifier policyYIId;
    private final InstanceIdentifier<PolicyDefinitions> policyDefinitionsIId;
    private final BindingTransactionChain transactionChain;

    public OpenconfigRoutingPolicyLoader(final ConfigLoader configLoader, final DataBroker dataBroker) {
        Preconditions.checkNotNull(configLoader);
        this.transactionChain = Preconditions.checkNotNull(dataBroker).createTransactionChain(this);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        final InstanceIdentifier<RoutingPolicy> routingPolicy = InstanceIdentifier.create(RoutingPolicy.class);
        this.policyDefinitionsIId = routingPolicy.child(PolicyDefinitions.class);
        this.policyYIId = this.bindingSerializer.toYangInstanceIdentifier(this.policyDefinitionsIId);
        this.registration = configLoader.registerConfigFile(this);
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
        this.transactionChain.close();
    }

    @Nonnull
    @Override
    public SchemaPath getSchemaPath() {
        return POLICY_SCHEMA_PATH;
    }

    @Override
    public synchronized void loadConfiguration(@Nonnull final NormalizedNode<?, ?> dto) {
        final Map.Entry<InstanceIdentifier<?>, DataObject> policyDefinitionsDto = this.bindingSerializer
            .fromNormalizedNode(this.policyYIId, dto);
        if (policyDefinitionsDto != null) {
            final PolicyDefinitions policyDefinitions = (PolicyDefinitions) policyDefinitionsDto.getValue();
            final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
            policyDefinitions.getPolicyDefinition().forEach(policyDefinition -> storePolicy(policyDefinition, wTx));
            wTx.submit();
        }
    }

    private synchronized void storePolicy(final PolicyDefinition policyDefinition, final WriteTransaction wTx) {
        final KeyedInstanceIdentifier<PolicyDefinition, PolicyDefinitionKey> policyDefinitionKIdd =
            this.policyDefinitionsIId.child(PolicyDefinition.class, new PolicyDefinitionKey
                (policyDefinition.getName()));
        wTx.put(LogicalDatastoreType.CONFIGURATION, policyDefinitionKIdd, policyDefinition,
            WriteTransaction.CREATE_MISSING_PARENTS);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
        final Throwable cause) {
        LOG.error("Transaction chain failed {}.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }
}
