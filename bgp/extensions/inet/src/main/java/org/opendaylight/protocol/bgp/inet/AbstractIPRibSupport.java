/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common {@link org.opendaylight.protocol.bgp.rib.spi.RIBSupport} class for IPv4 and IPv6 addresses.
 */
abstract class AbstractIPRibSupport<
        C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<S> & Identifiable<?>>
        extends AbstractRIBSupport<C, S, R> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIPRibSupport.class);
    private final NodeIdentifier prefixNid;
    private final NodeIdentifier nlriRoutesList;
    private final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects;

    AbstractIPRibSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<? extends DataObject> prefixClass,
            final AddressFamily addressFamilyClass,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<R> listClass,
            final QName destinationQname, final QName prefixesQname) {
        super(mappingService, cazeClass, containerClass, listClass, addressFamilyClass,
                UnicastSubsequentAddressFamily.VALUE, destinationQname);
        this.nlriRoutesList = new NodeIdentifier(prefixesQname);
        this.cacheableNlriObjects = ImmutableSet.of(prefixClass);
        this.prefixNid = new NodeIdentifier(QName.create(routeQName(), "prefix").intern());
    }

    final NodeIdentifier routePrefixIdentifier() {
        return this.prefixNid;
    }

    @Override
    public final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataTreeWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute function) {
        if (destination != null) {
            final DataContainerChild routes = destination.childByArg(this.nlriRoutesList);
            if (routes instanceof UnkeyedListNode) {
                // Instance identifier to table/(choice routes)/(map of route)
                final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).body();
                final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                for (final UnkeyedListEntryNode ipDest : routesList) {
                    final NodeIdentifierWithPredicates routeKey = createRouteKey(ipDest);
                    function.apply(tx, base, routeKey, ipDest, attributes);
                    keys.add(routeKey);
                }
                return keys;
            }
            LOG.warn("Routes {} are not a map", routes);
        }
        return List.of();
    }

    /**
     * Prefix and Path Id are the route key.
     *
     * @param prefixes UnkeyedListEntryNode containing route
     * @return Nid with Route Key
     */
    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode prefixes) {
        final DataContainerChild prefixLeaf = prefixes.childByArg(routePrefixIdentifier());
        checkState(prefixLeaf != null);
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(), prefixLeaf.body(),
            prefixes.findChildByArg(routePathIdNid()));
    }
}
