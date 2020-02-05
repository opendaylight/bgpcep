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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.BgpOriginatorIdSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchOriginatorIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.originator.id.set.OriginatorIdSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.originator.id.set.OriginatorIdSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.routing.policy.defined.sets.bgp.defined.sets.OriginatorIdSets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match an Originator Id(ANY, INVERT).
 */
public final class MatchOriginatorIdSetHandler
        implements BgpConditionsAugmentationPolicy<MatchOriginatorIdSetCondition, OriginatorId> {
    private static final InstanceIdentifier<OriginatorIdSets> ORIGINATOR_ID_SETS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .augmentation(BgpOriginatorIdSets.class).child(OriginatorIdSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, OriginatorIdSet> sets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, OriginatorIdSet>() {
                @Override
                public OriginatorIdSet load(final String key) throws ExecutionException, InterruptedException {
                    return loadSets(key);
                }
            });

    public MatchOriginatorIdSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private OriginatorIdSet loadSets(final String key) throws ExecutionException, InterruptedException {
        final ReadTransaction tr = this.dataBroker.newReadOnlyTransaction();
        final Optional<OriginatorIdSet> result = tr.read(LogicalDatastoreType.CONFIGURATION,
                ORIGINATOR_ID_SETS_IID.child(OriginatorIdSet.class, new OriginatorIdSetKey(key))).get();
        return result.orElse(null);
    }

    @Override
    public boolean matchImportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final OriginatorId originatorId,
            final MatchOriginatorIdSetCondition conditions) {

        return matchOriginatorCondition(routeEntryInfo.getOriginatorId(), originatorId,
                conditions.getMatchOriginatorIdSetCondition());
    }

    @Override
    public boolean matchExportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final OriginatorId originatorId,
            final MatchOriginatorIdSetCondition conditions) {
        return matchOriginatorCondition(routeEntryInfo.getOriginatorId(), originatorId,
                conditions.getMatchOriginatorIdSetCondition());
    }

    @Override
    public OriginatorId getConditionParameter(final Attributes attributes) {
        return attributes.getOriginatorId();
    }

    private boolean matchOriginatorCondition(
            final Ipv4Address localOriginatorId,
            final OriginatorId originatorId, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .odl.bgp._default.policy.rev200120.match.originator.id.set.condition.grouping
                .MatchOriginatorIdSetCondition condition) {

        final OriginatorIdSet originatorIdSet = this.sets.getUnchecked(StringUtils
                .substringBetween(condition.getOriginatorIdSet(), "=\"", "\""));

        if (originatorIdSet == null) {
            return false;
        }
        boolean found = false;
        if (originatorId != null) {
            final Ipv4Address remOrigin = originatorId.getOriginator();

            if (originatorIdSet.getLocal() != null && localOriginatorId.equals(remOrigin)) {
                found = true;
            }
            if (!found && originatorIdSet.getOriginatorId() != null) {
                found = originatorIdSet.getOriginatorId().contains(remOrigin);
            }
        }
        final MatchSetOptionsRestrictedType matchOption = condition.getMatchSetOptions();
        return matchOption.equals(MatchSetOptionsRestrictedType.ANY) && found
                || matchOption.equals(MatchSetOptionsRestrictedType.INVERT) && !found;
    }
}
