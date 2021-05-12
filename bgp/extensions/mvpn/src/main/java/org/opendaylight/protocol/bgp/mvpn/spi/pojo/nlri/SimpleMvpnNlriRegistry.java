/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.AbstractMvpnNlri;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.InterASIPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.IntraAsIPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.LeafADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SharedTreeJoinHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SourceActiveADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SourceTreeJoinHandler;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnParser;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnRegistry;
import org.opendaylight.protocol.bgp.mvpn.spi.nlri.MvpnSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;

/**
 * Mvpn Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class SimpleMvpnNlriRegistry implements MvpnRegistry {
    private static final @NonNull SimpleMvpnNlriRegistry INSTANCE = new SimpleMvpnNlriRegistry();

    @SuppressWarnings("rawtypes")
    private final ImmutableMap<Class<? extends MvpnChoice>, AbstractMvpnNlri> serializers;
    @SuppressWarnings("rawtypes")
    private final ImmutableMap<NlriType, AbstractMvpnNlri> parsers;

    private SimpleMvpnNlriRegistry() {
        @SuppressWarnings("rawtypes")
        final List<AbstractMvpnNlri> handlers = List.of(
            new IntraAsIPmsiADHandler(),
            new InterASIPmsiADHandler(),
            new SPmsiADHandler(),
            new LeafADHandler(),
            new SourceActiveADHandler(),
            new SharedTreeJoinHandler(),
            new SourceTreeJoinHandler());

        serializers = Maps.uniqueIndex(handlers, MvpnSerializer::getClazz);
        parsers = Maps.uniqueIndex(handlers, MvpnParser::getType);
    }

    public static @NonNull SimpleMvpnNlriRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public MvpnChoice parseMvpn(final NlriType type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && nlriBuf.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        @SuppressWarnings("unchecked")
        final MvpnParser<MvpnChoice> parser = parsers.get(type);
        return parser == null ? null : parser.parseMvpn(nlriBuf);
    }

    @Override
    public ByteBuf serializeMvpn(final MvpnChoice mvpn) {
        @SuppressWarnings("unchecked")
        final AbstractMvpnNlri<MvpnChoice> serializer = serializers.get(mvpn.implementedInterface());
        return serializer == null ? Unpooled.EMPTY_BUFFER : serializer.serializeMvpn(mvpn);
    }
}
