/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi;

public interface RSVPExtensionConsumerContext {
    RSVPTeObjectRegistry getRsvpRegistry();

    XROSubobjectRegistry getXROSubobjectHandlerRegistry();

    EROSubobjectRegistry getEROSubobjectHandlerRegistry();

    RROSubobjectRegistry getRROSubobjectHandlerRegistry();

    LabelRegistry getLabelHandlerRegistry();
}
