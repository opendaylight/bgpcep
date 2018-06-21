/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;

public abstract class AbstractFSFragmentHandler implements FlowspecTypeParser, FlowspecTypeSerializer {

    public static final int FRAGMENT_VALUE = 12;

    static final int LAST_FRAGMENT = 4;
    static final int FIRST_FRAGMENT = 5;
    static final int IS_A_FRAGMENT = 6;
    static final int DONT_FRAGMENT = 7;

    protected abstract Fragment parseFragment(final byte fragment);
    protected abstract byte serializeFragment(final Fragment fragment);

    @Override
    public void serializeType(final FlowspecType fsType, final ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof FragmentCase, "FragmentCase class is mandatory!");
        output.writeByte(FRAGMENT_VALUE);
        serializeFragments(((FragmentCase) fsType).getFragments(), output);
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        if (buffer == null) {
            return null;
        }
        return new FragmentCaseBuilder().setFragments(parseFragments(buffer)).build();
    }

    private void serializeFragments(final List<Fragments> fragments, final ByteBuf nlriByteBuf) {
        for (final Iterator<Fragments> it = fragments.iterator(); it.hasNext(); ) {
            final Fragments fragment = it.next();
            BitmaskOperandParser.INSTANCE.serialize(fragment.getOp(), 1, !it.hasNext(), nlriByteBuf);
            nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
        }
    }

    private List<Fragments> parseFragments(final ByteBuf nlri) {
        final List<Fragments> fragments = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FragmentsBuilder builder = new FragmentsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = BitmaskOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(parseFragment(nlri.readByte()));
            end = op.isEndOfList();
            fragments.add(builder.build());
        }
        return fragments;
    }
}
