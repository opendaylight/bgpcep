/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public abstract class AbstractAdjRIBs<I, D extends Identifiable<K> & Route, K extends Identifier<D>> implements AdjRIBsIn<I, D>, RouteEncoder {
    protected abstract static class RIBEntryData<I, D extends Identifiable<K> & Route, K extends Identifier<D>> {
        private final PathAttributes attributes;
        private final Peer peer;

        protected RIBEntryData(final Peer peer, final PathAttributes attributes) {
            this.attributes = Preconditions.checkNotNull(attributes);
            this.peer = Preconditions.checkNotNull(peer);
        }

        public PathAttributes getPathAttributes() {
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
            return addToStringAttributes(Objects.toStringHelper(this)).toString();
        }

        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("attributes", this.attributes);
        }
    }

    /**
     * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based
     * on ordering specified by the supplied comparator.
     *
     */
    private final class RIBEntry {
        /*
         * TODO: we could dramatically optimize performance by using the comparator
         *       to retain the candidate states ordered -- thus selection would occur
         *       automatically through insertion, without the need of a second walk.
         */
        private final Map<Peer, RIBEntryData<I, D, K>> candidates = new HashMap<>();
        private final I key;

        @GuardedBy("this")
        private KeyedInstanceIdentifier<D, K> name;
        @GuardedBy("this")
        private RIBEntryData<I, D, K> currentState;

        RIBEntry(final I key) {
            this.key = Preconditions.checkNotNull(key);
        }

        private KeyedInstanceIdentifier<D, K> getName() {
            if (this.name == null) {
                this.name = identifierForKey(AbstractAdjRIBs.this.basePath, this.key);
                LOG.trace("Entry {} grew key {}", this, this.name);
            }
            return this.name;
        }

        /**
         * Based on given comparator, finds a new best candidate for initial route.
         *
         * @param comparator
         * @param initial
         * @return
         */
        private RIBEntryData<I, D, K> findCandidate(final BGPObjectComparator comparator, final RIBEntryData<I, D, K> initial) {
            RIBEntryData<I, D, K> newState = initial;
            for (final RIBEntryData<I, D, K> s : this.candidates.values()) {
                if (newState == null || comparator.compare(newState, s) > 0) {
                    newState = s;
                }
            }

            return newState;
        }

        /**
         * Advertize newly elected best candidate to datastore.
         *
         * @param transaction
         * @param candidate
         */
        private void electCandidate(final AdjRIBsTransaction transaction, final RIBEntryData<I, D, K> candidate) {
            LOG.trace("Electing state {} to supersede {}", candidate, this.currentState);

            if (this.currentState == null || !this.currentState.equals(candidate)) {
                LOG.trace("Elected new state for {}: {}", getName(), candidate);
                transaction.advertise(AbstractAdjRIBs.this, this.key, getName(), candidate.getPeer(), candidate.getDataObject(this.key, getName().getKey()));
                this.currentState = candidate;
            }
        }

        /**
         * Removes RIBEntry from database. If we are removing best path, elect another candidate (using BPS).
         * If there are no other candidates, remove the path completely.
         * @param transaction
         * @param peer
         * @return true if the list of the candidates for this path is empty
         */
        synchronized boolean removeState(final AdjRIBsTransaction transaction, final Peer peer) {
            final RIBEntryData<I, D, K> data = this.candidates.remove(peer);
            LOG.trace("Removed data {}", data);

            final RIBEntryData<I, D, K> candidate = findCandidate(transaction.comparator(), null);
            if (candidate != null) {
                electCandidate(transaction, candidate);
            } else {
                LOG.trace("Final candidate disappeared, removing entry {}", getName());
                transaction.withdraw(AbstractAdjRIBs.this, this.key, getName());
            }

            return this.candidates.isEmpty();
        }

        synchronized void setState(final AdjRIBsTransaction transaction, final Peer peer, final RIBEntryData<I, D, K> state) {
            this.candidates.put(Preconditions.checkNotNull(peer), Preconditions.checkNotNull(state));
            electCandidate(transaction, findCandidate(transaction.comparator(), state));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAdjRIBs.class);
    private final KeyedInstanceIdentifier<Tables, TablesKey> basePath;
    private final BgpTableType tableType;
    private final Update eor;

    @GuardedBy("this")
    private final Map<I, RIBEntry> entries = new HashMap<>();

    @GuardedBy("this")
    private final Map<Peer, Boolean> peers = new HashMap<>();

    protected AbstractAdjRIBs(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        this.basePath = Preconditions.checkNotNull(basePath);
        this.tableType = new BgpTableTypeImpl(basePath.getKey().getAfi(), basePath.getKey().getSafi());
        this.eor = new UpdateBuilder().setPathAttributes(new PathAttributesBuilder().addAugmentation(
            PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(new MpReachNlriBuilder(this.tableType)
                .build()).build()).build()).build();
    }

    @Override
    public final synchronized void clear(final AdjRIBsTransaction trans, final Peer peer) {
        final Iterator<Entry<I, RIBEntry>> i = this.entries.entrySet().iterator();
        while (i.hasNext()) {
            final Entry<I, RIBEntry> e = i.next();

            if (e.getValue().removeState(trans, peer)) {
                i.remove();
            }
        }

        this.peers.remove(peer);
        trans.setUptodate(this.basePath, !this.peers.values().contains(Boolean.FALSE));
    }

    public final synchronized void addAllEntries(final AdjRIBsTransaction trans) {
        for (final Entry<I, RIBEntry> e : this.entries.entrySet()) {
            final RIBEntry entry = e.getValue();
            final RIBEntryData<I, D, K> state = entry.currentState;
            trans.advertise(this, e.getKey(), entry.name, state.peer, state.getDataObject(entry.key, entry.name.getKey()));
        }
    }

    /**
     * Construct a datastore identifier for an entry key.
     *
     * @param basePath datastore base path under which the entry to be stored
     * @param id object identifier
     * @return Data store identifier, may not be null
     */
    protected abstract KeyedInstanceIdentifier<D, K> identifierForKey(InstanceIdentifier<Tables> basePath, I id);

    public void addWith(final MpUnreachNlriBuilder builder, final InstanceIdentifier<?> key) {
        this.addWithdrawal(builder, keyForIdentifier(this.routeIdentifier(key)));
    }

    /**
     * Transform a withdrawn identifier into a the corresponding NLRI in MP_UNREACH attribute.
     *
     * @param id Route key
     */
    protected abstract void addWithdrawal(MpUnreachNlriBuilder builder, I id);

    public abstract @Nullable KeyedInstanceIdentifier<D, K> routeIdentifier(InstanceIdentifier<?> id);

    public abstract I keyForIdentifier(KeyedInstanceIdentifier<D, K> id);

    /**
     * Common backend for {@link AdjRIBsIn#addRoutes()} implementations.
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

        RIBEntry e = this.entries.get(Preconditions.checkNotNull(id));
        if (e == null) {
            e = new RIBEntry(id);
            this.entries.put(id, e);
        }

        e.setState(trans, peer, data);
        if (!this.peers.containsKey(peer)) {
            this.peers.put(peer, Boolean.FALSE);
            trans.setUptodate(this.basePath, Boolean.FALSE);
        }
    }

    /**
     * Common backend for {@link AdjRIBsIn#removeRoutes()} implementations.
     *
     * @param trans Transaction context
     * @param peer Originating peer
     * @param id Data store instance identifier
     */
    protected final synchronized void remove(final AdjRIBsTransaction trans, final Peer peer, final I id) {
        final RIBEntry e = this.entries.get(id);
        if (e != null && e.removeState(trans, peer)) {
            LOG.debug("Removed last state, removing entry for {}", id);
            this.entries.remove(id);
        }
    }

    @Override
    public final void markUptodate(final AdjRIBsTransaction trans, final Peer peer) {
        this.peers.put(peer, Boolean.TRUE);
        trans.setUptodate(this.basePath, !this.peers.values().contains(Boolean.FALSE));
    }

    @Override
    public final Update endOfRib() {
        return this.eor;
    }

    @Override
    public Update updateMessageFor(final Object key, final Route route) {
        final UpdateBuilder ub = new UpdateBuilder();
        final PathAttributesBuilder pab = new PathAttributesBuilder();

        if (route != null) {
            final MpReachNlriBuilder reach = new MpReachNlriBuilder(this.tableType);

            addAdvertisement(reach, (D)route);
            pab.fieldsFrom(route.getAttributes());
            pab.addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(reach.build()).build()).build();
        } else {
            final MpUnreachNlriBuilder unreach = new MpUnreachNlriBuilder(this.tableType);
            addWithdrawal(unreach, (I)key);
            pab.addAugmentation(PathAttributes2.class, new PathAttributes2Builder().setMpUnreachNlri(unreach.build()).build()).build();
        }

        ub.setPathAttributes(pab.build());
        return ub.build();
    }

}
