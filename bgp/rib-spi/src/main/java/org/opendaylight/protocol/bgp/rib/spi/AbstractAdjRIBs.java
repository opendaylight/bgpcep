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

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
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
public abstract class AbstractAdjRIBs<I, D extends Identifiable<K> & Route, K extends Identifier<D>> implements AdjRIBsIn {
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

        private RIBEntryData<I, D, K> findCandidate(final BGPObjectComparator comparator, final RIBEntryData<I, D, K> initial) {
            RIBEntryData<I, D, K> newState = initial;
            for (final RIBEntryData<I, D, K> s : this.candidates.values()) {
                if (newState == null || comparator.compare(newState, s) > 0) {
                    newState = s;
                }
            }

            return newState;
        }

        private void electCandidate(final AdjRIBsInTransaction transaction, final RIBEntryData<I, D, K> candidate) {
            LOG.trace("Electing state {} to supersede {}", candidate, this.currentState);

            if (this.currentState == null || !this.currentState.equals(candidate)) {
                LOG.trace("Elected new state for {}: {}", getName(), candidate);
                transaction.advertise(getName(), candidate.getDataObject(this.key, getName().getKey()));
                this.currentState = candidate;
            }
        }

        synchronized boolean removeState(final AdjRIBsInTransaction transaction, final Peer peer) {
            final RIBEntryData<I, D, K> data = this.candidates.remove(peer);
            LOG.trace("Removed data {}", data);

            final RIBEntryData<I, D, K> candidate = findCandidate(transaction.comparator(), null);
            if (candidate != null) {
                electCandidate(transaction, candidate);
            } else {
                LOG.trace("Final candidate disappeared, removing entry {}", getName());
                transaction.withdraw(getName());
            }

            return this.candidates.isEmpty();
        }

        synchronized void setState(final AdjRIBsInTransaction transaction, final Peer peer, final RIBEntryData<I, D, K> state) {
            this.candidates.put(Preconditions.checkNotNull(peer), Preconditions.checkNotNull(state));
            electCandidate(transaction, findCandidate(transaction.comparator(), state));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAdjRIBs.class);
    private final InstanceIdentifier<Tables> basePath;
    private final Update eor;

    @GuardedBy("this")
    private final Map<I, RIBEntry> entries = new HashMap<>();

    @GuardedBy("this")
    private final Map<Peer, Boolean> peers = new HashMap<>();

    protected AbstractAdjRIBs(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        this.basePath = Preconditions.checkNotNull(basePath);
        this.eor = new UpdateBuilder().setPathAttributes(new PathAttributesBuilder().addAugmentation(
                        PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(new MpReachNlriBuilder()
                            .setAfi(basePath.getKey().getAfi()).setSafi(basePath.getKey().getSafi()).build()).build()).build()).build();
    }

    @Override
    public final synchronized void clear(final AdjRIBsInTransaction trans, final Peer peer) {
        final Iterator<Map.Entry<I, RIBEntry>> i = this.entries.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<I, RIBEntry> e = i.next();

            if (e.getValue().removeState(trans, peer)) {
                i.remove();
            }
        }

        this.peers.remove(peer);
        trans.setUptodate(basePath, !this.peers.values().contains(Boolean.FALSE));
    }

    /**
     * Construct a datastore identifier for an entry key.
     *
     * @param basePath datastore base path under which the entry to be stored
     * @param id object identifier
     * @return Data store identifier, may not be null
     */
    protected abstract KeyedInstanceIdentifier<D, K> identifierForKey(InstanceIdentifier<Tables> basePath, I id);

    /**
     * Transform an advertised data object into the corresponding NLRI in MP_REACH attribute.
     *
     * @param data Data object
     * @param builder MP_REACH attribute builder
     */
    protected abstract void addAdvertisement(MpReachNlriBuilder builder, D data);

    /**
     * Transform a withdrawn identifier into a the corresponding NLRI in MP_UNREACH attribute.
     *
     * @param id Route key
     */
    protected abstract void addWithdrawal(MpUnreachNlriBuilder builder, I id);

    /**
     * Common backend for {@link AdjRIBsIn#addRoutes()} implementations.
     *
     * @param trans Transaction context
     * @param peer Originating peer
     * @param id Data store instance identifier
     * @param data Data object to be written
     */
    protected final synchronized void add(final AdjRIBsInTransaction trans, final Peer peer, final I id, final RIBEntryData<I, D, K> data) {
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
    protected final synchronized void remove(final AdjRIBsInTransaction trans, final Peer peer, final I id) {
        final RIBEntry e = this.entries.get(id);
        if (e != null && e.removeState(trans, peer)) {
            LOG.debug("Removed last state, removing entry for {}", id);
            this.entries.remove(id);
        }
    }

    @Override
    public final void markUptodate(final AdjRIBsInTransaction trans, final Peer peer) {
        this.peers.put(peer, Boolean.TRUE);
        trans.setUptodate(this.basePath, !this.peers.values().contains(Boolean.FALSE));
    }

    @Override
    public final Update endOfRib() {
        return this.eor;
    }
}
