/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.yang.igp.algo.types.rev261110;

import static com.google.common.base.Verify.verifyNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Utility methods for acquiring shared {@link FlexAlgo} and {@link AlgoType} instances.
 */
public final class OdlIgpAlgoUtil {
    private static final @NonNull AlgoType SPF = new AlgoType(NamedAlgo.Spf);
    private static final @NonNull AlgoType STRICT_SPF = new AlgoType(NamedAlgo.StrictSpf);
    private static final AlgoType[] FLEX = new AlgoType[128];

    private OdlIgpAlgoUtil() {
        // hidden on purpose
    }

    @NonNullByDefault
    public static AlgoType algoSpf() {
        return SPF;
    }

    @NonNullByDefault
    public static AlgoType algoStrictSpf() {
        return STRICT_SPF;
    }

    @NonNullByDefault
    private static AlgoType getFlex(final byte bits) {
        final var offset = 128 + bits;
        final var existing = FLEX[offset];
        return existing != null ? existing : loadFlex(bits, offset);
    }

    @NonNullByDefault
    private static AlgoType loadFlex(final byte bits, final int offset) {
        // TODO: use VarHandle.compareAndExchange()
        synchronized (FLEX) {
            var ret = FLEX[offset];
            if (ret == null) {
                FLEX[offset] = ret = new AlgoType(new FlexAlgo(Uint8.fromByteBits(bits)));
            }
            return ret;
        }
    }

    public static @Nullable FlexAlgo flexAlgoFor(final @NonNull Uint8 value) {
        final var bits = value.byteValue();
        return bits < 0 ? verifyNotNull(getFlex(bits).getFlexAlgo()) : null;
    }

    @NonNullByDefault
    public static FlexAlgo flexAlgoOf(final Uint8 value) {
        final var ret = flexAlgoFor(value);
        if (ret == null) {
            throw new IllegalArgumentException(value + " is not a valid IGP Flexible Algorithm");
        }
        return ret;
    }

    public static @Nullable AlgoType algoTypeFor(final @NonNull Uint8 value) {
        final var bits = value.byteValue();
        return switch (bits) {
            case 0 -> SPF;
            case 1 -> STRICT_SPF;
            default -> bits < 0 ? getFlex(bits) : null;
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
