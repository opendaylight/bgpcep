/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.route.target.spi.nlri.RouteTargetNlriRegistry;
import org.opendaylight.protocol.bgp.route.target.spi.nlri.RouteTargetParser;
import org.opendaylight.protocol.bgp.route.target.spi.nlri.RouteTargetSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Route Target Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class SimpleRouteTargetNlriRegistry implements RouteTargetNlriRegistry {
    private static final SimpleRouteTargetNlriRegistry SINGLETON = new SimpleRouteTargetNlriRegistry();
    private final HandlerRegistry<DataContainer, RouteTargetParser, RouteTargetSerializer> handlers
            = new HandlerRegistry<>();
    private final RouteTargetDefaultHandler defaultHandler;

    private SimpleRouteTargetNlriRegistry() {
        this.defaultHandler = new RouteTargetDefaultHandler();
        this.handlers.registerSerializer(this.defaultHandler.getClazz(), this.defaultHandler);
    }

    public static SimpleRouteTargetNlriRegistry getInstance() {
        return SINGLETON;
    }

    public <T extends RouteTargetChoice> AutoCloseable registerNlriParser(
            final RouteTargetParser<T> parser) {
        return this.handlers.registerParser(parser.getType(), parser);
    }

    public <T extends RouteTargetChoice> AutoCloseable registerNlriSerializer(
            final RouteTargetSerializer<T> serializer) {
        return this.handlers.registerSerializer(serializer.getClazz(), serializer);
    }

    public RouteTargetChoice parseRouteTarget(final Integer type, final ByteBuf nlriBuf) {
        Preconditions.checkArgument(nlriBuf != null
                        && (nlriBuf.isReadable() || type == null && !nlriBuf.isReadable()),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (type == null) {
            return this.defaultHandler.parseRouteTarget(nlriBuf);
        }
        final RouteTargetParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseRouteTarget(nlriBuf);
    }

    @SuppressWarnings("unchecked")
    public ByteBuf serializeRouteTarget(final RouteTargetChoice rtc) {
        final RouteTargetSerializer serializer = this.handlers.getSerializer(rtc.getImplementedInterface());
        if (serializer == null) {
            return Unpooled.buffer();
        }
        return serializer.serializeRouteTarget(rtc);
    }
}
