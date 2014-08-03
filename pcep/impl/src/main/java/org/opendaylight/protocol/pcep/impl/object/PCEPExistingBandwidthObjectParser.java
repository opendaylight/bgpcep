/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;

/**
 * Parser for Bandwidth
 */
public class PCEPExistingBandwidthObjectParser extends AbstractBandwidthParser {

    public static final int CLASS = 5;

    public static final int TYPE = 2;

    @Override
    protected void formatBandwidth(final Boolean processed, final Boolean ignored, final ByteBuf body, final ByteBuf buffer) {
        ObjectUtil.formatSubobject(TYPE, CLASS, processed, ignored, body, buffer);
    }
}
