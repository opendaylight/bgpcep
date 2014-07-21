/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration;

import com.google.common.collect.Lists;
import java.util.Collection;

public final class PcepImplBundleTest extends AbstractBundleTest {
    @Override
    protected Collection<String> prerequisiteBundles() {
        return Lists.newArrayList("concepts", "pcep-api", "pcep-spi", "pcep-ietf-stateful02", "pcep-ietf-stateful07",
                "pcep-topology-api", "pcep-tunnel-api", "rsvp-api", "programming-api", "programming-topology-api", "topology-api",
                "topology-tunnel-api", "programming-tunnel-api", "util");
    }

    @Override
    protected Collection<String> requiredBundles() {
        return Lists.newArrayList("pcep-impl");
    }
}
