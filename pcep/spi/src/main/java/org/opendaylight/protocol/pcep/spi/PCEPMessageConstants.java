/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

public final class PCEPMessageConstants {
    /**
     * Length of the common message header, in bytes.
     */
    public static final int COMMON_HEADER_LENGTH = 4;

    /**
     * Current supported version of PCEP.
     */
    public static final int PCEP_VERSION = 1;

    private PCEPMessageConstants() {
    }
}
