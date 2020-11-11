/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eclipse.jdt.annotation.NonNull;
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
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Mvpn Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class SimpleMvpnNlriRegistry implements MvpnRegistry {
    private static final @NonNull SimpleMvpnNlriRegistry INSTANCE = new SimpleMvpnNlriRegistry();

    private final HandlerRegistry<DataContainer, MvpnParser, MvpnSerializer> handlers = new HandlerRegistry<>();

    private SimpleMvpnNlriRegistry() {
        final IntraAsIPmsiADHandler intra = new IntraAsIPmsiADHandler();
        registerNlriParser(intra);
        registerNlriSerializer(intra);

        final InterASIPmsiADHandler inter = new InterASIPmsiADHandler();
        registerNlriParser(inter);
        registerNlriSerializer(inter);

        final SPmsiADHandler sPmsiADHandler = new SPmsiADHandler();
        registerNlriParser(sPmsiADHandler);
        registerNlriSerializer(sPmsiADHandler);

        final LeafADHandler leafHandler = new LeafADHandler();
        registerNlriParser(leafHandler);
        registerNlriSerializer(leafHandler);

        final SourceActiveADHandler activeADHandler = new SourceActiveADHandler();
        registerNlriParser(activeADHandler);
        registerNlriSerializer(activeADHandler);

        final SharedTreeJoinHandler sharedTreeJoinHandler = new SharedTreeJoinHandler();
        registerNlriParser(sharedTreeJoinHandler);
        registerNlriSerializer(sharedTreeJoinHandler);

        final SourceTreeJoinHandler sourceTreeJoinHandler = new SourceTreeJoinHandler();
        registerNlriParser(sourceTreeJoinHandler);
        registerNlriSerializer(sourceTreeJoinHandler);
    }

    public static @NonNull SimpleMvpnNlriRegistry getInstance() {
        return INSTANCE;
    }

    private <T extends MvpnChoice> void registerNlriParser(final MvpnParser<T> parser) {
        handlers.registerParser(parser.getType(), parser);
    }

    private <T extends MvpnChoice> void registerNlriSerializer(final MvpnSerializer<T> serializer) {
        handlers.registerSerializer(serializer.getClazz(), serializer);
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public MvpnChoice parseMvpn(final NlriType type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && nlriBuf.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final MvpnParser<MvpnChoice> parser = handlers.getParser(type.getIntValue());
        return parser == null ? null : parser.parseMvpn(nlriBuf);
    }

    @Override
    public ByteBuf serializeMvpn(final MvpnChoice mvpn) {
        final MvpnSerializer<MvpnChoice> serializer = handlers.getSerializer(mvpn.implementedInterface());
        return serializer == null ? Unpooled.EMPTY_BUFFER : serializer.serializeMvpn(mvpn);
    }
}
