/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import com.google.common.collect.Lists;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;

public class SrEroUtilTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<SrEroUtil> c = SrEroUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testValidateSrEroSubobjects() {
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createSRSubobject()))));
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(Collections.<Subobject>emptyList())));
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(null)));
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createSRSubobject(20L, true)))));
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createSRSubobject(20L, false)))));
        Assert.assertNull(SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createSRSubobject(10L, false)))));
        Assert.assertEquals(PCEPErrors.BAD_LABEL_VALUE, SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createSRSubobject(10L, true)))));
        Assert.assertEquals(PCEPErrors.NON_IDENTICAL_ERO_SUBOBJECTS,
                SrEroUtil.validateSrEroSubobjects(createEro(Lists.newArrayList(createIpPrefixSubobject()))));
    }

    private Ero createEro(final List<Subobject> subobejcts) {
        return new EroBuilder().setSubobject(subobejcts).build();
    }

    private Subobject createSRSubobject() {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setSubobjectType(new SrEroTypeBuilder().build());
        return builder.build();
    }

    private Subobject createSRSubobject(final long sid, final boolean isM) {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setSubobjectType(new SrEroTypeBuilder().setMFlag(isM).setSid(sid).build());
        return builder.build();
    }

    private Subobject createIpPrefixSubobject() {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setSubobjectType(new IpPrefixCaseBuilder().build());
        return builder.build();
    }
}
