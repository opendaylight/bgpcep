/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli;

import java.io.IOException;
import java.io.PrintStream;
import javax.management.JMException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.cli.utils.BgpCliUtils;

public class ShowBGPStats extends AbstractStatsCommandProvider {
    @Override
    public void execute(PrintStream printStream) throws IOException, JMException {
        super.execute(printStream);

        for (final ObjectName objectName : super.getObjectNames()) {
            final BgpSessionState state = super.getProxy(objectName).getBgpSessionState();
            BgpCliUtils.displayAll(objectName, state, printStream);
        }
    }
}
