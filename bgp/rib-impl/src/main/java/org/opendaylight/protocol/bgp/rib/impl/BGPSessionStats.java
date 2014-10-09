/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AdvertizedTableTypes;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceived;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorSent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.KeepAliveMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.PeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Received;
import org.opendaylight.controller.config.yang.bgp.rib.impl.Sent;
import org.opendaylight.controller.config.yang.bgp.rib.impl.SpeakerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.TotalMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.UpdateMsgs;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl.State;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapability;

final class BGPSessionStats {
    private final Stopwatch sessionStopwatch;
    private final BgpSessionState stats;
    private final TotalMsgs totalMsgs = new TotalMsgs();
    private final KeepAliveMsgs kaMsgs = new KeepAliveMsgs();
    private final UpdateMsgs updMsgs = new UpdateMsgs();
    private final ErrorMsgs errMsgs = new ErrorMsgs();

    public BGPSessionStats(final Open remoteOpen, final int holdTimerValue, final int keepAlive, final Channel channel,
            final Optional<BGPSessionPreferences> localPreferences, final Collection<BgpTableType> tableTypes) {
        this.sessionStopwatch = new Stopwatch();
        this.stats = new BgpSessionState();
        this.stats.setHoldtimeCurrent(holdTimerValue);
        this.stats.setKeepaliveCurrent(keepAlive);
        this.stats.setPeerPreferences(setPeerPref(remoteOpen, channel, tableTypes));
        this.stats.setSpeakerPreferences(setSpeakerPref(channel, localPreferences));
        initMsgs();
    }

    private void initMsgs() {
        this.totalMsgs.setReceived(new Received());
        this.totalMsgs.setSent(new Sent());
        this.kaMsgs.setReceived(new Received());
        this.kaMsgs.setSent(new Sent());
        this.updMsgs.setReceived(new Received());
        this.updMsgs.setSent(new Sent());
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

    public void updateReceivedMsgErr(final Notify error) {
        Preconditions.checkNotNull(error);
        final ErrorReceived received = this.errMsgs.getErrorReceived();
        received.setCount(received.getCount() + 1);
        received.setTimestamp(StatisticsUtil.getCurrentTimestampInSeconds());
        received.setCode(error.getErrorCode());
        received.setSubCode(error.getErrorSubcode());
    }

    public void updateSentMsgErr(final Notify error) {
        Preconditions.checkNotNull(error);
        final ErrorSent sent = this.errMsgs.getErrorSent();
        sent.setCount(sent.getCount() + 1);
        sent.setTimestamp(StatisticsUtil.getCurrentTimestampInSeconds());
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
        received.setCount(received.getCount() + 1);
        received.setTimestamp(StatisticsUtil.getCurrentTimestampInSeconds());
    }

    private static boolean isAs4ByteCapable(final CParameters cp) {
        if (cp instanceof As4BytesCase) {
            return ((As4BytesCase) cp).getAs4BytesCapability() != null;
        }
        return false;
    }

    private static void updateSentMsg(final Sent sent) {
        Preconditions.checkNotNull(sent);
        sent.setCount(sent.getCount() + 1);
        sent.setTimestamp(StatisticsUtil.getCurrentTimestampInSeconds());
    }

    private static AdvertizedTableTypes addTableType(final BgpTableType type) {
        Preconditions.checkNotNull(type);
        final AdvertizedTableTypes att = new AdvertizedTableTypes();
        att.setAfi(type.getAfi().getSimpleName());
        att.setSafi(type.getSafi().getSimpleName());
        return att;
    }

    private static SpeakerPreferences setSpeakerPref(final Channel channel, final Optional<BGPSessionPreferences> localPreferences) {
        Preconditions.checkNotNull(channel);
        final SpeakerPreferences pref = new SpeakerPreferences();
        final InetSocketAddress isa = (InetSocketAddress) channel.localAddress();
        pref.setAddress(isa.getAddress().getHostAddress());
        pref.setPort(isa.getPort());
        final List<AdvertizedTableTypes> tt = Lists.newArrayList();
        if (localPreferences.isPresent()) {
            final BGPSessionPreferences localPref = localPreferences.get();
            pref.setBgpId(localPref.getBgpId().getValue());
            pref.setAs(localPref.getMyAs().getValue());
            pref.setHoldtime(localPref.getHoldTime());
            if (localPref.getParams() != null && !localPref.getParams().isEmpty()) {
                for (final BgpParameters param : localPref.getParams()) {
                    final CParameters cp = param.getCParameters();
                    if (cp instanceof MultiprotocolCase) {
                        final MultiprotocolCapability mc = ((MultiprotocolCase) cp).getMultiprotocolCapability();
                        final AdvertizedTableTypes att = new AdvertizedTableTypes();
                        att.setAfi(mc.getAfi().getSimpleName());
                        att.setSafi(mc.getSafi().getSimpleName());
                        tt.add(att);
                    }
                    pref.setFourOctetAsCapability(isAs4ByteCapable(cp));
                }
            }
        }
        pref.setAdvertizedTableTypes(tt);
        return pref;
    }

    private static PeerPreferences setPeerPref(final Open remoteOpen, final Channel channel, final Collection<BgpTableType> tableTypes) {
        Preconditions.checkNotNull(remoteOpen);
        Preconditions.checkNotNull(channel);
        final PeerPreferences pref = new PeerPreferences();
        final InetSocketAddress isa = (InetSocketAddress) channel.remoteAddress();
        pref.setAddress(isa.getAddress().getHostAddress());
        pref.setPort(isa.getPort());
        pref.setBgpId(remoteOpen.getBgpIdentifier().getValue());
        pref.setAs(remoteOpen.getMyAsNumber().longValue());
        pref.setHoldtime(remoteOpen.getHoldTimer());
        final List<AdvertizedTableTypes> tt = Lists.newArrayList();
        for (final BgpTableType t : tableTypes) {
            tt.add(addTableType(t));
        }
        if (remoteOpen.getBgpParameters() != null && !remoteOpen.getBgpParameters().isEmpty()) {
            for (final BgpParameters param : remoteOpen.getBgpParameters()) {
                pref.setFourOctetAsCapability(isAs4ByteCapable(param.getCParameters()));
            }
        }
        pref.setAdvertizedTableTypes(tt);
        return pref;
    }
}
