/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.common.BandwidthObjectCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.common.BandwidthObjectCommonBuilder;

abstract class AbstractBandwidthParser {

    protected static final int BANDWIDTH_F_LENGTH = 4;

    protected BandwidthObjectCommon parseBandhwidth(final byte[] bytes) {
        return new BandwidthObjectCommonBuilder().setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125
            .Bandwidth(bytes)).build();
    }
}
