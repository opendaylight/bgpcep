package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs.RIBEntryData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based
 * on ordering specified by the supplied comparator.
 *
 */
final class RIBEntry<I, D extends Identifiable<K> & Route, K extends Identifier<D>> {
    private static final Logger LOG = LoggerFactory.getLogger(RIBEntry.class);
    private static final int DEFAULT_MAP_SIZE = 2;

    /*
     * TODO: we could dramatically optimize performance by using the comparator
     *       to retain the candidate states ordered -- thus selection would occur
     *       automatically through insertion, without the need of a second walk.
     */
    private final Map<Peer, RIBEntryData<I, D, K>> candidates = new HashMap<>(DEFAULT_MAP_SIZE);

    /**
     *
     */
    private final AbstractAdjRIBs<I, D, K> parent;
    private final I key;

    @GuardedBy("this")
    KeyedInstanceIdentifier<D, K> name;
    @GuardedBy("this")
    AbstractAdjRIBs.RIBEntryData<I, D, K> currentState;

    RIBEntry(final AbstractAdjRIBs<I, D, K> parent, final I key) {
        this.parent = Preconditions.checkNotNull(parent);
        this.key = Preconditions.checkNotNull(key);
    }

    I getKey() {
        return key;
    }

    private KeyedInstanceIdentifier<D, K> getName() {
        if (this.name == null) {
            this.name = parent.identifierForKey(this.key);
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
        for (final AbstractAdjRIBs.RIBEntryData<I, D, K> s : this.candidates.values()) {
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
            transaction.advertise(parent, this.key, getName(), candidate.getPeer(), candidate.getDataObject(this.key, getName().getKey()));
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

        final AbstractAdjRIBs.RIBEntryData<I, D, K> candidate = findCandidate(transaction.comparator(), null);
        if (candidate != null) {
            electCandidate(transaction, candidate);
        } else {
            LOG.trace("Final candidate disappeared, removing entry {}", getName());
            transaction.withdraw(parent, this.key, getName());
        }

        return this.candidates.isEmpty();
    }

    synchronized void setState(final AdjRIBsTransaction transaction, final Peer peer, final RIBEntryData<I, D, K> state) {
        this.candidates.put(Preconditions.checkNotNull(peer), Preconditions.checkNotNull(state));
        electCandidate(transaction, findCandidate(transaction.comparator(), state));
    }

}