/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
@ThreadSafe
public abstract class AbstractAdjRIBs<I, D extends Identifiable<K> & Route, K extends Identifier<D>> implements AdjRIBsIn<I, D>, RouteEncoder {
    protected abstract static class RIBEntryData<I, D extends Identifiable<K> & Route, K extends Identifier<D>> {
        private final Attributes attributes;
        private final Peer peer;

        protected RIBEntryData(final Peer peer, final Attributes attributes) {
            this.attributes = Preconditions.checkNotNull(attributes);
            this.peer = Preconditions.checkNotNull(peer);
        }

        public Attributes getAttributes() {
            return this.attributes;
        }

        public Peer getPeer() {
            return this.peer;
        }

        /**
         * Create a data object given the key and target instance identifier.
         *
         * @param key Route key
         * @param id Data store target identifier
         * @return Data object to be written to the data store.
         */
        protected abstract D getDataObject(I key, K id);

        @Override
        public final String toString() {
            return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
        }

        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("attributes", this.attributes);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAdjRIBs.class);
    private final KeyedInstanceIdentifier<Tables, TablesKey> basePath;
    private final BgpTableType tableType;
    private final Update eor;

    @GuardedBy("this")
    private final Map<I, RIBEntry<I, D, K>> entries = new HashMap<>();

    @GuardedBy("this")
    private final Map<Peer, Boolean> peers = new HashMap<>();

    protected AbstractAdjRIBs(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        this.basePath = Preconditions.checkNotNull(basePath);
        this.tableType = new BgpTableTypeImpl(basePath.getKey().getAfi(), basePath.getKey().getSafi());
        this.eor = new UpdateBuilder().setAttributes(new AttributesBuilder().addAugmentation(
            Attributes1.class, new Attributes1Builder().setMpReachNlri(new MpReachNlriBuilder(this.tableType)
                .build()).build()).build()).build();
    }

    @Override
    public final synchronized void clear(final AdjRIBsTransaction trans, final Peer peer) {
        final Iterator<Entry<I, RIBEntry<I, D, K>>> i = this.entries.entrySet().iterator();
        while (i.hasNext()) {
            final Entry<I, RIBEntry<I, D, K>> e = i.next();

            if (e.getValue().removeState(trans, peer)) {
                i.remove();
            }
        }

        this.peers.remove(peer);
        trans.setUptodate(getBasePath(), !this.peers.values().contains(Boolean.FALSE));
    }

    public final synchronized void addAllEntries(final AdjRIBsTransaction trans) {
        for (final Entry<I, RIBEntry<I, D, K>> e : this.entries.entrySet()) {
            final RIBEntry<I, D, K> entry = e.getValue();
            final RIBEntryData<I, D, K> state = entry.currentState;
            trans.advertise(this, e.getKey(), entry.name, state.peer, state.getDataObject(entry.getKey(), entry.name.getKey()));
        }
    }

    /**
     * Construct a datastore identifier for an entry key.
     *
     * @param basePath datastore base path under which the entry to be stored
     * @param id object identifier
     * @return Data store identifier, may not be null
     *
     * @deprecated Please override {@link #identifierForKey(Object)} instead. The basePath
     *             argument is constant for a particular instance and is the one your
     *             constructor specifies.
     */
    @Deprecated
    protected abstract KeyedInstanceIdentifier<D, K> identifierForKey(InstanceIdentifier<Tables> basePath, I id);

    /**
     * Return the base path specified at construction time.
     *
     * @return Base path.
     */
    protected final KeyedInstanceIdentifier<Tables, TablesKey> getBasePath() {
        return this.basePath;
    }

    /**
     * Construct a datastore identifier for an entry key.
     *
     * @param id object identifier
     * @return Data store identifier, may not be null
     */
    protected KeyedInstanceIdentifier<D, K> identifierForKey(final I id) {
        return identifierForKey(getBasePath(), id);
    }

    public void addWith(final MpUnreachNlriBuilder builder, final InstanceIdentifier<?> key) {
        this.addWithdrawal(builder, keyForIdentifier(this.routeIdentifier(key)));
    }

    /**
     * Transform a withdrawn identifier into a the corresponding NLRI in MP_UNREACH attribute.
     * @param builder MpUnreachNlriBuilder
     * @param id Route key
     */
    protected abstract void addWithdrawal(MpUnreachNlriBuilder builder, I id);

    /**
     * Creates router identifier out of instance identifier
     * @param id instance identifier
     * @return router identifier
     */
    @Nullable
    public abstract KeyedInstanceIdentifier<D, K> routeIdentifier(InstanceIdentifier<?> id);

    /**
     * Craates route key out of instance identifier
     * @param id instance identifier
     * @return route key
     */
    public abstract I keyForIdentifier(KeyedInstanceIdentifier<D, K> id);

    /**
     * Common backend for {@link AdjRIBsIn#addRoutes(AdjRIBsTransaction, Peer, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes)} implementations.
     *
     * If a new route is added, check first for its existence in Map of entries.
     * If the route is already there, change it's state. Then check for peer in
     * Map of peers, if it's not there, add it.
     *
     * @param trans Transaction context
     * @param peer Originating peer
     * @param id Data store instance identifier
     * @param data Data object to be written
     */
    protected final synchronized void add(final AdjRIBsTransaction trans, final Peer peer, final I id, final RIBEntryData<I, D, K> data) {
        LOG.debug("Adding state {} for {} peer {}", data, id, peer);

        RIBEntry<I, D, K> e = this.entries.get(Preconditions.checkNotNull(id));
        if (e == null) {
            e = new RIBEntry<I, D, K>(this, id);
            this.entries.put(id, e);
        }

        e.setState(trans, peer, data);
        if (!this.peers.containsKey(peer)) {
            this.peers.put(peer, Boolean.FALSE);
            trans.setUptodate(getBasePath(), Boolean.FALSE);
        }
    }

    /**
     * Common backend for {@link AdjRIBsIn#removeRoutes(AdjRIBsTransaction, Peer, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri)} implementations.
     *
     * @param trans Transaction context
     * @param peer Originating peer
     * @param id Data store instance identifier
     */
    protected final synchronized void remove(final AdjRIBsTransaction trans, final Peer peer, final I id) {
        final RIBEntry<I, D, K> e = this.entries.get(id);
        if (e != null && e.removeState(trans, peer)) {
            LOG.debug("Removed last state, removing entry for {}", id);
            this.entries.remove(id);
        }
    }

    @Override
    public final void markUptodate(final AdjRIBsTransaction trans, final Peer peer) {
        this.peers.put(peer, Boolean.TRUE);
        trans.setUptodate(getBasePath(), !this.peers.values().contains(Boolean.FALSE));
    }

    @Override
    public final Update endOfRib() {
        return this.eor;
    }

    @Override
    public Update updateMessageFor(final Object key, final Route route) {
        final UpdateBuilder ub = new UpdateBuilder();
        final AttributesBuilder pab = new AttributesBuilder();

        if (route != null) {
            final MpReachNlriBuilder reach = new MpReachNlriBuilder(this.tableType);

            addAdvertisement(reach, (D)route);
            pab.fieldsFrom(route.getAttributes());
            pab.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(reach.build()).build()).build();
        } else {
            final MpUnreachNlriBuilder unreach = new MpUnreachNlriBuilder(this.tableType);
            addWithdrawal(unreach, (I)key);
            pab.addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(unreach.build()).build()).build();
        }

        ub.setAttributes(pab.build());
        return ub.build();
    }

}
