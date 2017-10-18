/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BGP speaker (without Graceful restart capability) sends KeepAlive message after sending all initial Update messages
 * with certain AFI/SAFI. For each AFI/SAFI, it sends one KA message. As it is undetermined which KA message belongs to
 * which AFI/SAFI, an algorithm needed to be implemented.
 */
public class BGPSynchronization {

    private static final Logger LOG = LoggerFactory.getLogger(BGPSynchronization.class);

    @VisibleForTesting
    static class SyncVariables {

        private boolean upd = false;
        private boolean eor = false;

        public void setUpd(final boolean upd) {
            this.upd = upd;
        }

        public void setEorTrue() {
            this.eor = true;
        }

        public boolean getEor() {
            return this.eor;
        }

        public boolean getUpd() {
            return this.upd;
        }
    }

    @VisibleForTesting
    public final Map<TablesKey, SyncVariables> syncStorage = Maps.newHashMap();

    private final BGPSessionListener listener;

    public BGPSynchronization(final BGPSessionListener listener, final Set<TablesKey> types) {
        this.listener = requireNonNull(listener);

        for (final TablesKey type : types) {
            this.syncStorage.put(type, new SyncVariables());
        }
    }

    /**
     * For each received Update message, the upd sync variable needs to be updated to true, for particular AFI/SAFI
     * combination. Currently we only assume Unicast SAFI. From the Update message we have to extract the AFI. Each
     * Update message can contain BGP Object with one type of AFI. If the object is BGP Link, BGP Node or a BGPPrefix
     * the AFI is Linkstate. In case of BGPRoute, the AFI depends on the IP Address of the prefix.
     *
     * @param msg received Update message
     */
    public void updReceived(final Update msg) {
        TablesKey type = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        boolean isEOR = false;
        if (msg.getNlri() == null && msg.getWithdrawnRoutes() == null) {
            if (msg.getAttributes() != null) {
                if (msg.getAttributes().getAugmentation(Attributes1.class) != null) {
                    final Attributes1 pa = msg.getAttributes().getAugmentation(Attributes1.class);
                    if (pa.getMpReachNlri() != null) {
                        type = new TablesKey(pa.getMpReachNlri().getAfi(), pa.getMpReachNlri().getSafi());
                    }
                } else if (msg.getAttributes().getAugmentation(Attributes2.class) != null) {
                    final Attributes2 pa = msg.getAttributes().getAugmentation(Attributes2.class);
                    if (pa.getMpUnreachNlri() != null) {
                        type = new TablesKey(pa.getMpUnreachNlri().getAfi(), pa.getMpUnreachNlri().getSafi());
                    }
                    if (pa.getMpUnreachNlri().getWithdrawnRoutes() == null) {
                        // EOR message contains only MPUnreach attribute and no NLRI
                        isEOR = true;
                    }
                }
            } else {
                // true for empty Update Message
                isEOR = true;
            }
        }
        syncType(type, isEOR);
    }

    private void syncType(final TablesKey type, final boolean isEOR) {
        final SyncVariables s = this.syncStorage.get(type);
        if (s == null) {
            LOG.warn("BGPTableType was not present in open message : {}", type);
            return;
        }
        s.setUpd(true);
        if (isEOR) {
            s.setEorTrue();
            this.listener.markUptodate(type);
            LOG.info("BGP Synchronization finished for table {} ", type);
        }
    }

    /**
     * This method is called, when the second KA message is received. It checks each AFI/SAFI sync variables. If they
     * are all false, which means, that there was at least one update message followed by one KA, the EOR is sent to
     * session.
     */
    public void kaReceived() {
        for (final Entry<TablesKey, SyncVariables> entry : this.syncStorage.entrySet()) {
            final SyncVariables s = entry.getValue();
            if (!s.getEor()) {
                if (!s.getUpd()) {
                    s.setEorTrue();
                    LOG.info("BGP Synchronization finished for table {} ", entry.getKey());
                    this.listener.markUptodate(entry.getKey());
                }
                s.setUpd(false);
            }
        }
    }
}
