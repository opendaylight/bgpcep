/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType.ANY;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.GenericConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.conditions.MatchPrefixSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.PrefixSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.PrefixSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.PrefixSetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.prefix.set.prefix.set.Prefix;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

final class GenericConditionPolicyHandler {
    private static final InstanceIdentifier<PrefixSets> PREFIXES_SET_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .child(PrefixSets.class);
    private static final QName IPV4_PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final QName IPV6_PREFIX_QNAME = QName.create(Ipv6Route.QNAME, "prefix").intern();
    private static final String EXACT = "exact";
    private final DataBroker databroker;
    private final LoadingCache<String, List<Prefix>> prefixSets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<Prefix>>() {
                @Override
                public List<Prefix> load(final String key) throws ExecutionException, InterruptedException {
                    return loadPrefixSets(key);
                }
            });

    private final LoadingCache<String, List<IpPrefix>> prefixes = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<IpPrefix>>() {
                @Override
                public List<IpPrefix> load(final String key) {
                    final List<Prefix> prefixSet = GenericConditionPolicyHandler.this.prefixSets.getUnchecked(key);
                    return prefixSet
                            .stream().map(GenericConditionPolicyHandler::createPrefixes).flatMap(List::stream)
                            .collect(Collectors.toList());
                }
            });

    GenericConditionPolicyHandler(final DataBroker databroker) {
        this.databroker = requireNonNull(databroker);
    }

    private static List<IpPrefix> createPrefixes(final Prefix prefixContainer) {
        final IpPrefix prefix = prefixContainer.getIpPrefix();
        if (prefixContainer.getMasklengthRange().equals(EXACT)) {
            return Collections.singletonList(prefix);
        }

        final String[] range = prefixContainer.getMasklengthRange().split("\\..");

        boolean ipv4 = false;
        final String prefixString;
        if (prefix.getIpv4Prefix() != null) {
            prefixString = prefix.getIpv4Prefix().getValue();
            ipv4 = true;
        } else {
            prefixString = prefix.getIpv6Prefix().getValue();
        }

        int from = Integer.parseInt(range[0]);

        final List<IpPrefix> prefixes = new ArrayList<>();
        while (from <= Integer.parseInt(range[1])) {
            final String prefixVal = StringUtils.replacePattern(prefixString,
                    "[/][0-9]+", "/" + Integer.toString(from));
            if (ipv4) {
                prefixes.add(new IpPrefix(new Ipv4Prefix(prefixVal)));
            } else {
                prefixes.add(new IpPrefix(new Ipv6Prefix(prefixVal)));
            }
            from++;
        }
        return prefixes;
    }

    private List<Prefix> loadPrefixSets(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.databroker.newReadOnlyTransaction();
        final com.google.common.base.Optional<PrefixSet> result =
                tr.read(LogicalDatastoreType.CONFIGURATION, PREFIXES_SET_IID
                        .child(PrefixSet.class, new PrefixSetKey(key))).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getPrefix();
    }

    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final GenericConditions conditions) {
        final MatchPrefixSet prefixSet = conditions.getMatchPrefixSet();
        return prefixSet == null
                || !matchPrefix(routeEntryImportParameters.getRouteId(), conditions.getMatchPrefixSet());
    }

    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final GenericConditions conditions) {
        final MatchPrefixSet prefixSet = conditions.getMatchPrefixSet();
        return prefixSet == null
                || matchPrefix(routeEntryExportParameters.getRouteId(), conditions.getMatchPrefixSet());
    }

    private boolean matchPrefix(
            final NodeIdentifierWithPredicates routeId,
            final MatchPrefixSet matchPrefixSet) {
        final String prefixKey = StringUtils.substringBetween(matchPrefixSet.getPrefixSet(), "=\"", "\"");
        final boolean any = matchPrefixSet.getMatchSetOptions().equals(ANY);
        final boolean found = this.prefixes.getUnchecked(prefixKey).contains(extractPrefix(routeId));
        return any && found || !any && !found;
    }

    @Nonnull
    private IpPrefix extractPrefix(final NodeIdentifierWithPredicates routeId) {
        final QName qName = routeId.getNodeType();
        if (Ipv4Route.QNAME.equals(qName)) {
            final Map<QName, Object> values = routeId.getKeyValues();
            return new IpPrefix(new Ipv4Prefix((String) values.get(IPV4_PREFIX_QNAME)));
        } else {
            final Map<QName, Object> values = routeId.getKeyValues();
            return new IpPrefix(new Ipv6Prefix((String) values.get(IPV6_PREFIX_QNAME)));
        }
    }
}
