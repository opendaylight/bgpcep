/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Fragment;

public class FSIpv4FragmentHandler extends AbstractFSFragmentHandler {

    @Override
    protected final Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new Fragment(bs.get(DONT_FRAGMENT), bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    @Override
    protected final byte serializeFragment(final Fragment fragment) {
        final BitArray bs = new BitArray(Byte.SIZE);
        bs.set(DONT_FRAGMENT, fragment.isDoNot());
        bs.set(FIRST_FRAGMENT, fragment.isFirst());
        bs.set(IS_A_FRAGMENT, fragment.isIsA());
        bs.set(LAST_FRAGMENT, fragment.isLast());
        return bs.toByte();
    }
}
