/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
 * Listens to the session changes for peers in a PeerRegisty.
 *
 */
@Beta
public interface PeerRegistrySessionListener {

    /**
     * Invoked when new peer session is created.
     * @param ip The peer's IP address.
     */
    void onSessionCreated(@Nonnull IpAddress ip);

    /**
     * Invoked when peer session is removed.
     * @param ip The peer's IP address.
     */
    void onSessionRemoved(@Nonnull IpAddress ip);

}
