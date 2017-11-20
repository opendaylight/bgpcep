/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Listens to the changes in a PeerRegisty.
 *
 */
@Beta
public interface PeerRegistryListener {

    /**
     * Invoked when new peer is added into the registry.
     * @param ip The new peer's IP address.
     * @param prefs The new peer's preferences.
     */
    void onPeerAdded(@Nonnull IpAddress ip, @Nonnull BGPSessionPreferences prefs);

    /**
     * Invoked when peer is removed from registry.
     * @param ip The removed peer's IP address.
     */
    void onPeerRemoved(@Nonnull IpAddress ip);

}
