/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005;

import com.google.common.net.InetAddresses;

/**
 **/
public final class PceIdBuilder {
    private PceIdBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Construct a new PCE ID, from either an IPv4 or IPv6 form.
     *
     * @param defaultValue Which is a PCE-ID in string form
     * @return A PCE ID.
     */
    public static PceId getDefaultInstance(final String defaultValue) {
        return new PceId(InetAddresses.forString(defaultValue).getAddress());
    }
}
