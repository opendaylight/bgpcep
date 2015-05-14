package org.opendaylight.protocol.bgp.bmp.impl.message.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class RouteMonitoringMessageHandler extends AbstractBmpPerPeerMessageParser {

    public RouteMonitoringMessageHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }
}
