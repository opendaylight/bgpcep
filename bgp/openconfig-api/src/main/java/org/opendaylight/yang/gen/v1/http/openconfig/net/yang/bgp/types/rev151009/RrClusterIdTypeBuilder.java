/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

/**
 * Customized handler for instantiating {@link RrClusterIdType} from a String.
 */
public final class RrClusterIdTypeBuilder {
    private RrClusterIdTypeBuilder() {
        // Hidden
    }

    public static RrClusterIdType getDefaultInstance(final String defaultValue) {
        // IPv4 has to have a dot in it
        if (defaultValue.indexOf('.') != -1) {
            return new RrClusterIdType(new Ipv4Address(defaultValue));
        }

        try {
            return new RrClusterIdType(Integer.toUnsignedLong(Integer.parseUnsignedInt(defaultValue)));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Cannot create RrClusterIdType from " + defaultValue, e);
        }
    }
}
