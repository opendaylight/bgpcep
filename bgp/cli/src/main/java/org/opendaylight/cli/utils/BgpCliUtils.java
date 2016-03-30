/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.cli.utils;

import java.io.IOException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public final class BgpCliUtils{
    public static final String BGP_MBEANS_GET_STATS_OPERATION = "BgpSessionState";
    public static final String BGP_MBEANS_NAME = "org.opendaylight.controller:instanceName=example-bgp-peer,type=RuntimeBean,moduleFactoryName=bgp-peer";
    public static final String BGP_MBEANS_RESET_STATS_OPERATION = " resetStats";

    public static String displayAll(MBeanServerConnection conn,
                                    ObjectName pattern) throws IOException, JMException {
        StringBuffer buffer = new StringBuffer();
        final String separator = "---------------------------------------------------------";

        MBeanDataDisplay display = new MBeanDataDisplay(conn);
        buffer.append(separator);
        for (ObjectName mbean : conn.queryNames(pattern, null)) {
            buffer.append(display.toString(mbean));
            buffer.append(separator);
        }

        return buffer.toString();
    }
}