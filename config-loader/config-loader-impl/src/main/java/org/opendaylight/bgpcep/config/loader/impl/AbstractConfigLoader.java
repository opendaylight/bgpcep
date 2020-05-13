/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Reference implementation of configuration loading bits, without worrying where files are actually coming from.
 */
abstract class AbstractConfigLoader implements ConfigLoader {
    private final class PatternRegistration extends AbstractObjectRegistration<Pattern> {
        PatternRegistration(final Pattern pattern) {
            super(pattern);
        }

        @Override
        protected void removeRegistration() {
            unregister(this);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigLoader.class);
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    private static final String READ = "rw";
    private static final long TIMEOUT_SECONDS = 5;

    @GuardedBy("this")
    private final Map<PatternRegistration, ConfigFileProcessor> configServices = new HashMap<>();

    @Override
    public final synchronized AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        final String patternStr = INITIAL + config.getSchemaPath().getLastComponent().getLocalName() + EXTENSION;
        final Pattern pattern = Pattern.compile(patternStr);
        final PatternRegistration reg = new PatternRegistration(pattern);

        this.configServices.put(reg, config);

        final File[] fList = directory().listFiles();
        final List<String> newFiles = new ArrayList<>();
        if (fList != null) {
            for (final File newFile : fList) {
                if (newFile.isFile()) {
                    final String filename = newFile.getName();
                    if (pattern.matcher(filename).matches()) {
                        newFiles.add(filename);
                    }
                }
            }
        }
        for (final String filename : newFiles) {
            handleConfigFile(config, filename);
        }
        return reg;
    }

    final synchronized void handleEvent(final String filename) {
        configServices.entrySet().stream()
                .filter(entry -> entry.getKey().getInstance().matcher(filename).matches())
                .forEach(entry -> handleConfigFile(entry.getValue(), filename));
    }

    abstract @NonNull File directory();

    abstract @NonNull EffectiveModelContext modelContext();

    private synchronized void unregister(final PatternRegistration reg) {
        configServices.remove(reg);
    }

    private synchronized void handleConfigFile(final ConfigFileProcessor config, final String filename) {
        final NormalizedNode<?, ?> dto;
        try {
            dto = parseDefaultConfigFile(config, filename);
        } catch (final IOException | XMLStreamException e) {
            LOG.warn("Failed to parse config file {}", filename, e);
            return;
        }
        LOG.info("Loading initial config {}", filename);
        config.loadConfiguration(dto);
    }

    private NormalizedNode<?, ?> parseDefaultConfigFile(final ConfigFileProcessor config, final String filename)
            throws IOException, XMLStreamException {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        final File newFile = new File(directory(), filename);
        try (RandomAccessFile raf = new RandomAccessFile(newFile, READ)) {
            final FileChannel channel = raf.getChannel();

            FileLock lock = null;
            final Stopwatch stopwatch = Stopwatch.createStarted();
            while (lock == null || stopwatch.elapsed(TimeUnit.SECONDS) > TIMEOUT_SECONDS) {
                try {
                    lock = channel.tryLock();
                } catch (final IllegalStateException e) {
                    //Ignore
                }
                if (lock == null) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        LOG.warn("Failed to lock xml", e);
                    }
                }
            }

            try (InputStream resourceAsStream = new FileInputStream(newFile)) {
                final XMLInputFactory factory = XMLInputFactory.newInstance();
                final XMLStreamReader reader = factory.createXMLStreamReader(resourceAsStream);

                final EffectiveModelContext modelContext = modelContext();
                final SchemaNode schemaNode = SchemaContextUtil.findDataSchemaNode(modelContext(),
                    config.getSchemaPath());
                try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, modelContext, schemaNode)) {
                    xmlParser.parse(reader);
                } catch (final URISyntaxException | XMLStreamException | IOException | SAXException e) {
                    LOG.warn("Failed to parse xml", e);
                } finally {
                    reader.close();
                }
            }
        }

        return result.getResult();
    }
}
