/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.cli.utils;

import org.apache.karaf.shell.table.ShellTable;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

import java.io.IOException;
import javax.management.JMException;

public final class BgpCliUtils{
    public static final String BGP_MBEANS_NAME = "org.opendaylight.controller:instanceName=example-bgp-peer,type=RuntimeBean,moduleFactoryName=bgp-peer";
    public static final String BGP_MBEANS_RESET_STATS_OPERATION = "resetStats";

    public static void displayAll(final BgpSessionState bgpSessionState) throws IOException, JMException {
        final ShellTable table = new ShellTable();
        table.column("Name").alignLeft();
        table.column("Value").alignLeft();
        table.addRow().addContent("HoldtimeCurrent", bgpSessionState.getHoldtimeCurrent());
        table.addRow().addContent("KeepaliveCurrent", bgpSessionState.getKeepaliveCurrent());
        table.addRow().addContent("SessionDuration", bgpSessionState.getSessionDuration());
        table.addRow().addContent("SessionState", bgpSessionState.getSessionState());

        //Messages Stats
        table.addRow().addContent("MessagesStats.ErrorMsgsSent", bgpSessionState.getMessagesStats().getErrorMsgs().getErrorSent());
        table.addRow().addContent("MessagesStats.ErrorMsgsReceived", bgpSessionState.getMessagesStats().getErrorMsgs().getErrorReceived());
        table.addRow().addContent("MessagesStats.KeepAliveMsgsSent", bgpSessionState.getMessagesStats().getKeepAliveMsgs().getSent());
        table.addRow().addContent("MessagesStats.KeepAliveMsgsReceived", bgpSessionState.getMessagesStats().getKeepAliveMsgs().getReceived());
        table.addRow().addContent("MessagesStats.TotalMsgsSent", bgpSessionState.getMessagesStats().getTotalMsgs().getSent());
        table.addRow().addContent("MessagesStats.TotalMsgsReceived", bgpSessionState.getMessagesStats().getTotalMsgs().getReceived());
        table.addRow().addContent("MessagesStats.UpdateMsgsSent", bgpSessionState.getMessagesStats().getUpdateMsgs().getSent());
        table.addRow().addContent("MessagesStats.UpdateMsgsReceived", bgpSessionState.getMessagesStats().getUpdateMsgs().getReceived());

        //Peer Preferences
        table.addRow().addContent("PeerPreferences.AddPathCapability", bgpSessionState.getPeerPreferences().getAddPathCapability());
        table.addRow().addContent("PeerPreferences.Address", bgpSessionState.getPeerPreferences().getAddress());
        table.addRow().addContent("PeerPreferences.AS", bgpSessionState.getPeerPreferences().getAs());
        table.addRow().addContent("PeerPreferences.BgpExtendedMessageCapability", bgpSessionState.getPeerPreferences().getBgpExtendedMessageCapability());
        table.addRow().addContent("PeerPreferences.BgpId", bgpSessionState.getPeerPreferences().getBgpId());
        table.addRow().addContent("PeerPreferences.FourOctetAsCapability", bgpSessionState.getPeerPreferences().getFourOctetAsCapability());
        table.addRow().addContent("PeerPreferences.GrCapability", bgpSessionState.getPeerPreferences().getGrCapability());
        table.addRow().addContent("PeerPreferences.Holdtime", bgpSessionState.getPeerPreferences().getHoldtime());
        table.addRow().addContent("PeerPreferences.Port", bgpSessionState.getPeerPreferences().getPort());
        table.addRow().addContent("PeerPreferences.RouteRefreshCapability", bgpSessionState.getPeerPreferences().getRouteRefreshCapability());

        //Speaker Preferences
        table.addRow().addContent("SpeakerPreferences.AddPathCapability", bgpSessionState.getSpeakerPreferences().getAddPathCapability());
        table.addRow().addContent("SpeakerPreferences.Address", bgpSessionState.getSpeakerPreferences().getAddress());
        table.addRow().addContent("SpeakerPreferences.AS", bgpSessionState.getSpeakerPreferences().getAs());
        table.addRow().addContent("SpeakerPreferences.BgpExtendedMessageCapability", bgpSessionState.getSpeakerPreferences().getBgpExtendedMessageCapability());
        table.addRow().addContent("SpeakerPreferences.BgpId", bgpSessionState.getSpeakerPreferences().getBgpId());
        table.addRow().addContent("SpeakerPreferences.FourOctetAsCapability", bgpSessionState.getSpeakerPreferences().getFourOctetAsCapability());
        table.addRow().addContent("SpeakerPreferences.GrCapability", bgpSessionState.getSpeakerPreferences().getGrCapability());
        table.addRow().addContent("SpeakerPreferences.Holdtime", bgpSessionState.getSpeakerPreferences().getHoldtime());
        table.addRow().addContent("SpeakerPreferences.Port", bgpSessionState.getSpeakerPreferences().getPort());
        table.addRow().addContent("SpeakerPreferences.RouteRefreshCapability", bgpSessionState.getSpeakerPreferences().getRouteRefreshCapability());

        table.print(System.out);
    }
}