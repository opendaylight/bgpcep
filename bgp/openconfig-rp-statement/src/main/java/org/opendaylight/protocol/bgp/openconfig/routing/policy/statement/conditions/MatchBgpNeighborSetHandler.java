/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.NeighborSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.BgpNeighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.MatchBgpNeighborCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.match.bgp.neighbor.grouping.MatchBgpNeighborSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match a set of Neighbors(ip address) (ANY, INVERT).
 */
public final class MatchBgpNeighborSetHandler
        implements BgpConditionsAugmentationPolicy<MatchBgpNeighborCondition, Void> {
    private static final InstanceIdentifier<NeighborSets> NEIGHBOR_SET_IID
            = InstanceIdentifier.create(RoutingPolicy.class)
            .child(DefinedSets.class)
            .child(NeighborSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, List<PeerId>> peerSets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<PeerId>>() {
                @Override
                public List<PeerId> load(final String key) throws ExecutionException, InterruptedException {
                    return loadRoleSets(key);
                }
            });

    public MatchBgpNeighborSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private List<PeerId> loadRoleSets(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.dataBroker.newReadOnlyTransaction();
        final Optional<NeighborSet> result = tr.read(LogicalDatastoreType.CONFIGURATION,
                NEIGHBOR_SET_IID.child(NeighborSet.class, new NeighborSetKey(key))).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getNeighbor().stream()
                .map(nei -> RouterIds.createPeerId(nei.getAddress()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean matchImportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters,
            final Void nonAttributres,
            final MatchBgpNeighborCondition conditions) {
        return matchBgpNeighborSetCondition(importParameters.getFromPeerId(), null,
                conditions.getMatchBgpNeighborSet());
    }

    @Override
    public boolean matchExportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final Void nonAttributres,
            final MatchBgpNeighborCondition conditions) {
        return matchBgpNeighborSetCondition(exportParameters.getFromPeerId(), exportParameters.getToPeerId(),
                conditions.getMatchBgpNeighborSet());
    }

    private boolean matchBgpNeighborSetCondition(
            @Nonnull final PeerId fromPeerId,
            @Nullable final PeerId toPeerId,
            final MatchBgpNeighborSet matchBgpNeighborSet) {

        final BgpNeighbor from = matchBgpNeighborSet.getFromNeighbor();
        Boolean match = null;
        if (from != null) {
            match = checkMatch(from.getNeighborSet(), fromPeerId, from.getMatchSetOptions());
        }

        if (match != null && !match) {
            return false;
        }

        final BgpNeighbor to = matchBgpNeighborSet.getToNeighbor();
        if (to != null) {
            match = checkMatch(to.getNeighborSet(), toPeerId, to.getMatchSetOptions());
        }

        return match;
    }

    private boolean checkMatch(
            final String neighborSetName,
            final PeerId peerId,
            final MatchSetOptionsRestrictedType matchSetOptions) {
        final List<PeerId> roles = this.peerSets.getUnchecked(StringUtils
                .substringBetween(neighborSetName, "=\"", "\""));

        final boolean found = roles.contains(peerId);
        if (MatchSetOptionsRestrictedType.ANY.equals(matchSetOptions)) {
            return found;
        }
        //INVERT
        return !found;
    }

    @Override
    public Void getConditionParameter(final Attributes attributes) {
        return null;
    }
}
