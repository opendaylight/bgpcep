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
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AdvertisedAddPathTableTypes;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AdvertizedTableTypes;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceived;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorSent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.KeepAliveMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocalPeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Received;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RemotePeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteRefreshMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Sent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.TotalMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.UpdateMsgs;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl.State;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public final class BGPSessionStatsImpl {
    private final Stopwatch sessionStopwatch;
    private final BgpSessionState stats;
    private final TotalMsgs totalMsgs = new TotalMsgs();
    private final KeepAliveMsgs kaMsgs = new KeepAliveMsgs();
    private final UpdateMsgs updMsgs = new UpdateMsgs();
    private final RouteRefreshMsgs rrMsgs = new RouteRefreshMsgs();
    private final ErrorMsgs errMsgs = new ErrorMsgs();

    public BGPSessionStatsImpl(final Open remoteOpen, final int holdTimerValue, final int keepAlive, final Channel channel,
        final Optional<BGPSessionPreferences> localPreferences, final Collection<BgpTableType> tableTypes, final List<AddressFamilies> addPathTypes) {
        this.sessionStopwatch = Stopwatch.createUnstarted();
        this.stats = new BgpSessionState();
        this.stats.setHoldtimeCurrent(holdTimerValue);
        this.stats.setKeepaliveCurrent(keepAlive);
        this.stats.setLocalPeerPreferences(setLocalPeerPref(remoteOpen, channel, tableTypes, addPathTypes));
        this.stats.setRemotePeerPreferences(setRemotePeerPref(channel, localPreferences));
        initMsgs();
    }

    private void initMsgs() {
        this.totalMsgs.setReceived(new Received());
        this.totalMsgs.setSent(new Sent());
        this.kaMsgs.setReceived(new Received());
        this.kaMsgs.setSent(new Sent());
        this.updMsgs.setReceived(new Received());
        this.updMsgs.setSent(new Sent());
        this.rrMsgs.setReceived(new Received());
        this.rrMsgs.setSent(new Sent());
        this.errMsgs.setErrorReceived(new ErrorReceived());
        this.errMsgs.setErrorSent(new ErrorSent());
    }

    public void startSessionStopwatch() {
        this.sessionStopwatch.start();
    }

    public void updateSentMsgTotal() {
        updateSentMsg(this.totalMsgs.getSent());
    }

    public void updateReceivedMsgTotal() {
        updateReceivedMsg(this.totalMsgs.getReceived());
    }

    public void updateReceivedMsgKA() {
        updateReceivedMsg(this.kaMsgs.getReceived());
    }

    public void updateSentMsgKA() {
        updateSentMsg(this.kaMsgs.getSent());
    }

    public void updateSentMsgUpd() {
        updateSentMsg(this.updMsgs.getSent());
    }

    public void updateReceivedMsgUpd() {
        updateReceivedMsg(this.updMsgs.getReceived());
    }

    public void updateSentMsgRR() {
        updateSentMsg(this.rrMsgs.getSent());
    }

    public void updateReceivedMsgRR() {
        updateReceivedMsg(this.rrMsgs.getReceived());
    }

    public void updateReceivedMsgErr(final Notify error) {
        Preconditions.checkNotNull(error);
        final ErrorReceived received = this.errMsgs.getErrorReceived();
        received.setCount(new ZeroBasedCounter32(received.getCount().getValue() + 1));
        received.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
        received.setCode(error.getErrorCode());
        received.setSubCode(error.getErrorSubcode());
    }

    public void updateSentMsgErr(final Notify error) {
        Preconditions.checkNotNull(error);
        final ErrorSent sent = this.errMsgs.getErrorSent();
        sent.setCount(new ZeroBasedCounter32(sent.getCount().getValue() + 1));
        sent.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
        sent.setCode(error.getErrorCode());
        sent.setSubCode(error.getErrorSubcode());
    }

    public BgpSessionState getBgpSessionState(final State state) {
        Preconditions.checkNotNull(state);
        final MessagesStats msgs = new MessagesStats();
        msgs.setTotalMsgs(this.totalMsgs);
        msgs.setErrorMsgs(this.errMsgs);
        msgs.setKeepAliveMsgs(this.kaMsgs);
        msgs.setUpdateMsgs(this.updMsgs);
        msgs.setRouteRefreshMsgs(this.rrMsgs);
        this.stats.setSessionDuration(StatisticsUtil.formatElapsedTime(this.sessionStopwatch.elapsed(TimeUnit.SECONDS)));
        this.stats.setSessionState(state.toString());
        this.stats.setMessagesStats(msgs);
        return this.stats;
    }

    public void resetStats() {
        initMsgs();
    }

    private static void updateReceivedMsg(final Received received) {
        Preconditions.checkNotNull(received);
        received.setCount(new ZeroBasedCounter32(received.getCount().getValue() + 1));
        received.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
    }

    private static void updateSentMsg(final Sent sent) {
        Preconditions.checkNotNull(sent);
        sent.setCount(new ZeroBasedCounter32(sent.getCount().getValue() + 1));
        sent.setTimestamp(new Timestamp(StatisticsUtil.getCurrentTimestampInSeconds()));
    }

    private static AdvertizedTableTypes addTableType(final BgpTableType type) {
        Preconditions.checkNotNull(type);
        final AdvertizedTableTypes att = new AdvertizedTableTypes();
        att.setAfi(new IdentityAttributeRef(BindingReflections.findQName(type.getAfi()).intern().toString()));
        att.setSafi(new IdentityAttributeRef(BindingReflections.findQName(type.getSafi()).intern().toString()));
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
        if (localPreferences.isPresent()) {
            final BGPSessionPreferences localPref = localPreferences.get();
            pref.setBgpId(new BgpId(localPref.getBgpId()));
            pref.setAs(localPref.getMyAs().getValue());
            pref.setHoldtimer(localPref.getHoldTime());
            if (localPref.getParams() != null) {
                for (final BgpParameters param : localPref.getParams()) {
                    for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                        final CParameters cParam = capa.getCParameters();
                        if(cParam.getAugmentation(CParameters1.class) != null) {
                            final MultiprotocolCapability mc = cParam.getAugmentation(CParameters1.class).getMultiprotocolCapability();
                            if (mc != null) {
                                final AdvertizedTableTypes att = new AdvertizedTableTypes();
                                att.setAfi(new IdentityAttributeRef(BindingReflections.findQName(mc.getAfi()).intern().toString()));
                                att.setAfi(new IdentityAttributeRef(BindingReflections.findQName(mc.getSafi()).intern().toString()));
                                tt.add(att);
                            }
                            if (cParam.getAs4BytesCapability() != null) {
                                pref.setFourOctetAsCapability(true);
                            }
                            if (cParam.getAugmentation(CParameters1.class) != null &&
                                    cParam.getAugmentation(CParameters1.class).getGracefulRestartCapability() != null) {
                                pref.setGrCapability(true);
                            }
                            if (cParam.getAugmentation(CParameters1.class) != null &&
                                    cParam.getAugmentation(CParameters1.class).getAddPathCapability() != null) {
                                pref.setAddPathCapability(true);
                            }
                            if (cParam.getBgpExtendedMessageCapability() != null) {
                                pref.setBgpExtendedMessageCapability(true);
                            }
                            if (cParam.getAugmentation(CParameters1.class) != null &&
                                cParam.getAugmentation(CParameters1.class).getRouteRefreshCapability() != null) {
                                pref.setRouteRefreshCapability(true);
                            }
                        }
                    }
                }
            }
        }
        pref.setAdvertizedTableTypes(tt);
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
        pref.setBgpId(new BgpId(remoteOpen.getBgpIdentifier().getValue()));
        pref.setAs(remoteOpen.getMyAsNumber().longValue());
        pref.setHoldtimer(remoteOpen.getHoldTimer());

        final List<AdvertizedTableTypes> tt = tableTypes.stream().map(BGPSessionStatsImpl::addTableType).collect(Collectors.toList());
        final List<AdvertisedAddPathTableTypes> addPathTableTypeList = addPathTypes.stream().map(BGPSessionStatsImpl::addAddPathTableType)
            .collect(Collectors.toList());

        if (remoteOpen.getBgpParameters() != null) {
            for (final BgpParameters param : remoteOpen.getBgpParameters()) {
                for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                    final CParameters cParam = capa.getCParameters();
                    if (cParam.getAs4BytesCapability() != null) {
                        pref.setFourOctetAsCapability(true);
                    }
                    if (cParam.getAugmentation(CParameters1.class) != null &&
                            cParam.getAugmentation(CParameters1.class).getGracefulRestartCapability() != null) {
                        pref.setGrCapability(true);
                    }
                    // FIXME add path capability is deprecated
                    if (cParam.getAugmentation(CParameters1.class) != null &&
                            cParam.getAugmentation(CParameters1.class).getAddPathCapability() != null) {
                        pref.setAddPathCapability(true);
                    }
                    if (cParam.getAugmentation(CParameters1.class) != null &&
                        cParam.getAugmentation(CParameters1.class).getRouteRefreshCapability() != null) {
                        pref.setRouteRefreshCapability(true);
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
}
