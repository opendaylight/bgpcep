/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FluentFuture;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPSessionState;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Callback for updating session state registered with {@link SessionStateRegistry}.
 */
final class SessionStateUpdater {
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

    private final TopologyNodeState node;

    SessionStateUpdater(final TopologySessionStats topologySessionStats, final PCEPSessionState session,
            final TopologyNodeState node) {
        this.topologySessionStats = requireNonNull(topologySessionStats);
        this.session = requireNonNull(session);
        this.node = requireNonNull(node);

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

    @NonNull FluentFuture<? extends @NonNull CommitInfo> updateStatistics() {
        // Lockless
        final var aug = new PcepTopologyNodeStatsAugBuilder().setPcepSessionState(toPcepSessionState()).build();

        // FIXME: locking of this, check with session, etc. lifecycle
        final var tx = node.getChain().newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, node.getNodeId().augmentation(PcepTopologyNodeStatsAug.class), aug);
        return tx.commit();
    }

    // FIXME: add a caller
    @NonNull FluentFuture<? extends @NonNull CommitInfo> removeStatistics() {
        final var tx = node.getChain().newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, node.getNodeId().augmentation(PcepTopologyNodeStatsAug.class));
        return tx.commit();
    }

    @VisibleForTesting
    @NonNull PcepSessionState toPcepSessionState() {
        // Note: callout to session, do not hold lock
        final Messages sessionMessages = session.getMessages();

        synchronized (this) {
            final long averageReply = requestCount == 0 ? 0 : Math.round((double) totalReplyMillis / requestCount);

            return new PcepSessionStateBuilder()
                .setLocalPref(localPref)
                .setPeerPref(peerPref)
                .setSessionDuration(StatisticsUtil.formatElapsedTime(sessionUpDuration.elapsed(TimeUnit.SECONDS)))
                .setSynchronized(topologySessionStats.isSessionSynchronized())
                .setDelegatedLspsCount(getDelegatedLspsCount())
                .setPeerCapabilities(new PeerCapabilitiesBuilder()
                    .addAugmentation(new StatefulCapabilitiesStatsAugBuilder()
                        .setActive(topologySessionStats.isLspUpdateCapability())
                        .setInstantiation(topologySessionStats.isInitiationCapability())
                        .setStateful(topologySessionStats.isStatefulCapability())
                        .build())
                    .build())
                .setMessages(new MessagesBuilder(sessionMessages)
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
                    .build())
                .build();
        }
    }

    @VisibleForTesting
    @NonNull Uint16 getDelegatedLspsCount() {
        return Uint16.saturatedOf(topologySessionStats.getDelegatedLspsCount());
    }

    synchronized void processRequestStats(final long durationMillis) {
        if (minReplyMillis == 0 || durationMillis < minReplyMillis) {
            minReplyMillis = durationMillis;
        }
        if (durationMillis > maxReplyMillis) {
            maxReplyMillis = durationMillis;
        }

        requestCount++;
        totalReplyMillis += durationMillis;
    }

    synchronized void updateLastReceivedRptMsg() {
        receivedRptMessageCount++;
        receivedRptMessageTime = StatisticsUtil.getCurrentTimestampInSeconds();
    }

    synchronized void updateStatefulSentMsg(final Message msg) {
        if (msg instanceof Pcinitiate) {
            sentInitMessageCount++;
        } else if (msg instanceof Pcupd) {
            sentUpdMessageCount++;
        }
    }
}
