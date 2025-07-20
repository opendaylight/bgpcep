/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;

/**
 * Stores usability for available capabilities. All implementations are required to be effectively immutable.
 */
public abstract class PCEPCapability {
    /**
     * Sets stateful capabilities tlv in incoming builder.
     *
     * @param address peer address to assign capability proposal
     * @param builder for TLVs included in PCEPOpenObject
     */
    public abstract void setCapabilityProposal(InetSocketAddress address, TlvsBuilder builder);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    protected @NonNull ToStringHelper addToStringAttributes(final @NonNull ToStringHelper helper) {
        // No-op by default
        return helper;
    }
}
