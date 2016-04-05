/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

abstract class AbstractParserTest {
    protected static final int VALUE_SIZE = 9;
    protected static final long LD = 33686018;
    protected static final MacAddress MAC = new MacAddress("f2:0c:dd:80:9f:f7");
}
