/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.extended.community;

public abstract class Abstract4OctetAsExtendedCommunity implements ExtendedCommunityParser,
        ExtendedCommunitySerializer {

    private static final int TRANSITIVE_TYPE = 2;
    private static final int NON_TRANSITIVE_TYPE = 66;

    @Override
    public int getType(final boolean isTransitive) {
        return isTransitive ? TRANSITIVE_TYPE : NON_TRANSITIVE_TYPE;
    }
}
