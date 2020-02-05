/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;

/**
 * Parsed {@link Attributes}, potentially indicating withdraw.
 *
 * @author Robert Varga
 */
public final class ParsedAttributes {
    private final @NonNull Attributes attributes;
    private final @Nullable BGPTreatAsWithdrawException withdrawCause;

    public ParsedAttributes(final @NonNull Attributes attributes,
            final @Nullable BGPTreatAsWithdrawException withdrawCause) {
        this.attributes = requireNonNull(attributes);
        this.withdrawCause = withdrawCause;
    }

    public @NonNull Attributes getAttributes() {
        return attributes;
    }

    public @NonNull Optional<BGPTreatAsWithdrawException> getWithdrawCause() {
        return Optional.ofNullable(withdrawCause);
    }
}
