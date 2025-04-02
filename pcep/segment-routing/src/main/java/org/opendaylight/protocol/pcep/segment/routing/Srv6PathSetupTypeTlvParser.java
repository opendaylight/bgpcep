/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeTlvParser;

public class Srv6PathSetupTypeTlvParser extends PathSetupTypeTlvParser {

    private static final short SRV6_PST = 3;

    public Srv6PathSetupTypeTlvParser() {
        super(SRV6_PST);
    }

}
