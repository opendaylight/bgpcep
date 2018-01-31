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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AsPathSegment;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match a set of AS (All, ANY, INVERT).
 */
public final class MatchAsPathSetHandler implements BgpConditionsPolicy<MatchAsPathSet> {
    private static final InstanceIdentifier<AsPathSets> AS_PATHS_SETS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .child(AsPathSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, AsPathSet> sets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, AsPathSet>() {
                @Override
                public AsPathSet load(final String key) throws ExecutionException, InterruptedException {
                    return loadSets(key);
                }
            });

    public MatchAsPathSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private AsPathSet loadSets(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.dataBroker.newReadOnlyTransaction();
        final Optional<AsPathSet> result = tr.read(LogicalDatastoreType.CONFIGURATION,
                AS_PATHS_SETS_IID.child(AsPathSet.class, new AsPathSetKey(key))).get();
        return result.orNull();
    }

    @Override
    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final MatchAsPathSet conditions) {
        return matchAsPathSetCondition(attributes.getAsPath(), conditions.getAsPathSet(),
                conditions.getMatchSetOptions());
    }


    @Override
    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final MatchAsPathSet conditions) {
        return matchAsPathSetCondition(attributes.getAsPath(), conditions.getAsPathSet(),
                conditions.getMatchSetOptions());

    }


    private boolean matchAsPathSetCondition(final AsPath asPath, final String asPathSetName,
            final MatchSetOptionsType matchSetOptions) {
        if (asPath == null) {
            return false;
        }
        final AsPathSet asPathSetFilter = this.sets.getUnchecked(StringUtils
                .substringBetween(asPathSetName, "=\"", "\""));

        final List<Segments> segments = asPath.getSegments();
        if (asPathSetFilter == null || segments == null) {
            return false;
        }

        final List<AsNumber> l1 = segments.stream()
                .map(AsPathSegment::getAsSequence)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<AsNumber> l2 = segments.stream()
                .map(AsPathSegment::getAsSet)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<AsNumber> allAs = Stream.of(l1, l2).flatMap(Collection::stream).collect(Collectors.toList());

        final List<AsNumber> asPathSetFilterList = asPathSetFilter.getAsPathSetMember();
        if (matchSetOptions.equals(MatchSetOptionsType.ALL)) {
            return allAs.containsAll(asPathSetFilterList)
                    && asPathSetFilterList.containsAll(allAs);
        }
        final boolean noneInCommon = Collections.disjoint(allAs, asPathSetFilterList);
        if (matchSetOptions.equals(MatchSetOptionsType.ANY)) {
            return !noneInCommon;
        }
        //(matchSetOptions.equals(MatchSetOptionsType.INVERT))
        return noneInCommon;
    }
}