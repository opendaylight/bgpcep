/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRIBRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class BGPRIBPolicyImpl implements BGPRIBRoutingPolicy {
    private final DefaultPolicyType defaultExportPolicy;
    private final DefaultPolicyType defaultImportPolicy;
    private final List<String> exportPolicy;
    private final List<String> importPolicy;
    private final StatementRegistryConsumer policyRegistry;
    private final OpenconfigPolicyConsumer policyProvider;
    private final OpenconfigPolicyConsumer openconfigPolicy;
    private final PolicyRIBBaseParameters ribBaseParameters;

    BGPRIBPolicyImpl(final OpenconfigPolicyConsumer openconfigPolicy, final StatementRegistryConsumer policyRegistry,
        final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
        final OpenconfigPolicyConsumer policyProvider, final DefaultPolicyType defaultExportPolicy,
        final DefaultPolicyType defaultImportPolicy, final List<String> exportPolicy, final List<String> importPolicy) {
        this.policyRegistry = Preconditions.checkNotNull(policyRegistry);
        this.policyProvider = Preconditions.checkNotNull(policyProvider);
        this.defaultExportPolicy = Preconditions.checkNotNull(defaultExportPolicy);
        this.defaultImportPolicy = Preconditions.checkNotNull(defaultImportPolicy);
        this.exportPolicy = Preconditions.checkNotNull(exportPolicy);
        this.importPolicy = Preconditions.checkNotNull(importPolicy);
        this.openconfigPolicy = Preconditions.checkNotNull(openconfigPolicy);
        this.ribBaseParameters = new PolicyRIBBaseParametersImpl(localAs, originatorId, clusterId);
    }

    @Override
    public Optional<ContainerNode> applyImportPolicies(final BGPRouteBaseParameters policyParameters,
        final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = new RouteAttributeContainer(attributes, false);
        for (final String policyName : this.importPolicy) {
            final List<Statement> statements = this.policyProvider.getPolicy(policyName);
            if (statements == null) {
                continue;
            }
            for (final Statement statement : statements) {
                final ContainerNode currentAttributes = finalAttributes.getAttributes();
                if (currentAttributes == null) {
                    return Optional.empty();
                }
                finalAttributes = this.policyRegistry.applyImportStatement(this.openconfigPolicy,
                    this.ribBaseParameters, policyParameters, finalAttributes, statement);
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
    public Optional<ContainerNode> applyExportPolicies(final BGPRouteBaseExportParameters policyParameters,
        final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = new RouteAttributeContainer(attributes, false);
        for (final String policyName : this.exportPolicy) {
            final List<Statement> statements = this.policyProvider.getPolicy(policyName);
            if (statements == null) {
                continue;
            }
            for (final Statement statement : statements) {
                final ContainerNode currentAttributes = finalAttributes.getAttributes();
                if (currentAttributes == null) {
                    return Optional.empty();
                }
                finalAttributes = this.policyRegistry.applyExportStatement(
                    this.openconfigPolicy,
                    this.ribBaseParameters,
                    policyParameters,
                    finalAttributes,
                    statement);
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
