/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.stats.peer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AdvertisedAddPathTableTypes;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AdvertizedTableTypes;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceived;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceivedTotal;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorSent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorSentTotal;
import org.opendaylight.controller.config.yang.bgp.rib.impl.KeepAliveMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocalPeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Received;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RemotePeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteRefreshMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Sent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.TotalMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.UpdateMsgs;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPSessionStatsImpl implements BGPSessionStats {
    private static final Logger LOG = LoggerFactory.getLogger(BGPSessionStatsImpl.class);

    private final Stopwatch sessionStopwatch;
    private final BgpSessionState stats;
    private final BGPSessionImpl session;
    private final TotalMsgs totalMsgs = new TotalMsgs();
    private final KeepAliveMsgs kaMsgs = new KeepAliveMsgs();
    private final UpdateMsgs updMsgs = new UpdateMsgs();
    private final RouteRefreshMsgs rrMsgs = new RouteRefreshMsgs();
    private final ErrorMsgs errMsgs = new ErrorMsgs();
    private final ErrorSentTotal errMsgsSentTotal = new ErrorSentTotal();
    private final ErrorReceivedTotal errMsgsRecvTotal = new ErrorReceivedTotal();
    private static final ZeroBasedCounter32 INITIAL_COUNTER = new ZeroBasedCounter32(0L);

    public BGPSessionStatsImpl(@Nonnull final BGPSessionImpl session, @Nonnull final Open remoteOpen, final int holdTimerValue, final int keepAlive, @Nonnull final Channel channel,
        @Nonnull final Optional<BGPSessionPreferences> localPreferences, @Nonnull final Collection<BgpTableType> tableTypes, @Nonnull final List<AddressFamilies> addPathTypes) {
        this.session = session;
        this.sessionStopwatch = Stopwatch.createUnstarted();
        this.stats = new BgpSessionState();
        this.stats.setHoldtimeCurrent(holdTimerValue);
        this.stats.setKeepaliveCurrent(keepAlive);
        this.stats.setLocalPeerPreferences(setLocalPeerPref(remoteOpen, channel, tableTypes, addPathTypes));
        this.stats.setRemotePeerPreferences(setRemotePeerPref(channel, localPreferences));
        this.errMsgs.setErrorReceivedTotal(this.errMsgsRecvTotal);
        this.errMsgs.setErrorSentTotal(this.errMsgsSentTotal);
        this.errMsgs.setErrorReceived(new ArrayList<>());
        this.errMsgs.setErrorSent(new ArrayList<>());
        initMsgs();
    }

    private static Received newReceivedInstance() {
        final Received recv = new Received();
        recv.setCount(INITIAL_COUNTER);
        return recv;
    }

    private static Sent newSentInstance() {
        final Sent sent = new Sent();
        sent.setCount(INITIAL_COUNTER);
        return sent;
    }

    private static void updateReceivedMsg(final Received received) {
        Preconditions.checkNotNull(received);
        final long count = received.getCount() == null ? 0L : received.getCount().getValue();
        received.setCount(new ZeroBasedCounter32(count + 1));
        received.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
    }

    private static void updateSentMsg(final Sent sent) {
        Preconditions.checkNotNull(sent);
        final long count = sent.getCount() == null ? 0L : sent.getCount().getValue();
        sent.setCount(new ZeroBasedCounter32(count + 1));
        sent.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
    }

    private static AdvertizedTableTypes addTableType(final BgpTableType type) {
        Preconditions.checkNotNull(type);
        final AdvertizedTableTypes att = new AdvertizedTableTypes();
        final QName afi = BindingReflections.findQName(type.getAfi()).intern();
        final QName safi = BindingReflections.findQName(type.getSafi()).intern();
        att.setAfi(new IdentityAttributeRef(afi.toString()));
        att.setSafi(new IdentityAttributeRef(safi.toString()));
        return att;
    }

    private static AdvertisedAddPathTableTypes addAddPathTableType(final AddressFamilies addressFamilies) {
        Preconditions.checkNotNull(addressFamilies);
        final AdvertisedAddPathTableTypes att = new AdvertisedAddPathTableTypes();
        att.setAfi(new IdentityAttributeRef(BindingReflections.findQName(addressFamilies.getAfi()).intern().toString()));
        att.setSafi(new IdentityAttributeRef(BindingReflections.findQName(addressFamilies.getSafi()).intern().toString()));
        att.setSendReceive(addressFamilies.getSendReceive());
        return att;
    }

    private static RemotePeerPreferences setRemotePeerPref(final Channel channel, final Optional<BGPSessionPreferences> localPreferences) {
        Preconditions.checkNotNull(channel);
        final RemotePeerPreferences pref = new RemotePeerPreferences();
        final InetSocketAddress isa = (InetSocketAddress) channel.localAddress();
        pref.setHost(IpAddressBuilder.getDefaultInstance(isa.getAddress().getHostAddress()));
        pref.setPort(new PortNumber(isa.getPort()));
        final List<AdvertizedTableTypes> tt = new ArrayList<>();
        final List<AdvertisedAddPathTableTypes> attList = new ArrayList<>();
        if (localPreferences.isPresent()) {
            final BGPSessionPreferences localPref = localPreferences.get();
            pref.setBgpId(localPref.getBgpId());
            pref.setAs(localPref.getMyAs());
            pref.setHoldtimer(localPref.getHoldTime());
            if (localPref.getParams() != null) {
                for (final BgpParameters param : localPref.getParams()) {
                    for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                        final CParameters cParam = capa.getCParameters();
                        final CParameters1 capabilities = cParam.getAugmentation(CParameters1.class);
                        if (capabilities != null) {
                            final MultiprotocolCapability mc = capabilities.getMultiprotocolCapability();
                            if (mc != null) {
                                final AdvertizedTableTypes att = new AdvertizedTableTypes();
                                att.setAfi(new IdentityAttributeRef(BindingReflections.findQName(mc.getAfi()).intern().toString()));
                                att.setSafi(new IdentityAttributeRef(BindingReflections.findQName(mc.getSafi()).intern().toString()));
                                tt.add(att);
                            }
                            if (capabilities.getGracefulRestartCapability() != null) {
                                pref.setGrCapability(true);
                            }
                            if (capabilities.getAddPathCapability() != null) {
                                final List<AdvertisedAddPathTableTypes> addPathTableTypeList = capabilities.getAddPathCapability()
                                    .getAddressFamilies()
                                    .stream()
                                    .map(BGPSessionStatsImpl::addAddPathTableType)
                                    .collect(Collectors.toList());
                                attList.addAll(addPathTableTypeList);
                            }
                            if (capabilities.getRouteRefreshCapability() != null) {
                                pref.setRouteRefreshCapability(true);
                            }
                        }
                        if (cParam.getAs4BytesCapability() != null) {
                            pref.setFourOctetAsCapability(true);
                        }
                        if (cParam.getBgpExtendedMessageCapability() != null) {
                            pref.setBgpExtendedMessageCapability(true);
                        }
                    }
                }
            }
        }
        pref.setAdvertizedTableTypes(tt);
        pref.setAdvertisedAddPathTableTypes(attList);
        return pref;
    }

    private static LocalPeerPreferences setLocalPeerPref(final Open remoteOpen, final Channel channel, final Collection<BgpTableType> tableTypes,
        final List<AddressFamilies> addPathTypes) {
        Preconditions.checkNotNull(remoteOpen);
        Preconditions.checkNotNull(channel);
        final LocalPeerPreferences pref = new LocalPeerPreferences();
        final InetSocketAddress isa = (InetSocketAddress) channel.remoteAddress();
        pref.setHost(IpAddressBuilder.getDefaultInstance(isa.getAddress().getHostAddress()));
        pref.setPort(new PortNumber(isa.getPort()));
        pref.setBgpId(new BgpId(remoteOpen.getBgpIdentifier()));
        pref.setAs(new AsNumber(remoteOpen.getMyAsNumber().longValue()));
        pref.setHoldtimer(remoteOpen.getHoldTimer());

        final List<AdvertizedTableTypes> tt = tableTypes.stream().map(BGPSessionStatsImpl::addTableType).collect(Collectors.toList());
        final List<AdvertisedAddPathTableTypes> addPathTableTypeList = addPathTypes.stream().map(BGPSessionStatsImpl::addAddPathTableType)
            .collect(Collectors.toList());

        if (remoteOpen.getBgpParameters() != null) {
            for (final BgpParameters param : remoteOpen.getBgpParameters()) {
                for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                    final CParameters cParam = capa.getCParameters();
                    final CParameters1 capabilities = cParam.getAugmentation(CParameters1.class);
                    if (cParam.getAs4BytesCapability() != null) {
                        pref.setFourOctetAsCapability(true);
                    }
                    if (capabilities != null) {
                        if (capabilities.getGracefulRestartCapability() != null) {
                            pref.setGrCapability(true);
                        }
                        if (capabilities.getRouteRefreshCapability() != null) {
                            pref.setRouteRefreshCapability(true);
                        }
                    }
                    if (cParam.getBgpExtendedMessageCapability() != null) {
                        pref.setBgpExtendedMessageCapability(true);
                    }
                }

            }
        }
        pref.setAdvertizedTableTypes(tt);
        pref.setAdvertisedAddPathTableTypes(addPathTableTypeList);
        return pref;
    }

    private void initMsgs() {
        this.totalMsgs.setReceived(newReceivedInstance());
        this.totalMsgs.setSent(newSentInstance());
        this.kaMsgs.setReceived(newReceivedInstance());
        this.kaMsgs.setSent(newSentInstance());
        this.updMsgs.setReceived(newReceivedInstance());
        this.updMsgs.setSent(newSentInstance());
        this.rrMsgs.setReceived(newReceivedInstance());
        this.rrMsgs.setSent(newSentInstance());
        this.errMsgsSentTotal.setCount(INITIAL_COUNTER);
        this.errMsgsRecvTotal.setCount(INITIAL_COUNTER);
        this.errMsgs.getErrorSent().clear();
        this.errMsgs.getErrorReceived().clear();
    }

    public void startSessionStopwatch() {
        this.sessionStopwatch.start();
    }

    private void updateSentMsgTotal() {
        updateSentMsg(this.totalMsgs.getSent());
    }

    private void updateReceivedMsgTotal() {
        updateReceivedMsg(this.totalMsgs.getReceived());
    }

    private void updateReceivedMsgKA() {
        updateReceivedMsg(this.kaMsgs.getReceived());
    }

    public void updateSentMsgKA() {
        updateSentMsg(this.kaMsgs.getSent());
    }

    private void updateSentMsgUpd() {
        updateSentMsg(this.updMsgs.getSent());
    }

    private void updateReceivedMsgUpd() {
        updateReceivedMsg(this.updMsgs.getReceived());
    }

    private void updateSentMsgRR() {
        updateSentMsg(this.rrMsgs.getSent());
    }

    private void updateReceivedMsgRR() {
        updateReceivedMsg(this.rrMsgs.getReceived());
    }

    private void updateReceivedMsgErr(@Nonnull final Notify error) {
        Preconditions.checkNotNull(error);
        final List<ErrorReceived> errList = this.errMsgs.getErrorReceived();
        ErrorReceived received = null;
        for (final ErrorReceived err : errList) {
            if (err.getErrorCode().equals(error.getErrorCode()) && err.getErrorSubcode().equals(error.getErrorSubcode())) {
                received = err;
                break;
            }
        }
        if (null == received) {
            received = new ErrorReceived();
            received.setErrorCode(error.getErrorCode());
            received.setErrorSubcode(error.getErrorSubcode());
            received.setCount(INITIAL_COUNTER);
            errList.add(received);
        }
        received.setCount(new ZeroBasedCounter32(received.getCount().getValue() + 1));
        final Timestamp curTimestamp = new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds());
        received.setTimestamp(curTimestamp);
        this.errMsgsRecvTotal.setCount(new ZeroBasedCounter32(this.errMsgsRecvTotal.getCount().getValue() + 1));
        this.errMsgsRecvTotal.setTimestamp(curTimestamp);
    }

    private void updateSentMsgErr(@Nonnull final Notify error) {
        Preconditions.checkNotNull(error);
        final List<ErrorSent> errList = this.errMsgs.getErrorSent();
        ErrorSent sent = null;
        for (final ErrorSent err : errList) {
            if (err.getErrorCode().equals(error.getErrorCode()) && err.getErrorSubcode().equals(error.getErrorSubcode())) {
                sent = err;
                break;
            }
        }
        if (null == sent) {
            sent = new ErrorSent();
            sent.setErrorCode(error.getErrorCode());
            sent.setErrorSubcode(error.getErrorSubcode());
            sent.setCount(INITIAL_COUNTER);
            errList.add(sent);
        }
        sent.setCount(new ZeroBasedCounter32(sent.getCount().getValue() + 1));
        final Timestamp curTimestamp = new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds());
        sent.setTimestamp(curTimestamp);
        this.errMsgsSentTotal.setCount(new ZeroBasedCounter32(this.errMsgsSentTotal.getCount().getValue() + 1));
        this.errMsgsSentTotal.setTimestamp(curTimestamp);
    }

    @Override
    public BgpSessionState getBgpSessionState() {
        final MessagesStats msgs = new MessagesStats();
        msgs.setTotalMsgs(this.totalMsgs);
        msgs.setErrorMsgs(this.errMsgs);
        msgs.setKeepAliveMsgs(this.kaMsgs);
        msgs.setUpdateMsgs(this.updMsgs);
        msgs.setRouteRefreshMsgs(this.rrMsgs);
        this.stats.setSessionDuration(StatisticsUtil.formatElapsedTime(this.sessionStopwatch.elapsed(TimeUnit.SECONDS)));
        this.stats.setSessionState(this.session.getState().toString());
        this.stats.setMessagesStats(msgs);
        return this.stats;
    }

    @Override
    public void resetBgpSessionStats() {
        initMsgs();
    }

    public void updateReceivedMsg(final Notification msg) {
        LOG.trace("Updating received BGP session message count..");
        this.updateReceivedMsgTotal();
        if (msg instanceof Notify) {
            this.updateReceivedMsgErr((Notify) msg);
        } else if (msg instanceof Keepalive) {
            this.updateReceivedMsgKA();
        } else if (msg instanceof RouteRefresh) {
            this.updateReceivedMsgRR();
        } else if (msg instanceof Update) {
            this.updateReceivedMsgUpd();
        }
    }

    public void updateSentMsg(final Notification msg) {
        LOG.trace("Updating sent BGP session message count..");
        this.updateSentMsgTotal();
        if (msg instanceof Update) {
            this.updateSentMsgUpd();
        } else if (msg instanceof Notify) {
            this.updateSentMsgErr((Notify) msg);
        } else if (msg instanceof RouteRefresh) {
            this.updateSentMsgRR();
        }
    }
}
