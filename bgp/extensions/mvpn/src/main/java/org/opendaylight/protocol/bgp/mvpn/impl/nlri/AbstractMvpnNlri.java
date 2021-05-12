/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnParser;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.MulticastSourceRdGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.s.pmsi.a.d.grouping.SPmsiADBuilder;

/**
 * Abstract Mvpn Nlri.
 *
 * @author Claudio D. Gasparini
 */
public abstract class AbstractMvpnNlri<T extends MvpnChoice> implements MvpnSerializer<T>, MvpnParser<T> {
    private final @NonNull Class<T> choice;
    private final @NonNull NlriType type;
    private final int intType;

    protected AbstractMvpnNlri(final Class<T> choice, final NlriType type) {
        this.choice = requireNonNull(choice);
        this.type = requireNonNull(type);
        intType = type.getIntValue();
    }

    @Override
    public final Class<T> getClazz() {
        return choice;
    }

    @Override
    public final NlriType getType() {
        return type;
    }

    @Override
    public final ByteBuf serializeMvpn(final T mvpn) {
        final ByteBuf output = Unpooled.buffer();
        final ByteBuf body = serializeBody(mvpn);
        output.writeByte(intType);
        output.writeByte(body.readableBytes());
        output.writeBytes(body);
        return output;
    }

    protected abstract ByteBuf serializeBody(T mvpn);

    static final MulticastSourceRdGrouping parseRDMulticastSource(final ByteBuf buffer) {
        return new SPmsiADBuilder()
            .setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(buffer))
            .setMulticastSource(IpAddressUtil.addressForByteBuf(buffer))
            .build();
    }

    static final void serializeRDMulticastSource(final MulticastSourceRdGrouping route, final ByteBuf output) {
        RouteDistinguisherUtil.serializeRouteDistinquisher(route.getRouteDistinguisher(), output);
        output.writeBytes(IpAddressUtil.bytesFor(route.getMulticastSource()));
    }
}
