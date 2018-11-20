/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;

/**
 * Exception thrown from the parser when it encounters an UPDATE message with recoverable parsing errors, as specified
 * by RFC7606. Errors encountered are available in {@link #getErrors()} and users recognizing this exception must ensure
 * corresponding {@link Notify} messages are sent to the peer before the recovered message, from {@link #getUpdate()},
 * is applied as if it were a correct message.
 *
 * @author Robert Varga
 */
public final class BGPRecoveredUpdateException extends Exception {
    private static final long serialVersionUID = 1L;

    private final List<BGPDocumentedException> errors;
    private final Update update;

    public BGPRecoveredUpdateException(final Update update, final List<BGPDocumentedException> errors) {
        this.update = requireNonNull(update);
        this.errors = ImmutableList.copyOf(errors);
    }

    public List<BGPDocumentedException> getErrors() {
        return errors;
    }

    public Update getUpdate() {
        return update;
    }
}
