/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.yang.igp.algo.types.rev261110;

import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana.igp.algo.types.rev260709.AlgoType.AlgoSpf;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana.igp.algo.types.rev260709.AlgoType.AlgoStrictSpf;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Utility methods for acquiring shared {@link AlgoFlex} and {@link AlgoType} instances.
 */
public final class OdlIgpAlgoUtil {
    private static final AlgoType TYPE_0 = new AlgoType(AlgoSpf);
    private static final AlgoType TYPE_1 = new AlgoType(AlgoStrictSpf);
    private static final AlgoType[] TYPE_FLEX = new AlgoType[128];

    private OdlIgpAlgoUtil() {
        // hidden on purpose
    }

    @NonNullByDefault
    private static AlgoType getTypeFlex(final byte bits) {
        final var offset = 128 + bits;
        final var existing = TYPE_FLEX[offset];
        return existing != null ? existing : loadTypeFlex(bits, offset);
    }

    @NonNullByDefault
    private static AlgoType loadTypeFlex(final byte bits, final int offset) {
        // TODO: use VarHandle.compareAndExchange()
        synchronized (TYPE_FLEX) {
            var ret = TYPE_FLEX[offset];
            if (ret == null) {
                TYPE_FLEX[offset] = ret = new AlgoType(new AlgoFlex(Uint8.fromByteBits(bits)));
            }
            return ret;
        }
    }

    public static @Nullable AlgoFlex algoFlexFor(final @NonNull Uint8 value) {
        final var bits = value.byteValue();
        return bits < 0 ? getTypeFlex(bits).getAlgoFlex(): null;
    }

    @NonNullByDefault
    public static AlgoFlex algoFlexOf(final Uint8 value) {
        final var ret = algoFlexFor(value);
        if (ret == null) {
            throw new IllegalArgumentException(value + " is not a valid IGP Flexible Algorithm");
        }
        return ret;
    }

    public static @Nullable AlgoType algoTypeFor(final @NonNull Uint8 value) {
        final var bits = value.byteValue();
        return switch (bits) {
            case 0 -> TYPE_0;
            case 1 -> TYPE_1;
            default -> bits < 0 ? getTypeFlex(bits) : null;
        };
    }

    @NonNullByDefault
    public static AlgoType algoTypeOf(final Uint8 value) {
        final var ret = algoTypeFor(value);
        if (ret == null) {
            throw new IllegalArgumentException(value + " is not a valid IGP Algorithm Type");
        }
        return ret;
    }
}
