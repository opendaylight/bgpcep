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
import org.junit.Test;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.PeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.SpeakerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceived;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorMsgs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

public final class BgpCliUtilsTests{

    @Test
    public void testDisplayAll() throws IOException, JMException {
        //Arrange
        ObjectName objectName = mock(ObjectName.class);
        BgpSessionState bgpSessionState = mock(BgpSessionState.class);
        PrintStream printStream = mock(PrintStream.class);
        MessagesStats msgStats = mock(MessagesStats.class);
        SpeakerPreferences speakerPref = mock(SpeakerPreferences.class);
        PeerPreferences peerPref = mock(PeerPreferences.class);
        ErrorMsgs errorMsgs = mock(ErrorMsgs.class);
        ErrorReceived errorReceived = mock(ErrorReceived.class);

        when(bgpSessionState.getMessagesStats()).thenReturn(msgStats);
        when(speakerPref.getAddPathCapability()).thenReturn(true);
        when(peerPref.getAddPathCapability()).thenReturn(true);
        when(errorReceived.getCount()).thenReturn(5l);
        when(errorMsgs.getErrorReceived()).thenReturn(errorReceived);
        when(msgStats.getErrorMsgs()).thenReturn(errorMsgs);
        when(bgpSessionState.getSpeakerPreferences()).thenReturn(speakerPref);
        when(bgpSessionState.getPeerPreferences()).thenReturn(peerPref);

        //Act
        BgpCliUtils.displayAll(objectName, bgpSessionState, printStream);

        //Assert
        verify(objectName, atLeastOnce()).getCanonicalName();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getErrorMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getKeepAliveMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getRouteRefreshMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getTotalMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getUpdateMsgs();
        verify(bgpSessionState, atLeastOnce()).getPeerPreferences();
        verify(bgpSessionState, atLeastOnce()).getSpeakerPreferences();
    }
}
