/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
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

abstract class AbstractConfigLoader implements ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigLoader.class);
    private static final String INTERRUPTED = "InterruptedException";
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    private static final String READ = "rw";
    private static final long TIMEOUT_SECONDS = 5;

    @GuardedBy("this")
    private final Map<String, ConfigFileProcessor> configServices = new HashMap<>();
    private final String path;
    private final Thread watcherThread;
    private final File file;
    @GuardedBy("this")
    private boolean closed = false;

    AbstractConfigLoader(final FileWatcher fileWatcher) {
        this.path = requireNonNull(fileWatcher.getPathFile());
        this.file = new File(this.path);
        this.watcherThread = new Thread(new ConfigLoaderImplRunnable(requireNonNull(fileWatcher.getWatchService())));
    }

    final void start() {
        this.watcherThread.start();
        LOG.info("Config Loader service started");
    }

    final void stop() {
        LOG.info("Config Loader service stopping");

        synchronized (this) {
            this.closed = true;
            this.watcherThread.interrupt();
        }

        try {
            this.watcherThread.join();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for watcher thread to terminate", e);
        }
        LOG.info("Config Loader service stopped");
    }

    abstract @NonNull EffectiveModelContext modelContext();

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

        final File newFile = new File(this.path, filename);
        FileChannel channel = new RandomAccessFile(newFile, READ).getChannel();

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
            final SchemaNode schemaNode = SchemaContextUtil.findDataSchemaNode(modelContext(), config.getSchemaPath());
            try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, modelContext, schemaNode)) {
                xmlParser.parse(reader);
            } catch (final URISyntaxException | XMLStreamException | IOException | SAXException e) {
                LOG.warn("Failed to parse xml", e);
            } finally {
                reader.close();
                channel.close();
            }
        }

        return result.getResult();
    }

    @Override
    public final synchronized AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        final String pattern = INITIAL + config.getSchemaPath().getLastComponent().getLocalName() + EXTENSION;
        this.configServices.put(pattern, config);

        final File[] fList = this.file.listFiles();
        final List<String> newFiles = new ArrayList<>();
        if (fList != null) {
            for (final File newFile : fList) {
                if (newFile.isFile()) {
                    final String filename = newFile.getName();
                    if (Pattern.matches(pattern, filename)) {
                        newFiles.add(filename);
                    }
                }
            }
        }
        for (final String filename : newFiles) {
            handleConfigFile(config, filename);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (AbstractConfigLoader.this) {
                    AbstractConfigLoader.this.configServices.remove(pattern);
                }
            }
        };
    }

    private class ConfigLoaderImplRunnable implements Runnable {
        @GuardedBy("this")
        private final WatchService watchService;

        ConfigLoaderImplRunnable(final WatchService watchService) {
            this.watchService = watchService;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                handleChanges();
            }
        }

        private synchronized void handleChanges() {
            final WatchKey key;
            try {
                key = this.watchService.take();
            } catch (final InterruptedException | ClosedWatchServiceException e) {
                if (!AbstractConfigLoader.this.closed) {
                    LOG.warn(INTERRUPTED, e);
                    Thread.currentThread().interrupt();
                }
                return;
            }

            if (key != null) {
                final List<String> fileNames = key.pollEvents()
                        .stream().map(event -> event.context().toString())
                        .collect(Collectors.toList());
                fileNames.forEach(this::handleEvent);

                final boolean reset = key.reset();
                if (!reset) {
                    LOG.warn("Could not reset the watch key.");
                }
            }
        }

        private synchronized void handleEvent(final String filename) {
            AbstractConfigLoader.this.configServices.entrySet().stream()
                    .filter(entry -> Pattern.matches(entry.getKey(), filename))
                    .forEach(entry -> handleConfigFile(entry.getValue(), filename));
        }
    }
}
