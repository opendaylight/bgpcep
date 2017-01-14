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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRIBRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class BGPRIBPolicyImpl implements BGPRIBRoutingPolicy {
    private final DefaultPolicyType defaultExportPolicy;
    private final DefaultPolicyType defaultImportPolicy;
    private final List<String> exportPolicy;
    private final List<String> importPolicy;
    private final StatementRegistryConsumer policyRegistry;
    private final OpenconfigPolicyConsumer policyProvider;
    private final long localAs;
    private final Ipv4Address originatorId;
    private final ClusterIdentifier clusterId;
    private final OpenconfigPolicyConsumer openconfigPolicy;

    BGPRIBPolicyImpl(final OpenconfigPolicyConsumer openconfigPolicy, final StatementRegistryConsumer policyRegistry,
        final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
        final OpenconfigPolicyConsumer policyProvider, final DefaultPolicyType defaultExportPolicy,
        final DefaultPolicyType defaultImportPolicy, final List<String> exportPolicy, final List<String> importPolicy) {
        this.localAs = Preconditions.checkNotNull(localAs);
        this.originatorId = Preconditions.checkNotNull(originatorId);
        this.clusterId = Preconditions.checkNotNull(clusterId);
        this.policyRegistry = Preconditions.checkNotNull(policyRegistry);
        this.policyProvider = Preconditions.checkNotNull(policyProvider);
        this.defaultExportPolicy = Preconditions.checkNotNull(defaultExportPolicy);
        this.defaultImportPolicy = Preconditions.checkNotNull(defaultImportPolicy);
        this.exportPolicy = Preconditions.checkNotNull(exportPolicy);
        this.importPolicy = Preconditions.checkNotNull(importPolicy);
        this.openconfigPolicy = Preconditions.checkNotNull(openconfigPolicy);
    }

    @Override
    public Optional<ContainerNode> applyImportPolicies(final PathArgument key, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = new RouteAttributeContainer(attributes, false);
        for (final String policyName : this.importPolicy) {
            final List<Statement> statements = this.policyProvider.getPolicy(policyName);
            if (statements == null) {
                continue;
            }
            for (final Statement statement : statements) {
                final Optional<ContainerNode> currentAttributes = finalAttributes.getAttributes();
                if(currentAttributes.isPresent()) {
                    finalAttributes = this.policyRegistry.applyImportStatement(this.openconfigPolicy, this.localAs,
                        this.originatorId, this.clusterId, key, fromPeerId, fromPeerRole, finalAttributes, statement);
                } else {
                    return finalAttributes.getAttributes();
                }
            }
        }
        if (!finalAttributes.anyConditionSastified()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultImportPolicy)) {
                return null;
            }
        }
        return finalAttributes.getAttributes();
    }

    @Override
    public Optional<ContainerNode> applyExportPolicies(final PathArgument key, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final PeerId toPeer, final PeerRole toPeerRole, final ContainerNode attributes) {
        RouteAttributeContainer finalAttributes = new RouteAttributeContainer(attributes, false);
        for (final String policyName : this.exportPolicy) {
            final List<Statement> statements = this.policyProvider.getPolicy(policyName);
            for (final Statement statement : statements) {
                final Optional<ContainerNode> currentAttributes = finalAttributes.getAttributes();
                if(currentAttributes.isPresent()) {
                    finalAttributes = this.policyRegistry.applyExportStatement(this.openconfigPolicy, this.localAs,
                        this.originatorId, this.clusterId, key, fromPeerId, fromPeerRole, toPeer, toPeerRole,
                        finalAttributes, statement);
                } else {
                    return finalAttributes.getAttributes();
                }
            }
        }
        if (!finalAttributes.anyConditionSastified()) {
            if (DefaultPolicyType.REJECTROUTE.equals(this.defaultExportPolicy)) {
                return null;
            }
        }
        return finalAttributes.getAttributes();
    }
}
