/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

public final class AttributeFlags {

    private AttributeFlags(){

    }

    public static final int OPTIONAL = 128;
    public static final int TRANSITIVE = 64;
    public static final int PARTIAL = 32;
    public static final int EXTENDED = 16;
    public static final int UNUSED_8 = 8;
    public static final int UNUSED_4 = 4;
    public static final int UNUSED_2 = 2;
    public static final int UNUSED_1 = 1;
}
