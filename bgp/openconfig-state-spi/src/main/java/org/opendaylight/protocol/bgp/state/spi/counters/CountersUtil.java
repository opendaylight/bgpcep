/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.counters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CountersUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CountersUtil.class);

    private CountersUtil(){
        throw new UnsupportedOperationException();
    }

    /**
     * Increments counter by 1 if supported, otherwise produce a warn
     * @param counter counter
     * @param afiSafiType afiSafi Type
     */
    public static void increment(@Nullable final UnsignedInt32Counter counter,
        @Nonnull Class<? extends AfiSafiType> afiSafiType) {
        if(counter != null){
            counter.incrementCount();
            return;
        }
        LOG.warn("Not supported family {}", afiSafiType);
    }

    /**
     * Increments counter by 1 if supported, otherwise produce a warn
     * @param counter counter
     * @param afiSafiType afiSafi Type
     */
    public static void decrement(@Nullable final UnsignedInt32Counter counter,
        @Nonnull Class<? extends AfiSafiType> afiSafiType) {
        if(counter != null){
            counter.decrementCount();
            return;
        }
        LOG.warn("Not supported family {}", afiSafiType);
    }
}
