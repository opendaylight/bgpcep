/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public interface BGPPeerRegistry extends AutoCloseable {

    void addPeer(IpAddress ip, ReusableBGPPeer peer, BGPSessionPreferences prefs);

    void removePeer(IpAddress ip);

    boolean isPeerConfigured(IpAddress ip);

    BGPSessionListener getPeer(IpAddress ip, Ipv4Address bgpIdentifier, Ipv4Address bgpId) throws BGPDocumentedException;

    BGPSessionPreferences getPeerPreferences(IpAddress ip);

}
