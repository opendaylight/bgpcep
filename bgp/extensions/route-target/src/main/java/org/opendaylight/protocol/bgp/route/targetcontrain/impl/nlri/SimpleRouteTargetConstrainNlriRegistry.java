/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargeConstraintNlriRegistry;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainParser;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Route Target Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class SimpleRouteTargetConstrainNlriRegistry implements RouteTargeConstraintNlriRegistry {
    private static final SimpleRouteTargetConstrainNlriRegistry SINGLETON =
            new SimpleRouteTargetConstrainNlriRegistry();
    private static final short RT_SUBTYPE = 2;
    private final HandlerRegistry<DataContainer, RouteTargetConstrainParser, RouteTargetConstrainSerializer> handlers =
            new HandlerRegistry<>();
    private final RouteTargetDefaultHandler defaultHandler = new RouteTargetDefaultHandler();

    private SimpleRouteTargetConstrainNlriRegistry() {
        this.handlers.registerSerializer(this.defaultHandler.getClazz(), this.defaultHandler);
    }

    public static SimpleRouteTargetConstrainNlriRegistry getInstance() {
        return SINGLETON;
    }

    public <T extends RouteTargetConstrainChoice> Registration registerNlriParser(
            final RouteTargetConstrainParser<T> parser) {
        return this.handlers.registerParser(parser.getType(), parser);
    }

    public <T extends RouteTargetConstrainChoice> Registration registerNlriSerializer(
            final RouteTargetConstrainSerializer<T> serializer) {
        return this.handlers.registerSerializer(serializer.getClazz(), serializer);
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public RouteTargetConstrainChoice parseRouteTargetConstrain(final Integer type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && (nlriBuf.isReadable() || type == null && !nlriBuf.isReadable()),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (type != null) {
            final RouteTargetConstrainParser<?> parser = this.handlers.getParser(type);
            return parser == null ? null : parser.parseRouteTargetConstrain(nlriBuf);
        }
        return this.defaultHandler.parseRouteTargetConstrain(nlriBuf);
    }

    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainChoice routeTarget) {
        final RouteTargetConstrainSerializer<RouteTargetConstrainChoice> serializer =
                this.handlers.getSerializer(routeTarget.implementedInterface());
        return serializer == null || serializer.getType() == null ? Unpooled.EMPTY_BUFFER
                : Unpooled.buffer()
                .writeByte(serializer.getType())
                .writeByte(RT_SUBTYPE)
                .writeBytes(serializer.serializeRouteTargetConstrain(routeTarget));
    }
}
