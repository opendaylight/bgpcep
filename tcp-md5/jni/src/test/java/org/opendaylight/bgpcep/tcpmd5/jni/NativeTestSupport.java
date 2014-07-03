/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.tcpmd5.jni;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(NativeTestSupport.class);

    public static void assumeSupportedPlatform() {
        try {
            NativeKeyAccessFactory.getInstance();
        }catch (NativeSupportUnavailableException e) {
            LOG.info("Skipping test, this platform is not supported");
            Assume.assumeNoException(e);
        }
    }
}
