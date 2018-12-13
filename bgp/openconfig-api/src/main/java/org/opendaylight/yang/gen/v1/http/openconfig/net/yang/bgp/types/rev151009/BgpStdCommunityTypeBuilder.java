/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009;

/**
 * Customized handler for instantiating {@link BgpStdCommunityType} from a String.
 */
public final class BgpStdCommunityTypeBuilder {
    private BgpStdCommunityTypeBuilder() {
        // Hidden
    }

    public static BgpStdCommunityType getDefaultInstance(final String defaultValue) {
        // uints cannot have a ':', which is allowed for four-octets
        if (defaultValue.indexOf(':') != -1) {
            return new BgpStdCommunityType(defaultValue);
        }

        try {
            return new BgpStdCommunityType(Integer.toUnsignedLong(Integer.parseUnsignedInt(defaultValue)));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Cannot create BgpStdCommunityType from " + defaultValue, e);
        }
    }
}
