/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * Listener that receives session informations from the session.
 */
public interface PCEPSessionListener {
    void onSessionUp(PCEPSession var1);

    void onSessionDown(PCEPSession var1, Exception var2);

    void onSessionTerminated(PCEPSession var1, PCEPTerminationReason var2);

    void onMessage(PCEPSession var1, Message var2);
}
