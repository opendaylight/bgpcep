/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerFalse;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.Statements;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class BGPRibPolicyImpl implements BGPRibRoutingPolicy {
    private static final InstanceIdentifier<RoutingPolicy> ROUTING_POLICY_IID
            = InstanceIdentifier.create(RoutingPolicy.class);
    private static final List<String> EMPTY_POLICY = Collections.emptyList();
    private final DefaultPolicyType defaultExportPolicy;
    private final DefaultPolicyType defaultImportPolicy;
    private final List<String> exportPolicy;
    private final List<String> importPolicy;
    private final StatementRegistryConsumer policyRegistry;
    private final RouteEntryBaseAttributes ribBaseParameters;
    private final DataBroker databroker;
    private final LoadingCache<String, List<Statement>> statements = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<Statement>>() {
                @Override
                public List<Statement> load(final String key) throws ExecutionException, InterruptedException {
                    return loadStatements(key);
                }
            });

    BGPRibPolicyImpl(final DataBroker databroker, final StatementRegistryConsumer policyRegistry,
            final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
            final Config policyConfig) {
        this.policyRegistry = requireNonNull(policyRegistry);
        this.databroker = requireNonNull(databroker);
        requireNonNull(policyConfig);


        List<String> epolicy = policyConfig.getExportPolicy();
        if (epolicy == null) {
            epolicy = EMPTY_POLICY;
        }
        List<String> ipolicy = policyConfig.getImportPolicy();
        if (ipolicy == null) {
            ipolicy = EMPTY_POLICY;
        }

        this.defaultExportPolicy = requireNonNull(policyConfig.getDefaultExportPolicy());
        this.defaultImportPolicy = requireNonNull(policyConfig.getDefaultImportPolicy());
        this.exportPolicy = requireNonNull(epolicy);
        this.importPolicy = requireNonNull(ipolicy);
        this.ribBaseParameters = new PolicyRIBBaseParametersImpl(localAs, originatorId, clusterId);
    }

    private List<Statement> loadStatements(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.databroker.newReadOnlyTransaction();
        final com.google.common.base.Optional<Statements> result =
                tr.read(LogicalDatastoreType.CONFIGURATION, ROUTING_POLICY_IID.child(PolicyDefinitions.class)
                        .child(PolicyDefinition.class, new PolicyDefinitionKey(key)).child(Statements.class)).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getStatement();
    }

    @Override
    public Optional<ContainerNode> applyImportPolicies(final BGPRouteEntryImportParameters policyParameters,
            final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = routeAttributeContainerFalse(attributes);
        for (final String policyName : this.importPolicy) {
            for (final Statement statement : this.statements.getUnchecked(policyName)) {
                final ContainerNode currentAttributes = finalAttributes.getAttributes();
                if (currentAttributes == null) {
                    return Optional.empty();
                }
                finalAttributes = this.policyRegistry
                        .applyImportStatement(this.ribBaseParameters, policyParameters, finalAttributes, statement);
            }
        }
        if (!finalAttributes.anyConditionSatisfied()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultImportPolicy)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(finalAttributes.getAttributes());
    }

    @Override
    public Optional<ContainerNode> applyExportPolicies(final BGPRouteEntryExportParameters policyParameters,
            final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = routeAttributeContainerFalse(attributes);
        for (final String policyName : this.exportPolicy) {
            for (final Statement statement : this.statements.getUnchecked(policyName)) {
                final ContainerNode currentAttributes = finalAttributes.getAttributes();
                if (currentAttributes == null) {
                    return Optional.empty();
                }
                finalAttributes = this.policyRegistry.applyExportStatement(
                        this.ribBaseParameters, policyParameters, finalAttributes, statement);
            }
        }
        if (!finalAttributes.anyConditionSatisfied()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultExportPolicy)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(finalAttributes.getAttributes());
    }
}
