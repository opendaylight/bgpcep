/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SourceTreeJoinCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SourceTreeJoinCaseBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.6.
 *
 * @author Claudio D. Gasparini
 */
public final class SourceTreeJoinHandler extends AbstractMvpnNlri<SourceTreeJoinCase> {
    @Override
    public int getType() {
        return NlriType.SourceTreeJoin.getIntValue();
    }

    @Override

    public SourceTreeJoinCase parseMvpn(final ByteBuf buffer) {
        return new SourceTreeJoinCaseBuilder().setCMulticast(CMulticastUtil.parseCMulticastGrouping(buffer)).build();
    }

    @Override
    protected ByteBuf serializeBody(final SourceTreeJoinCase mvpn) {
        return CMulticastUtil.serializeCMulticast(mvpn.getCMulticast());
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return SourceTreeJoinCase.class;
    }
}
