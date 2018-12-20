/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnParser;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnRegistry;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Mvpn Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class SimpleMvpnNlriRegistry implements MvpnRegistry {
    private static final SimpleMvpnNlriRegistry SINGLETON = new SimpleMvpnNlriRegistry();
    private final HandlerRegistry<DataContainer, MvpnParser, MvpnSerializer> handlers = new HandlerRegistry<>();

    private SimpleMvpnNlriRegistry() {
    }

    public static SimpleMvpnNlriRegistry getInstance() {
        return SINGLETON;
    }

    public <T extends MvpnChoice> Registration registerNlriParser(final MvpnParser<T> parser) {
        return this.handlers.registerParser(parser.getType(), parser);
    }

    public <T extends MvpnChoice> Registration registerNlriSerializer(
            final MvpnSerializer<T> serializer) {
        return this.handlers.registerSerializer(serializer.getClazz(), serializer);
    }

    @Override
    public MvpnChoice parseMvpn(final NlriType type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && nlriBuf.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final MvpnParser parser = this.handlers.getParser(type.getIntValue());
        if (parser == null) {
            return null;
        }
        return parser.parseMvpn(nlriBuf);
    }

    @Override
    public ByteBuf serializeMvpn(final MvpnChoice mvpn) {
        final MvpnSerializer serializer = this.handlers.getSerializer(mvpn.getImplementedInterface());
        if (serializer == null) {
            return Unpooled.buffer();
        }
        return serializer.serializeMvpn(mvpn);
    }
}
