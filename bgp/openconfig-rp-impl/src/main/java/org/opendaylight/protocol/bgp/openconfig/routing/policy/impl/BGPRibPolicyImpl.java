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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.Statements;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BGPRibPolicyImpl implements BGPRibRoutingPolicy {
    private static final InstanceIdentifier<RoutingPolicy> ROUTING_POLICY_IID
            = InstanceIdentifier.create(RoutingPolicy.class);
    private static final List<String> DEFAULT_IMPORT_POLICY = Collections.singletonList("default-odl-import-policy");
    private static final List<String> DEFAULT_EXPORT_POLICY = Collections.singletonList("default-odl-export-policy");
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
            epolicy = DEFAULT_EXPORT_POLICY;
        }
        List<String> ipolicy = policyConfig.getImportPolicy();
        if (ipolicy == null) {
            ipolicy = DEFAULT_IMPORT_POLICY;
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
    public Optional<Attributes> applyImportPolicies(final BGPRouteEntryImportParameters policyParameters,
            final Attributes attributes, final Class<? extends AfiSafiType> afiSafiType) {
        RouteAttributeContainer currentAttributes = routeAttributeContainerFalse(attributes);
        for (final String policyName : this.importPolicy) {
            for (final Statement statement : this.statements.getUnchecked(policyName)) {
                currentAttributes = this.policyRegistry
                        .applyImportStatement(this.ribBaseParameters, afiSafiType, policyParameters, currentAttributes,
                                statement);
            }
        }
        if (!currentAttributes.anyConditionSatisfied()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultImportPolicy)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(currentAttributes.getAttributes());
    }

    @Override
    public Optional<Attributes> applyExportPolicies(final BGPRouteEntryExportParameters policyParameters,
            final Attributes attributes, final Class<? extends AfiSafiType> afiSafi) {
        RouteAttributeContainer currentAttributes = routeAttributeContainerFalse(attributes);
        for (final String policyName : this.exportPolicy) {
            for (final Statement statement : this.statements.getUnchecked(policyName)) {
                currentAttributes = this.policyRegistry.applyExportStatement(
                        this.ribBaseParameters, afiSafi, policyParameters, currentAttributes, statement);
            }
        }
        if (!currentAttributes.anyConditionSatisfied()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultExportPolicy)) {
                return Optional.empty();
            }
        }

        return Optional.ofNullable(currentAttributes.getAttributes());
    }
}
