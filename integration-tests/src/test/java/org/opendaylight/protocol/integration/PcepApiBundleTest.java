/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration;

import java.util.Collection;

import com.google.common.collect.Lists;

public final class PcepApiBundleTest extends AbstractBundleTest {
	@Override
	protected Collection<String> prerequisiteBundles() {
		return Lists.newArrayList("concepts", "framework", "util");
	}

	@Override
	protected Collection<String> requiredBundles() {
		return Lists.newArrayList("pcep-api");
	}
}
