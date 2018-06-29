/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides util for increment/decrement counters whenever is not null
 * Otherwise prints an informative warn.
 */
public final class CountersUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CountersUtil.class);

    private CountersUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Increments counter by 1 if supported, otherwise produce a warn.
     *
     * @param counter   counter
     * @param tablesKey tablesKey Type
     */
    static void increment(@Nullable final LongAdder counter, @Nonnull TablesKey tablesKey) {
        if (counter != null) {
            counter.increment();
            return;
        }
        LOG.warn("Family {} not supported", tablesKey);
    }

    /**
     * Increments counter by 1 if supported, otherwise produce a warn.
     *
     * @param counter   counter
     * @param tablesKey tablesKey Type
     */
    static void decrement(@Nullable final LongAdder counter, @Nonnull TablesKey tablesKey) {
        if (counter != null) {
            counter.decrement();
            return;
        }
        LOG.warn("Family {} not supported", tablesKey);
    }
}
