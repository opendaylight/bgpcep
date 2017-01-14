/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.neighbor.set.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.PrefixSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.prefix.set.Prefix;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.tag.set.TagSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.tag.set.tag.set.Tag;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry per Openconfig Routing Policies defined under Config DS
 */
public final class OpenconfigRoutingPolicy implements ClusteredDataTreeChangeListener<RoutingPolicy>,
    OpenconfigPolicyConsumer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OpenconfigRoutingPolicy.class);

    @GuardedBy("this")
    private final ListenerRegistration<OpenconfigRoutingPolicy> registration;
    @GuardedBy("this")
    private Map<String, List<Statement>>policyDefinitions;
    @GuardedBy("this")
    private Map<String, List<IpAddress>> neighborsSets;
    @GuardedBy("this")
    private Map<String, List<IpPrefix>> prefixesSets;
    @GuardedBy("this")
    private Map<String, List<TagType>> tagsSets;

    public OpenconfigRoutingPolicy(final DataBroker dataBroker) {
        final InstanceIdentifier<RoutingPolicy> policiesIID = InstanceIdentifier.create(RoutingPolicy.class);
        this.registration = Preconditions.checkNotNull(dataBroker).registerDataTreeChangeListener(
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, policiesIID), this);
        LOG.info("Routing Policy Provider {} started.", policiesIID);
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.info("Closing Routing Policy Provider");
        if (this.registration != null) {
            this.registration.close();
        }
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<RoutingPolicy>> changes) {
        LOG.debug("Received data change to OpenconfigRoutingPolicy {}", changes);
        final DataObjectModification<RoutingPolicy> rootNode = Iterables.getOnlyElement(changes).getRootNode();
        LOG.trace("Routing Policy has changed: {}", rootNode);
        for (final DataObjectModification<? extends DataObject> dataObjectModification : rootNode.getModifiedChildren()) {
            if (dataObjectModification.getDataType().equals(DefinedSets.class)) {
                handleSets(((DataObjectModification<DefinedSets>) dataObjectModification).getDataAfter());
            } else if (dataObjectModification.getDataType().equals(PolicyDefinitions.class)) {
                final PolicyDefinitions policyDefinitions =
                    ((DataObjectModification<PolicyDefinitions>) dataObjectModification).getDataAfter();
                handlePolicyDefinitions(policyDefinitions.getPolicyDefinition());
            }
        }
    }

    private synchronized void handlePolicyDefinitions(@Nullable final List<PolicyDefinition> policyDefinitions) {
        if (policyDefinitions == null || policyDefinitions.isEmpty()) {
            return;
        }
        final ImmutableMap.Builder<String, List<Statement>> builder = new ImmutableMap.Builder<>();
        policyDefinitions.forEach(policy -> builder.put(policy.getName(),
            ImmutableList.copyOf(policy.getStatements().getStatement())));

        this.policyDefinitions = builder.build();
    }

    private synchronized void handleSets(final DefinedSets definedSets) {
        final List<NeighborSet> neighborsSets = definedSets.getNeighborSets().getNeighborSet();
        final ImmutableMap.Builder<String, List<IpAddress>> builderN = new ImmutableMap.Builder<>();
        neighborsSets.forEach(neighborSet -> builderN.put(neighborSet.getNeighborSetName(),
            ImmutableList.copyOf(neighborSet.getNeighbor()
            .stream().map(Neighbor::getAddress).collect(Collectors.toList()))));
        this.neighborsSets = builderN.build();

        final List<PrefixSet> prefixesSets = definedSets.getPrefixSets().getPrefixSet();
        final ImmutableMap.Builder<String, List<IpPrefix>> builderP = new ImmutableMap.Builder<>();
        prefixesSets.forEach(prefixSet -> builderP.put(prefixSet.getKey().getPrefixSetName(),
            ImmutableList.copyOf(prefixSet.getPrefix().stream().map(Prefix::getIpPrefix).collect(Collectors.toList()))));
        this.prefixesSets = builderP.build();

        final List<TagSet> tagsSets = definedSets.getTagSets().getTagSet();
        final ImmutableMap.Builder<String, List<TagType>> builderT = new ImmutableMap.Builder<>();
        tagsSets.forEach(tagSet -> builderT.put(tagSet.getKey().getTagSetName(),
            ImmutableList.copyOf(tagSet.getTag()
                .stream().map(Tag::getValue).collect(Collectors.toList()))));
        this.tagsSets = builderT.build();
    }

    @Override
    public synchronized List<Statement> getPolicy(final String policyName) {
        return this.policyDefinitions.get(policyName);
    }

    @Override
    public synchronized boolean matchPrefix(final String prefixSetName, final IpPrefix prefix) {
        final List<IpPrefix> set = this.prefixesSets.get(prefixSetName);
        return set != null && set.contains(prefix);
    }

    @Override
    public synchronized boolean matchNeighbor(final String neighborSetName, final IpAddress ipAddress) {
        final List<IpAddress> set = this.neighborsSets.get(neighborSetName);
        return set != null && set.contains(ipAddress);
    }

    @Override
    public synchronized boolean matchTag(final String tagSetName, final TagType tagType) {
        final List<TagType> set = this.tagsSets.get(tagSetName);
        return set != null && set.contains(tagType);
    }
}