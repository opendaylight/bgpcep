/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

public final class BgpPeerUtil {
    private BgpPeerUtil() {
        // Hidden on purpose
    }

    /**
     * Creates UPDATE message that contains EOR marker for specified address family.
     *
     * @param key of family for which we need EOR
     * @return UPDATE message with EOR marker
     */
    public static Update createEndOfRib(final TablesKey key) {
        return key.getAfi() == Ipv4AddressFamily.class && key.getSafi() == UnicastSubsequentAddressFamily.class
                ? new UpdateBuilder().build() : new UpdateBuilder()
                        .setAttributes(new AttributesBuilder()
                            .addAugmentation(new Attributes2Builder()
                                .setMpUnreachNlri(new MpUnreachNlriBuilder()
                                    .setAfi(key.getAfi())
                                    .setSafi(key.getSafi())
                                    .build())
                                .build())
                            .build())
                        .build();
    }

    /**
     * Verify presence of EOR marker in UPDATE message.
     *
     * @param msg UPDATE message to be checked
     * @return True if message contains EOR marker, otherwise return False
     */
    public static boolean isEndOfRib(final UpdateMessage msg) {
        if (msg.getNlri() == null && msg.getWithdrawnRoutes() == null) {
            if (msg.getAttributes() != null) {
                final Attributes2 pa = msg.getAttributes().augmentation(Attributes2.class);
                if (pa != null && msg.getAttributes().augmentation(Attributes1.class) == null) {
                    //only MP_UNREACH_NLRI allowed in EOR
                    if (pa.getMpUnreachNlri() != null && pa.getMpUnreachNlri().getWithdrawnRoutes() == null) {
                        // EOR message contains only MPUnreach attribute and no NLRI
                        return true;
                    }
                }
            } else {
                // true for empty IPv4 Unicast
                return true;
            }
        }
        return false;
    }

    /**
     * DTO for transferring LLGR advertizements.
     *
     * @deprecated This class is deprecated for refactoring.
     */
    // FIXME: there should be no need for this class, as we should be able to efficiently translate TableKey classes
    //        and rely on yang-parser-api.
    @Deprecated
    public static final class LlGracefulRestartDTO {

        private final TablesKey tableKey;
        private final int staleTime;
        private final boolean forwardingFlag;

        public LlGracefulRestartDTO(final TablesKey tableKey, final int staleTime, final boolean forwardingFlag) {
            this.tableKey = requireNonNull(tableKey);
            this.staleTime = staleTime;
            this.forwardingFlag = forwardingFlag;
        }

        public TablesKey getTableKey() {
            return tableKey;
        }

        public int getStaleTime() {
            return staleTime;
        }

        public boolean isForwarding() {
            return forwardingFlag;
        }
    }
}
