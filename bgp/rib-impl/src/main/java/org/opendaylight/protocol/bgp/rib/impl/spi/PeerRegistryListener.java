/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;

/**
 * Listens to the changes in a PeerRegisty.
 */
@NonNullByDefault
public interface PeerRegistryListener {

    /**
     * Invoked when new peer is added into the registry.
     * @param ip The new peer's IP address.
     * @param prefs The new peer's preferences.
     */
    void onPeerAdded(IpAddressNoZone ip, BGPSessionPreferences prefs);

    /**
     * Invoked when peer is removed from registry.
     * @param ip The removed peer's IP address.
     */
    void onPeerRemoved(IpAddressNoZone ip);

}
