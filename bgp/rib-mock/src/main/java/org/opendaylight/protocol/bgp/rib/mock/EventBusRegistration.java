/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has @Subscribe annotated methods which receive events from {@link EventBus} . Events are produced by
 * {@link BGPMock}, and each instance notifies exactly one {@link BGPSessionListener}.
 */
final class EventBusRegistration extends AbstractListenerRegistration<BGPSessionListener> {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusRegistration.class);

    private final EventBus eventBus;

    public static EventBusRegistration createAndRegister(final EventBus eventBus, final BGPSessionListener listener,
            final List<Notification<?>> allPreviousMessages) {
        final EventBusRegistration instance = new EventBusRegistration(eventBus, listener, allPreviousMessages);
        eventBus.register(instance);
        return instance;
    }

    private EventBusRegistration(final EventBus eventBus, final BGPSessionListener listener,
            final List<Notification<?>> allPreviousMessages) {
        super(listener);
        this.eventBus = eventBus;
        for (final Notification<?> message : allPreviousMessages) {
            sendMessage(listener, message);
        }
    }

    @Subscribe
    public void onMessage(final Notification<?> message) {
        sendMessage(this.getInstance(), message);
    }

    @Override
    public synchronized void removeRegistration() {
        eventBus.unregister(this);
    }

    private static void sendMessage(final BGPSessionListener listener, final Notification<?> message) {
        if (BGPMock.CONNECTION_LOST_MAGIC_MSG.equals(message)) {
            listener.onSessionTerminated(null, new BGPTerminationReason(BGPError.CEASE));
        } else if (message instanceof Open) {
            final Set<BgpTableType> tts = new HashSet<>();
            final List<AddressFamilies> addPathCapabilitiesList = new ArrayList<>();
            for (final BgpParameters param : ((Open) message).getBgpParameters()) {
                for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                    final CParameters cParam = capa.getCParameters();
                    final CParameters1 aug = cParam.augmentation(CParameters1.class);
                    if (aug == null) {
                        continue;
                    }
                    if (aug.getMultiprotocolCapability() != null) {
                        final MultiprotocolCapability p = aug.getMultiprotocolCapability();
                        LOG.debug("Adding open parameter {}", p);
                        final BgpTableType type = new BgpTableTypeImpl(p.getAfi(), p.getSafi());
                        tts.add(type);
                    } else if (aug.getAddPathCapability() != null) {
                        final AddPathCapability addPathCap = aug.getAddPathCapability();
                        addPathCapabilitiesList.addAll(addPathCap.getAddressFamilies());
                    }
                }
            }
            listener.onSessionUp(new MockBGPSession(tts, addPathCapabilitiesList));
        } else if (!(message instanceof Keepalive)) {
            try {
                listener.onMessage(new MockBGPSession(), message);
            } catch (BGPDocumentedException e) {
                LOG.warn("Exception encountered while handling message", e);
            }
        }
    }

    private static class MockBGPSession implements BGPSession {
        private static final Uint32 AS = Uint32.valueOf(30).intern();

        private final List<AddressFamilies> addPathAfis;
        private final Set<BgpTableType> tts;

        MockBGPSession(final Set<BgpTableType> tts, final List<AddressFamilies> addPathAfis) {
            this.tts = tts;
            this.addPathAfis = addPathAfis;
        }

        MockBGPSession() {
            this(Collections.emptySet(), Collections.emptyList());
        }

        @Override
        public void channelRegistered(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelUnregistered(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelActive(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelInactive(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelRead(final ChannelHandlerContext channelHandlerContext, final Object obj)
                throws Exception {
        }

        @Override
        public void channelReadComplete(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext channelHandlerContext, final Object obj)
                throws Exception {
        }

        @Override
        public void channelWritabilityChanged(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void handlerAdded(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void handlerRemoved(final ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final Throwable throwable)
                throws Exception {
        }

        @Override
        public void close() {
            LOG.debug("Session {} closed", this);
        }

        @Override
        public Set<BgpTableType> getAdvertisedTableTypes() {
            return tts;
        }

        @Override
        public Ipv4Address getBgpId() {
            return new Ipv4Address("127.0.0.1");
        }

        @Override
        public AsNumber getAsNumber() {
            return new AsNumber(AS);
        }

        @Override
        public List<AddressFamilies> getAdvertisedAddPathTableTypes() {
            return addPathAfis;
        }

        @Override
        public void closeWithoutMessage() {
            close();
        }

        @Override
        public <T extends PeerConstraint> void addDecoderConstraint(final Class<T> constraintClass,
                final T constraint) {
            // No-op
        }

        @Override
        public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
            return null;
        }
    }
}
