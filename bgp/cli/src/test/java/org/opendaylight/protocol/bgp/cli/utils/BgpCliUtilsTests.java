/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli.utils;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintStream;
import javax.management.JMException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

public final class BgpCliUtilsTests{
    @Test
    public void displayAll() throws IOException, JMException {
        ObjectName objectName = mock(ObjectName.class);
        BgpSessionState bgpSessionState = mock(BgpSessionState.class);
        PrintStream printStream = mock(PrintStream.class);
        BgpCliUtils.displayAll(objectName, bgpSessionState, printStream);

        verify(objectName, atLeast(1)).getCanonicalName();
        verify(bgpSessionState, atLeast(8)).getMessagesStats();
        verify(bgpSessionState, atLeast(9)).getSpeakerPreferences();
        verify(bgpSessionState, atLeast(10)).getPeerPreferences();
    }
}
