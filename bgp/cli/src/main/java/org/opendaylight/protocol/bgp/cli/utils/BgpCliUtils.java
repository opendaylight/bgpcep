/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli.utils;

import java.io.PrintStream;
import javax.management.ObjectName;
import org.apache.karaf.shell.table.ShellTable;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocalPeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RemotePeerPreferences;

public final class BgpCliUtils {
    private static final String MESSAGE_STATS_LABEL = "MessagesStats.";
    private static final String PEER_PREFERENCES_LABEL = "PeerPreferences.";
    private static final String SPEAKER_PREFERENCES_LABEL = "SpeakerPreferences.";

    private BgpCliUtils() {
        throw new UnsupportedOperationException();
    }
    public static void displayAll(final ObjectName objectName, final BgpSessionState bgpSessionState, final PrintStream printStream) {
        if (bgpSessionState == null) {
            printStream.println(String.format("No BgpSessionState found for [%s]", objectName));
            return;
        }
        final ShellTable table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();
        table.addRow().addContent("Object Name", objectName.getCanonicalName());
        table.addRow().addContent("HoldtimeCurrent", bgpSessionState.getHoldtimeCurrent());
        table.addRow().addContent("KeepaliveCurrent", bgpSessionState.getKeepaliveCurrent());
        table.addRow().addContent("SessionDuration", bgpSessionState.getSessionDuration());
        table.addRow().addContent("SessionState", bgpSessionState.getSessionState());

        //Messages Stats
        final MessagesStats messageStats = bgpSessionState.getMessagesStats();
        if (messageStats != null) {
            table.addRow().addContent(MESSAGE_STATS_LABEL + "ErrorMsgsSent", messageStats.getErrorMsgs().getErrorSent().size());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "ErrorMsgsReceived", messageStats.getErrorMsgs().getErrorReceived().size());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "KeepAliveMsgsSent", messageStats.getKeepAliveMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "KeepAliveMsgsReceived", messageStats.getKeepAliveMsgs().getReceived().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "TotalMsgsSent", messageStats.getTotalMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "TotalMsgsReceived", messageStats.getTotalMsgs().getReceived().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "UpdateMsgsSent", messageStats.getUpdateMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "UpdateMsgsReceived", messageStats.getUpdateMsgs().getReceived().getCount());
        }

        //Peer Preferences
        final LocalPeerPreferences peerPreferences = bgpSessionState.getLocalPeerPreferences();
        if (peerPreferences != null) {
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "AS", peerPreferences.getAs());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "BgpExtendedMessageCapability", peerPreferences.getBgpExtendedMessageCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "BgpId", peerPreferences.getBgpId());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "FourOctetAsCapability", peerPreferences.getFourOctetAsCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "GrCapability", peerPreferences.getGrCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "Port", peerPreferences.getPort());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "RouteRefreshCapability", peerPreferences.getRouteRefreshCapability());
        }

        //Speaker Preferences
        final RemotePeerPreferences speakerPreferences = bgpSessionState.getRemotePeerPreferences();
        if (speakerPreferences != null) {
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "AS", speakerPreferences.getAs());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "BgpExtendedMessageCapability", speakerPreferences.getBgpExtendedMessageCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "BgpId", speakerPreferences.getBgpId());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "FourOctetAsCapability", speakerPreferences.getFourOctetAsCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "GrCapability", speakerPreferences.getGrCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "Port", speakerPreferences.getPort());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "RouteRefreshCapability", speakerPreferences.getRouteRefreshCapability());
        }

        table.print(printStream);
    }
}
