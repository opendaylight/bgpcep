/*
 * Copyright (c) 2016 AT&T Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableMap;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class KeyMapping extends HashMap<InetAddress, byte[]> {
    private static final long serialVersionUID = 1L;

    private KeyMapping() {
        super();
    }

    public static KeyMapping getKeyMapping(@Nonnull final InetAddress inetAddress, @Nullable final String password){
        final KeyMapping keyMapping = new KeyMapping();
        if (!isNullOrEmpty(password)) {
            keyMapping.put(inetAddress, password.getBytes(StandardCharsets.US_ASCII));
        }
        return keyMapping;
    }

    public static KeyMapping getKeyMapping(){
        return new KeyMapping();
    }
}
