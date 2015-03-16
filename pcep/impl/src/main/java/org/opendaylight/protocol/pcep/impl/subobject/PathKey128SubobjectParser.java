/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.util.Ipv6Util;

public class PathKey128SubobjectParser extends AbstractPathKeySubobjectParser implements SubobjectParser, SubobjectSerializer {

    public static final int TYPE = 65;

    public PathKey128SubobjectParser() {
        super(Ipv6Util.IPV6_LENGTH);
    }
}
