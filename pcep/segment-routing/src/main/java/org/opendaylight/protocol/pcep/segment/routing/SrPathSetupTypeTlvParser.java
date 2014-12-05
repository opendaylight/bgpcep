/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import org.opendaylight.protocol.pcep.impl.tlv.PathSetupTypeTlvParser;

public class SrPathSetupTypeTlvParser extends PathSetupTypeTlvParser {

    private static final short SR_TE_PST = 1;

    public SrPathSetupTypeTlvParser() {
        super();
        PSTS.add(SR_TE_PST);
    }

}
