/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public interface GlobalBGPSessionRegistry {

    RegistrationResult addSession(BGPSession session, final Ipv4Address fromId, final Ipv4Address toId);

    void removeSession(final Ipv4Address fromId, final Ipv4Address toId);

    public enum RegistrationResult {
        SUCCESS, DUPLICATE, DROPPED, DROPPED_PREVIOUS
    }

}
