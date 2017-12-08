/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider.session.stats;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.protocol.pcep.PCEPSessionState;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.PcepEntityIdStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.PcepEntityIdStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulCapabilitiesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulMessagesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulMessagesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SessionStateImpl implements PcepSessionState {
    private final LongAdder lastReceivedRptMsgTimestamp = new LongAdder();
    private final LongAdder receivedRptMsgCount = new LongAdder();
    private final LongAdder sentUpdMsgCount = new LongAdder();
    private final LongAdder sentInitMsgCount = new LongAdder();
    private final Stopwatch sessionUpDuration;

    private final LongAdder minReplyTime = new LongAdder();
    private final LongAdder maxReplyTime = new LongAdder();
    private final LongAdder totalTime = new LongAdder();
    private final LongAdder reqCount = new LongAdder();
    private final TopologySessionStats topologySessionStats;
    private LocalPref localPref;
    private PeerPref peerPref;
    private PCEPSessionState pcepSessionState;

    public SessionStateImpl(final TopologySessionStats topologySessionStats) {
        this.sessionUpDuration = Stopwatch.createUnstarted();
        this.topologySessionStats = requireNonNull(topologySessionStats);
    }

    public synchronized void init(final PCEPSessionState session) {
        requireNonNull(session);
        this.pcepSessionState = session;
        final Open localOpen = session.getLocalOpen();

        if (localOpen.getTlvs() != null && localOpen.getTlvs().getAugmentation(Tlvs3.class) != null) {
            final SpeakerEntityId entityId = localOpen.getTlvs().getAugmentation(Tlvs3.class).getSpeakerEntityId();
            if (entityId != null) {
                this.localPref = new LocalPrefBuilder(session.getLocalPref())
                        .addAugmentation(PcepEntityIdStatsAug.class,
                                new PcepEntityIdStatsAugBuilder(entityId).build()).build();
            }
        } else {
            this.localPref = session.getLocalPref();
        }

        this.peerPref = session.getPeerPref();
        this.sessionUpDuration.start();
    }

    public synchronized void processRequestStats(final long duration) {
        if (this.minReplyTime.longValue() == 0) {
            this.minReplyTime.reset();
            this.minReplyTime.add(duration);
        } else if (duration < this.minReplyTime.longValue()) {
            this.minReplyTime.reset();
            this.minReplyTime.add(duration);
        }
        if (duration > this.maxReplyTime.longValue()) {
            this.maxReplyTime.reset();
            this.maxReplyTime.add(duration);
        }
        this.totalTime.add(duration);
        this.reqCount.increment();
    }

    public synchronized void updateLastReceivedRptMsg() {
        this.lastReceivedRptMsgTimestamp.reset();
        this.lastReceivedRptMsgTimestamp.add(StatisticsUtil.getCurrentTimestampInSeconds());
        this.receivedRptMsgCount.increment();
    }

    public synchronized void updateStatefulSentMsg(final Message msg) {
        if (msg instanceof Pcinitiate) {
            this.sentInitMsgCount.increment();
        } else if (msg instanceof Pcupd) {
            this.sentUpdMsgCount.increment();
        }
    }

    public synchronized String getSessionDuration() {
        return StatisticsUtil.formatElapsedTime(this.sessionUpDuration.elapsed(TimeUnit.SECONDS));
    }

    @Override
    public synchronized Boolean isSynchronized() {
        return this.topologySessionStats.isSessionSynchronized();
    }

    @Override
    public synchronized PeerCapabilities getPeerCapabilities() {
        return new PeerCapabilitiesBuilder()
                .addAugmentation(StatefulCapabilitiesStatsAug.class, createStatefulCapabilities())
                .build();
    }

    private synchronized StatefulCapabilitiesStatsAug createStatefulCapabilities() {
        return new StatefulCapabilitiesStatsAugBuilder()
                .setActive(this.topologySessionStats.isLspUpdateCapability())
                .setInstantiation(this.topologySessionStats.isInitiationCapability())
                .setStateful(this.topologySessionStats.isStatefulCapability())
                .build();
    }

    @Override
    public synchronized Messages getMessages() {
        return new MessagesBuilder(this.pcepSessionState.getMessages())
                .setReplyTime(setReplyTime())
                .addAugmentation(StatefulMessagesStatsAug.class, createStatefulMessages())
                .build();
    }

    private synchronized StatefulMessagesStatsAug createStatefulMessages() {
        return new StatefulMessagesStatsAugBuilder()
                .setLastReceivedRptMsgTimestamp(this.lastReceivedRptMsgTimestamp.longValue())
                .setReceivedRptMsgCount(this.receivedRptMsgCount.longValue())
                .setSentInitMsgCount(this.sentInitMsgCount.longValue())
                .setSentUpdMsgCount(this.sentUpdMsgCount.longValue())
                .build();
    }

    private synchronized ReplyTime setReplyTime() {
        long avg = 0;
        if (this.reqCount.longValue() != 0) {
            avg = Math.round((double) this.totalTime.longValue() / this.reqCount.longValue());
        }
        return new ReplyTimeBuilder()
                .setAverageTime(avg)
                .setMaxTime(this.maxReplyTime.longValue())
                .setMinTime(this.minReplyTime.longValue())
                .build();
    }

    @Override
    public synchronized LocalPref getLocalPref() {
        return this.localPref;
    }

    @Override
    public synchronized PeerPref getPeerPref() {
        return this.peerPref;
    }

    @Override
    public synchronized Integer getDelegatedLspsCount() {
        return this.topologySessionStats.getDelegatedLspsCount();
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return null;
    }
}
