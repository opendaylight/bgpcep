/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli.utils;

import java.io.IOException;
import java.io.PrintStream;
import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.karaf.shell.table.ShellTable;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

public final class BgpCliUtils{
    private static final String MESSAGE_STATS_LABEL = "MessagesStats.";
    private static final String PEER_PREFERENCES_LABEL = "PeerPreferences.";
    private static final String SPEAKER_PREFERENCES_LABEL = "SpeakerPreferences.";

    public static void displayAll(final ObjectName objectName, final BgpSessionState bgpSessionState, final PrintStream printStream) throws IOException, JMException {
        if (bgpSessionState != null) {
            final ShellTable table = new ShellTable();
            table.column("Attribute").alignLeft();
            table.column("Value").alignLeft();
            table.addRow().addContent("Object Name", objectName.getCanonicalName());
            table.addRow().addContent("HoldtimeCurrent", bgpSessionState.getHoldtimeCurrent());
            table.addRow().addContent("KeepaliveCurrent", bgpSessionState.getKeepaliveCurrent());
            table.addRow().addContent("SessionDuration", bgpSessionState.getSessionDuration());
            table.addRow().addContent("SessionState", bgpSessionState.getSessionState());

            //Messages Stats
            table.addRow().addContent(MESSAGE_STATS_LABEL + "ErrorMsgsSent", bgpSessionState.getMessagesStats().getErrorMsgs().getErrorSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "ErrorMsgsReceived", bgpSessionState.getMessagesStats().getErrorMsgs().getErrorReceived().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "KeepAliveMsgsSent", bgpSessionState.getMessagesStats().getKeepAliveMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "KeepAliveMsgsReceived", bgpSessionState.getMessagesStats().getKeepAliveMsgs().getReceived().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "TotalMsgsSent", bgpSessionState.getMessagesStats().getTotalMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "TotalMsgsReceived", bgpSessionState.getMessagesStats().getTotalMsgs().getReceived().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "UpdateMsgsSent", bgpSessionState.getMessagesStats().getUpdateMsgs().getSent().getCount());
            table.addRow().addContent(MESSAGE_STATS_LABEL + "UpdateMsgsReceived", bgpSessionState.getMessagesStats().getUpdateMsgs().getReceived().getCount());

            //Peer Preferences
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "AddPathCapability", bgpSessionState.getPeerPreferences().getAddPathCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "Address", bgpSessionState.getPeerPreferences().getAddress());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "AS", bgpSessionState.getPeerPreferences().getAs());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "BgpExtendedMessageCapability", bgpSessionState.getPeerPreferences().getBgpExtendedMessageCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "BgpId", bgpSessionState.getPeerPreferences().getBgpId());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "FourOctetAsCapability", bgpSessionState.getPeerPreferences().getFourOctetAsCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "GrCapability", bgpSessionState.getPeerPreferences().getGrCapability());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "Holdtime", bgpSessionState.getPeerPreferences().getHoldtime());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "Port", bgpSessionState.getPeerPreferences().getPort());
            table.addRow().addContent(PEER_PREFERENCES_LABEL + "RouteRefreshCapability", bgpSessionState.getPeerPreferences().getRouteRefreshCapability());

            //Speaker Preferences
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "Address", bgpSessionState.getSpeakerPreferences().getAddress());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "AS", bgpSessionState.getSpeakerPreferences().getAs());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "BgpExtendedMessageCapability", bgpSessionState.getSpeakerPreferences().getBgpExtendedMessageCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "BgpId", bgpSessionState.getSpeakerPreferences().getBgpId());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "FourOctetAsCapability", bgpSessionState.getSpeakerPreferences().getFourOctetAsCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "GrCapability", bgpSessionState.getSpeakerPreferences().getGrCapability());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "Holdtime", bgpSessionState.getSpeakerPreferences().getHoldtime());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "Port", bgpSessionState.getSpeakerPreferences().getPort());
            table.addRow().addContent(SPEAKER_PREFERENCES_LABEL + "RouteRefreshCapability", bgpSessionState.getSpeakerPreferences().getRouteRefreshCapability());
            table.print(printStream);
        } else {
            printStream.println(String.format("No BgpSessionState found for [%s]", objectName));
        }
    }
}
