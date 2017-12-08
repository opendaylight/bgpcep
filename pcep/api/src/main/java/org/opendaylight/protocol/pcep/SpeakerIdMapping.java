/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep;

import java.net.InetAddress;
import java.util.HashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpeakerIdMapping extends HashMap<InetAddress, byte[]> {
    private static final long serialVersionUID = 1L;

    private SpeakerIdMapping() {
        super();
    }

    public static SpeakerIdMapping getSpeakerIdMap(
            @Nonnull final InetAddress inetAddress,
            @Nullable final byte[] speakerId
    ) {
        final SpeakerIdMapping speakerIdMap = new SpeakerIdMapping();
        if (speakerId != null) {
            speakerIdMap.put(inetAddress, speakerId);
        }
        return speakerIdMap;
    }

    public static SpeakerIdMapping getSpeakerIdMap() {
        return new SpeakerIdMapping();
    }
}