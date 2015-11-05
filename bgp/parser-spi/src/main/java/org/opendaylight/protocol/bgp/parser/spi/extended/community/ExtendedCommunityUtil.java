/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.extended.community;

/**
 * The utility functions related to the extended communities.
 *
 */
public final class ExtendedCommunityUtil {

    private ExtendedCommunityUtil() {
        throw new UnsupportedOperationException();
    }

    private static final byte NON_TRANS = 0x40;

    /**
     * Sets transitivity flag for the Extended Community type.
     * @param type Extended Community Type
     * @param isTransitive Extended Community transitivity
     * @return Extended Community type with a transitivity flag set if isTransitive false, otherwise returns unchanged type.
     */
    public static int setTransitivity(final int type, final boolean isTransitive) {
        return isTransitive ? type : ExtendedCommunityUtil.toNonTransitiveType(type);
    }

    /**
     * Check the Extended Community type for transitivity.
     * @param type Extended Community Type
     * @return True if input type is transitive, false if the type is non-transitive
     */
    public static boolean isTransitive(final int type) {
        return (type & NON_TRANS) == 0 ? true : false;
    }

    private static int toNonTransitiveType(final int type) {
        return type | NON_TRANS;
    }

}
