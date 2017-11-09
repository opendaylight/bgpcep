/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.Nanotime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanotimeUtilTest {
    private static final Logger LOG = LoggerFactory.getLogger(NanotimeUtilTest.class);

    @Test
    public void testCurrentTime() {
        assertTrue(NanotimeUtil.currentTime().getValue().divide(BigInteger.valueOf(1000000)).subtract(
                BigInteger.valueOf(System.currentTimeMillis())).shortValue() <= 0);
    }

    @Test
    public void testNanoTime() throws InterruptedException {
        final Nanotime nt1 = NanotimeUtil.currentNanoTime();
        Thread.sleep(1);
        final Nanotime nt2 = NanotimeUtil.currentNanoTime();

        LOG.debug("Times: {} {}", nt1, nt2);
        assertTrue(nt1.getValue().compareTo(nt2.getValue()) < 0);
    }
}
