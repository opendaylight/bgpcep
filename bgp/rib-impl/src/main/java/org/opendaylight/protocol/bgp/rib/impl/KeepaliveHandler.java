/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static com.google.common.base.Verify.verify;

import io.netty.handler.timeout.IdleStateHandler;

final class KeepaliveHandler extends IdleStateHandler {
    private static final int KA_TO_DEADTIMER_RATIO = 3;

    KeepaliveHandler(final int holdTimerSeconds) {
        super(holdTimerSeconds, holdTimerSeconds / KA_TO_DEADTIMER_RATIO, 0);
        verify(holdTimerSeconds > 0);
    }
}
