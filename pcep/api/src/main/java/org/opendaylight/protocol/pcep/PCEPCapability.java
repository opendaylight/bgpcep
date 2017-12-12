/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

/**
 * Stores usability for available capabilities.
 */
public interface PCEPCapability {

    /**
     * Sets stateful capabilities tlv in incoming builder.
     *
     * @param address peer address to assign capability proposal
     * @param builder for TLVs included in PCEPOpenObject
     */
    void setCapabilityProposal(InetSocketAddress address, TlvsBuilder builder);

    /**
     * Returs stateful capability state.
     *
     * @return true if capability is present
     */
    boolean isStateful();
}
