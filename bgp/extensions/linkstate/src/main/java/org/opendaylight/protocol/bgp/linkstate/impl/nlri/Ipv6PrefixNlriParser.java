/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NlriType;

public final class Ipv6PrefixNlriParser extends AbstractPrefixNlriParser {
    @Override
    public int getNlriType() {
        return NlriType.Ipv6Prefix.getIntValue();
    }
}