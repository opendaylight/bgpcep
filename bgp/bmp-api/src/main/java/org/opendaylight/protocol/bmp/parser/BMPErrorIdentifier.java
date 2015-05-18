package org.opendaylight.protocol.bmp.parser;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Created by cgasparini on 18.5.2015.
 */
public class BMPErrorIdentifier {
    private final short code;
    private final short subcode;

    BMPErrorIdentifier(final short code, final short subcode) {
        this.code = code;
        this.subcode = subcode;
    }

    public short getCode() {
        return this.code;
    }

    public short getSubCode() {
        return this.subcode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.code;
        result = prime * result + this.subcode;
        return result;
    }

    @Override
    public boolean equals(final java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final BMPErrorIdentifier other = (BMPErrorIdentifier) obj;
        if (this.code != other.code) {
            return false;
        }
        if (this.subcode != other.subcode) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "type " + this.code + " value " + this.subcode;
    }
}
