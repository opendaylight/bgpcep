/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

/**
 * Proposal from peer for establishment of PCEP session
 */
public interface PCEPPeerProposal {

    /**
     * Sets specific TLVs into incoming builder
     *
     * @param address of the pcep speaker to be assigned given tlvs
     * @param tlvs    builder, to attach tl's with more information to be sent under Open message
     */
    void setPeerSpecificProposal(@Nonnull InetSocketAddress address, @Nonnull TlvsBuilder tlvs);
}
