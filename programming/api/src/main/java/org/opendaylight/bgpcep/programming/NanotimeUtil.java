/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming;

import java.math.BigInteger;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class NanotimeUtil {
	private static final Logger LOG = LoggerFactory.getLogger(NanotimeUtil.class);
	private static final BigInteger MILLION = BigInteger.valueOf(1000000);
	private static volatile BigInteger nanoTimeOffset = null;

	private NanotimeUtil() {

	}

	public static final Nanotime currentTime() {
		return new Nanotime(BigInteger.valueOf(System.currentTimeMillis()).multiply(MILLION));
	}

	public static final Nanotime currentNanoTime() {
		if (nanoTimeOffset == null) {
			calibrate();
		}

		return new Nanotime(BigInteger.valueOf(System.nanoTime()).add(nanoTimeOffset));
	}

	public static final void calibrate() {
		final long tm1 = System.currentTimeMillis();
		final long nt1 = System.nanoTime();
		final long tm2 = System.currentTimeMillis();
		final long nt2 = System.nanoTime();

		LOG.debug("Calibrated currentTime and nanoTime to {}m <= {}n <= {}m <= {}n", tm1, nt1, tm2, nt2);

		final BigInteger tm = BigInteger.valueOf(tm1).add(BigInteger.valueOf(tm2)).divide(BigInteger.valueOf(2));
		final BigInteger nt = BigInteger.valueOf(nt1).add(BigInteger.valueOf(nt2)).divide(BigInteger.valueOf(2));

		nanoTimeOffset = nt.subtract(tm.multiply(MILLION));
	}
}
