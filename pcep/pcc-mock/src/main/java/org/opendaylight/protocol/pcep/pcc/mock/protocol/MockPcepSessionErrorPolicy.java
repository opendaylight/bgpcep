/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionErrorPolicy;
import org.opendaylight.yangtools.yang.common.Uint16;

public record MockPcepSessionErrorPolicy(Uint16 maxUnknownMessages) implements PcepSessionErrorPolicy {
    public static final MockPcepSessionErrorPolicy ZERO = new MockPcepSessionErrorPolicy(Uint16.ZERO);

    public MockPcepSessionErrorPolicy {
        requireNonNull(maxUnknownMessages);
    }

    @Override
    public Uint16 getMaxUnknownMessages() {
        return maxUnknownMessages;
    }

    @Override
    public Class<? extends PcepSessionErrorPolicy> implementedInterface() {
        throw new UnsupportedOperationException();
    }
}
