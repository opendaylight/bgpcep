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
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPSessionState;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.open.object.open.Tlvs;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class SessionStateImpl implements PcepSessionState {
    private final Stopwatch sessionUpDuration = Stopwatch.createStarted();
    private final TopologySessionStats topologySessionStats;
    private final PCEPSessionState session;
    private final LocalPref localPref;
    private final PeerPref peerPref;

    @GuardedBy("this")
    private long minReplyMillis;
    @GuardedBy("this")
    private long maxReplyMillis;
    @GuardedBy("this")
    private long totalReplyMillis;
    @GuardedBy("this")
    private long requestCount;

    @GuardedBy("this")
    private long receivedRptMessageCount;
    @GuardedBy("this")
    private long receivedRptMessageTime;

    @GuardedBy("this")
    private long sentUpdMessageCount;
    @GuardedBy("this")
    private long sentInitMessageCount;

    public SessionStateImpl(final TopologySessionStats topologySessionStats, final PCEPSessionState session) {
        this.topologySessionStats = requireNonNull(topologySessionStats);
        this.session = requireNonNull(session);

        final SpeakerEntityId entityId = extractEntityId(session.getLocalOpen());
        localPref = entityId == null ? session.getLocalPref() : new LocalPrefBuilder(session.getLocalPref())
            .addAugmentation(new PcepEntityIdStatsAugBuilder(entityId).build())
            .build();
        peerPref = session.getPeerPref();
    }

    private static @Nullable SpeakerEntityId extractEntityId(final @NonNull Open localOpen) {
        final Tlvs tlvs = localOpen.getTlvs();
        if (tlvs != null) {
            final Tlvs3 aug = tlvs.augmentation(Tlvs3.class);
            if (aug != null) {
                return aug.getSpeakerEntityId();
            }
        }
        return null;
    }

    public synchronized void processRequestStats(final long durationMillis) {
        if (minReplyMillis == 0 || durationMillis < minReplyMillis) {
            minReplyMillis = durationMillis;
        }
        if (durationMillis > maxReplyMillis) {
            maxReplyMillis = durationMillis;
        }

        requestCount++;
        totalReplyMillis += durationMillis;
    }

    public synchronized void updateLastReceivedRptMsg() {
        receivedRptMessageCount++;
        receivedRptMessageTime = StatisticsUtil.getCurrentTimestampInSeconds();
    }

    public synchronized void updateStatefulSentMsg(final Message msg) {
        if (msg instanceof Pcinitiate) {
            sentInitMessageCount++;
        } else if (msg instanceof Pcupd) {
            sentUpdMessageCount++;
        }
    }

    @Override
    public String getSessionDuration() {
        return StatisticsUtil.formatElapsedTime(sessionUpDuration.elapsed(TimeUnit.SECONDS));
    }

    @Override
    public Boolean getSynchronized() {
        return topologySessionStats.isSessionSynchronized();
    }

    @Override
    public PeerCapabilities getPeerCapabilities() {
        return new PeerCapabilitiesBuilder()
            .addAugmentation(new StatefulCapabilitiesStatsAugBuilder()
                .setActive(topologySessionStats.isLspUpdateCapability())
                .setInstantiation(topologySessionStats.isInitiationCapability())
                .setStateful(topologySessionStats.isStatefulCapability())
                .build())
            .build();
    }

    @Override
    public Messages getMessages() {
        // Note: callout to session, do not hold lock
        final Messages sessionMessages = session.getMessages();

        synchronized (this) {
            final long averageReply = requestCount == 0 ? 0 : Math.round((double) totalReplyMillis / requestCount);

            return new MessagesBuilder(sessionMessages)
                .setReplyTime(new ReplyTimeBuilder()
                    .setAverageTime(Uint32.saturatedOf(averageReply))
                    .setMaxTime(Uint32.saturatedOf(maxReplyMillis))
                    .setMinTime(Uint32.saturatedOf(minReplyMillis))
                    .build())
                .addAugmentation(new StatefulMessagesStatsAugBuilder()
                    .setLastReceivedRptMsgTimestamp(Uint32.saturatedOf(receivedRptMessageTime))
                    .setReceivedRptMsgCount(Uint32.saturatedOf(receivedRptMessageCount))
                    .setSentInitMsgCount(Uint32.saturatedOf(sentInitMessageCount))
                    .setSentUpdMsgCount(Uint32.saturatedOf(sentUpdMessageCount))
                    .build())
                .build();
        }
    }

    @Override
    public LocalPref getLocalPref() {
        return localPref;
    }

    @Override
    public PeerPref getPeerPref() {
        return peerPref;
    }

    @Override
    public Uint16 getDelegatedLspsCount() {
        return Uint16.saturatedOf(topologySessionStats.getDelegatedLspsCount());
    }

    @Override
    public Class<PcepSessionState> implementedInterface() {
        return PcepSessionState.class;
    }
}
