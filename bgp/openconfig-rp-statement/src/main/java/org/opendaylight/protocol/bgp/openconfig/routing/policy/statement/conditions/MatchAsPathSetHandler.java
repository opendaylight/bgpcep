/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchAsPathSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.AsPathSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.as.path.sets.AsPathSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.as.path.sets.AsPathSetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Match a set of AS (All, ANY, INVERT).
 */
public final class MatchAsPathSetHandler implements BgpConditionsPolicy<MatchAsPathSet, AsPath> {
    private static final DataObjectIdentifier<AsPathSets> AS_PATHS_SETS_IID =
        DataObjectIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class)
            .child(DefinedSets.class)
            .augmentation(DefinedSets1.class)
            .child(BgpDefinedSets.class)
            .child(AsPathSets.class)
            .build();

    private final LoadingCache<String, AsPathSet> sets = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public AsPathSet load(final String key) throws ExecutionException, InterruptedException {
            return loadSet(key);
        }
    });
    private final DataBroker dataBroker;

    @NonNullByDefault
    public MatchAsPathSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private @Nullable AsPathSet loadSet(final String key) throws ExecutionException, InterruptedException {
        final FluentFuture<Optional<AsPathSet>> future;
        try (var tr = dataBroker.newReadOnlyTransaction()) {
            future = tr.read(LogicalDatastoreType.CONFIGURATION,
                AS_PATHS_SETS_IID.toBuilder().child(AsPathSet.class, new AsPathSetKey(key)).build());
        }
        return future.get().orElse(null);
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final AsPath asPath,
            final MatchAsPathSet conditions) {
        return matchAsPathSetCondition(asPath, conditions.getAsPathSet(), conditions.getMatchSetOptions());
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final AsPath asPath,
            final MatchAsPathSet conditions) {
        return matchAsPathSetCondition(asPath, conditions.getAsPathSet(), conditions.getMatchSetOptions());
    }

    @Override
    public AsPath getConditionParameter(final Attributes attributes) {
        return attributes.getAsPath();
    }

    private boolean matchAsPathSetCondition(final AsPath asPath, final String asPathSetName,
            final MatchSetOptionsType matchSetOptions) {
        if (asPath == null) {
            return false;
        }
        // TODO: can we ditch commons-lang3 here?
        final var asPathSetFilter = sets.getUnchecked(StringUtils.substringBetween(asPathSetName, "=\"", "\""));
        final var segments = asPath.getSegments();
        if (asPathSetFilter == null || segments == null) {
            return false;
        }

        // TODO: can we do something smarter than collecting upfront?
        final var allAs = new ArrayList<AsNumber>();
        for (var segment : segments) {
            final var asSequence = segment.getAsSequence();
            if (asSequence != null) {
                allAs.addAll(asSequence);
            }
            final var asSet = segment.getAsSet();
            if (asSet != null) {
                allAs.addAll(asSet);
            }
        }

        // FIXME: handle null
        final var asPathSetFilterList = asPathSetFilter.getAsPathSetMember();
        return switch (matchSetOptions) {
            case ALL -> allAs.containsAll(asPathSetFilterList) && asPathSetFilterList.containsAll(allAs);
            case ANY -> !Collections.disjoint(allAs, asPathSetFilterList);
            case INVERT -> Collections.disjoint(allAs, asPathSetFilterList);
        };
    }
}
