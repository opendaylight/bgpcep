/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

//FIXME: merge with org.opendaylight.controller.test.sal.binding.it.AbstractTest ?
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public abstract class AbstractBundleTest {
    private static final String GROUP = "org.opendaylight.bgpcep";

    @Inject
    @Filter(timeout = 60 * 1000)
    BundleContext ctx;

    abstract protected Collection<String> prerequisiteBundles();

    abstract protected Collection<String> requiredBundles();

    private List<Option> coreBundles() {
        final List<Option> ret = new ArrayList<>();

        ret.add(mavenBundle("com.google.guava", "guava").versionAsInProject());
        ret.add(mavenBundle("commons-codec", "commons-codec").versionAsInProject());
        ret.add(mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject());
        ret.add(mavenBundle("commons-io", "commons-io").versionAsInProject());

        TestHelper.configMinumumBundles();

        ret.add(mavenBundle("org.opendaylight.yangtools", "concepts").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "object-cache-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-binding").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-common").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-data-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-data-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-data-util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-model-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-model-util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-parser-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "yang-parser-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-data-codec").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-generator-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-generator-spi").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-generator-util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-generator-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-model-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools", "binding-type-provider").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools.model", "ietf-inet-types").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools.model", "ietf-yang-types").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools.model", "ietf-topology").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools.model", "yang-ext").versionAsInProject());

        ret.add(mavenBundle("org.opendaylight.tcpmd5", "tcpmd5-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.tcpmd5", "tcpmd5-netty").versionAsInProject());

        ret.add(mavenBundle("org.javassist", "javassist").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "config-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "netty-config-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "protocol-framework").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-common-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-core-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-core-spi").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-api").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-broker-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-inmemory-datastore").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-config").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-binding-util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-broker-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-common").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-common-impl").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.controller", "sal-common-util").versionAsInProject());
        ret.add(mavenBundle("org.opendaylight.yangtools.thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject());

        ret.add(systemProperty("pax.exam.osgi.unresolved.fail").value("true"));

        ret.add(mavenBundle("org.slf4j", "slf4j-api").versionAsInProject());
        ret.add(mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject());
        ret.add(mavenBundle("ch.qos.logback", "logback-core").versionAsInProject());
        ret.add(mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject());
        ret.add(mavenBundle("org.openexi", "nagasena").versionAsInProject());

        ret.add(mavenBundle("io.netty", "netty-common").versionAsInProject());
        ret.add(mavenBundle("io.netty", "netty-buffer").versionAsInProject());
        ret.add(mavenBundle("io.netty", "netty-handler").versionAsInProject());
        ret.add(mavenBundle("io.netty", "netty-codec").versionAsInProject());
        ret.add(mavenBundle("io.netty", "netty-transport").versionAsInProject());

        return ret;
    }

    private Bundle getBundle(final String name) {
        final String bn = GROUP + "." + name;
        for (Bundle b : ctx.getBundles()) {
            if (bn.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }

    private void testBundle(final String name) {
        final Bundle b = getBundle(name);
        assertNotNull("Bundle '" + name + "' not found", b);
        assertEquals("Bundle '" + name + "' is not in ACTIVE state", Bundle.ACTIVE, b.getState());
    }

    @Configuration
    public final Option[] config() {
        final List<Option> options = coreBundles();

        for (final String s : prerequisiteBundles()) {
            options.add(mavenBundle(GROUP, s).versionAsInProject());
        }

        for (final String s : requiredBundles()) {
            options.add(mavenBundle(GROUP, s).versionAsInProject());
        }

        options.addAll(Arrays.asList(junitBundles()));
        options.add(systemPackages("sun.nio.ch"));
        return options.toArray(new Option[0]);
    }

    @Test
    public final void testBundleActivation() throws BundleException {
        for (final String s : requiredBundles()) {
            testBundle(s);
        }
    }
}
