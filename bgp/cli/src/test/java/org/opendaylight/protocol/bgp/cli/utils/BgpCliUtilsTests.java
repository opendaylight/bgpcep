/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli.utils;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.management.JMException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorMsgs;
import org.opendaylight.controller.config.yang.bgp.rib.impl.ErrorReceived;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocalPeerPreferences;
import org.opendaylight.controller.config.yang.bgp.rib.impl.MessagesStats;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RemotePeerPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;

public final class BgpCliUtilsTests{

    @Test
    public void testDisplayAll() throws IOException, JMException {
        //Arrange
        ObjectName objectName = mock(ObjectName.class);
        BgpSessionState bgpSessionState = mock(BgpSessionState.class);
        PrintStream printStream = mock(PrintStream.class);
        MessagesStats msgStats = mock(MessagesStats.class);
        RemotePeerPreferences speakerPref = mock(RemotePeerPreferences.class);
        LocalPeerPreferences peerPref = mock(LocalPeerPreferences.class);
        ErrorMsgs errorMsgs = mock(ErrorMsgs.class);
        ErrorReceived errorReceived = mock(ErrorReceived.class);
        List<ErrorReceived> newlist = new ArrayList<>();
        newlist.add(errorReceived);

        when(bgpSessionState.getMessagesStats()).thenReturn(msgStats);
        when(errorReceived.getCount()).thenReturn(new ZeroBasedCounter32(5L));

        when(errorMsgs.getErrorReceived()).thenReturn(newlist);
        when(msgStats.getErrorMsgs()).thenReturn(errorMsgs);
        when(bgpSessionState.getRemotePeerPreferences()).thenReturn(speakerPref);
        when(bgpSessionState.getLocalPeerPreferences()).thenReturn(peerPref);

        //Act
        BgpCliUtils.displayAll(objectName, bgpSessionState, printStream);

        //Assert
        verify(objectName, atLeastOnce()).getCanonicalName();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getErrorMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getKeepAliveMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getRouteRefreshMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getTotalMsgs();
        verify(bgpSessionState, atLeastOnce()).getMessagesStats().getUpdateMsgs();
        verify(bgpSessionState, atLeastOnce()).getLocalPeerPreferences();
        verify(bgpSessionState, atLeastOnce()).getRemotePeerPreferences();
    }
}
