/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

/**
 * Util class for encoding/decoding 20bit leftmost value.
 */
public final class MplsLabelUtil {

    private static final int LABEL_OFFSET = 4;
    private static final byte BOTTOM_LABEL_BIT = 0x1;

    private MplsLabelUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates and returns MplsLabel object from 20 leftmost bits in the incoming buffer.
     * Moves reader index by 3.
     * @param buffer expecting 3 bytes with 20 leftmost bits as label
     * @return MplsLabel object
     */
    public static MplsLabel mplsLabelForByteBuf(final ByteBuf buffer) {
        return new MplsLabel(Long.valueOf(buffer.readUnsignedMedium() >> LABEL_OFFSET));
    }

    /**
     * Serializes incoming MplsLabel without bottom bit
     * @param label MplsLabel object
     * @return
     */
    public static ByteBuf byteBufForMplsLabel(final MplsLabel label) {
        return Unpooled.copyMedium(intForMplsLabel(label));
    }

    /**
     * Serializes incoming MplsLabel with bottom bit
     * @param label MplsLabel object
     * @return
     */
    public static ByteBuf byteBufForMplsLabelWithBottomBit(final MplsLabel label) {
        return Unpooled.copyMedium(intForMplsLabelWithBottomBit(label));
    }

    /**
     * Makes a value of incoming label 20 leftmost bits in 24bit number
     * @param label object
     * @return shifted value
     */
    public static int intForMplsLabel(final MplsLabel label) {
        return label.getValue().intValue() << LABEL_OFFSET;
    }

    /**
     * Makes a value of incoming label 20 leftmost bits in 24bit number and sets bottom bit
     * @param label object
     * @return value with bottom bit
     */
    private static int intForMplsLabelWithBottomBit(final MplsLabel label) {
        final int value = intForMplsLabel(label);
        return setBottomBit(value);
    }

    /**
     * Sets bottom bit of 3 byte value
     * @param value where 20 leftmost bits are label
     * @return value with set bottom bit
     */
    private static int setBottomBit(final int value) {
        return value | BOTTOM_LABEL_BIT;
    }

    /**
     * @param slice with 20 leftmost bits as label
     * @return value of bottom bit
     */
    public static boolean getBottomBit(final ByteBuf slice) {
        return (slice.getUnsignedMedium(slice.readerIndex()) & BOTTOM_LABEL_BIT) == 1;
    }
}
