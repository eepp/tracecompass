/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.SafeByteBufferFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

/**
 * Utility to serialize and deserialize states.
 *
 * @since 2.1
 */
@NonNullByDefault
public final class StateSystemSerializationUtils {

    private StateSystemSerializationUtils() {}

    /** File format version. Bump if the format changes */
    private static final int STATEDUMP_FORMAT_VERSION = 1;

    private static final String STATEDUMP_DIRECTORY = ".tc-states"; //$NON-NLS-1$
    private static final String FILE_SUFFIX = ".statedump"; //$NON-NLS-1$

    /**
     * Wrapper object representing a full query, along with its corresponding
     * attributes. It allows to reconstruct an initial state from scratch.
     */
    public static class Statedump {

        private final List<String[]> fAttributes;
        private final List<ITmfStateValue> fStates;
        private final int fStatedumpVersion;

        /**
         * Baseline constructor. Builds a statedump by passing elements
         * directly.
         *
         * @param attributes
         *            The list of state system attributes
         * @param states
         *            The corresponding states. The indices should match the
         *            previous list.
         * @param version
         *            Version of the statedump
         * @throws IllegalArgumentException
         *             If the two arrays don't have the same size
         */
        public Statedump(List<String[]> attributes, List<ITmfStateValue> states, int version) {
            if (attributes.size() != states.size()) {
                throw new IllegalArgumentException("Both lists should have the same number of elements"); //$NON-NLS-1$
            }
            fAttributes = ImmutableList.copyOf(attributes);
            fStates = ImmutableList.copyOf(states);
            fStatedumpVersion = version;
        }

        /**
         * "Online" constructor. Builds a statedump from a given state system
         * and timestamp.
         *
         * @param ss
         *            The state system for which to build the state dump
         * @param timestamp
         *            The timestamp at which to query the state to dump
         * @param version
         *            Version of the statedump
         */
        public Statedump(ITmfStateSystem ss, long timestamp, int version) {
            List<ITmfStateInterval> fullQuery;
            try {
                fullQuery = ss.queryFullState(timestamp);
            } catch (StateSystemDisposedException e1) {
                fAttributes = Collections.EMPTY_LIST;
                fStates = Collections.EMPTY_LIST;
                fStatedumpVersion = -1;
                return;
            }

            ImmutableList.Builder<String[]> attributesBuilder = ImmutableList.builder();
            for (int quark = 0; quark < ss.getNbAttributes(); quark++) {
                attributesBuilder.add(ss.getFullAttributePathArray(quark));
            }
            fAttributes = attributesBuilder.build();

            List<ITmfStateValue> states = fullQuery.stream()
                    .map(ITmfStateInterval::getStateValue)
                    .collect(Collectors.toList());
            fStates = ImmutableList.copyOf(states);

            fStatedumpVersion = version;
        }

        /**
         * Get the list of attributes of this state dump.
         *
         * @return The attributes
         */
        public List<String[]> getAttributes() {
            return fAttributes;
        }

        /**
         * Get the state values of this state dump.
         *
         * @return The state values
         */
        public List<ITmfStateValue> getStates() {
            return fStates;
        }

        /**
         * Get the version of this statedump. Can be used to consider if a
         * statedump should be read or not if the analysis changed since it was
         * written.
         *
         * @return The statedump's version
         */
        public int getVersion() {
            return fStatedumpVersion;
        }
    }

    private static final String SD_VERSION_KEY = "statedump-version"; //$NON-NLS-1$
    private static final String STATES_ARRAY_KEY = "states"; //$NON-NLS-1$

    private static final String ATTRIBUTE_KEY = "attribute"; //$NON-NLS-1$
    private static final String TYPE_KEY = "type"; //$NON-NLS-1$
    private static final String STATE_VALUE_KEY = "state-value"; //$NON-NLS-1$

    /**
     * Save a statedump at the given location.
     *
     * @param parentPath
     *            The location where to save the statedump file, usually in or
     *            close to its corresponding trace. It will be put under a Trace
     *            Compass-specific sub-directory.
     * @param ssid
     *            The state system ID of the state system we are saving. This
     *            will be used for restoration.
     * @param statedump
     *            The full state dump to save
     * @throws IOException
     *             If there are problems creating or writing to the target
     *             directory
     */
    public static void saveStatedump(Path parentPath, String ssid, Statedump statedump) throws IOException {
        Path sdPath = parentPath.resolve(STATEDUMP_DIRECTORY);
        if (!Files.exists(sdPath)) {
            Files.createDirectory(sdPath);
        }

        String fileName = ssid + FILE_SUFFIX;
        Path filePath = sdPath.resolve(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        Files.createFile(filePath);

        try (Writer bw = Files.newBufferedWriter(filePath, Charsets.UTF_8)) {
            JSONWriter writer = new JSONWriter(bw);
            writer.object()
                    .key(SD_VERSION_KEY)
                    .value(statedump.getVersion())
                    .endObject();

            writer.key(STATES_ARRAY_KEY);
            writer.array();
            for (int i = 0; i < statedump.getAttributes().size(); i++) {
                String[] attribute = statedump.getAttributes().get(i);
                ITmfStateValue sv = statedump.getStates().get(i);

                writer.object()
                        .key(ATTRIBUTE_KEY)
                        .value(Arrays.toString(attribute)); // kek

                writer.key(TYPE_KEY);
                switch (sv.getType()) {
                case CUSTOM:
                    writer.value("custom");

                    CustomStateValue customValue = (CustomStateValue) sv;

                    writer.key(STATE_VALUE_KEY);
                    writer.key("custom-value-type");
                    writer.value(customValue.getCustomTypeId());

                    /* Custom state values are serialized via a ByteBuffer */
                    int size = customValue.getSerializedSize();
                    ByteBuffer buffer = ByteBuffer.allocate(size);
                    buffer.clear();
                    ISafeByteBufferWriter sbbw = SafeByteBufferFactory.wrapWriter(buffer, size);
                    customValue.serialize(sbbw);
                    byte[] serializedValue = buffer.array();

                    String base64serializedValue = Base64.getEncoder().encodeToString(serializedValue);
                    writer.key(STATE_VALUE_KEY);
                    writer.value(base64serializedValue);
                    break;
                case DOUBLE:
                    writer.value("double");
                    writer.key(STATE_VALUE_KEY);
                    writer.value(sv.unboxDouble());
                    break;
                case INTEGER:
                    writer.value("int");
                    writer.key(STATE_VALUE_KEY);
                    writer.value(sv.unboxInt());
                    break;
                case LONG:
                    writer.value("long");
                    writer.key(STATE_VALUE_KEY);
                    writer.value(sv.unboxLong());
                    break;
                case NULL:
                    writer.value("null");
                    break;
                case STRING:
                    writer.value("string");
                    writer.key(STATE_VALUE_KEY);
                    writer.value(sv.unboxStr());
                    break;
                default:
                    writer.value("unknown");
                    writer.key(STATE_VALUE_KEY);
                    writer.value(sv.toString());
                    break;
                }

                writer.endObject();
            }
            writer.endArray();

            bw.flush();

        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Retrieve a previously-saved statedump.
     *
     * @param parentPath
     *            The expected location of the statedump file. Like the
     *            corresponding parameter in {@link #saveStatedump}, this is the
     *            parent path of the TC-specific subdirectory.
     * @param ssid
     *            The ID of the state system to retrieve
     * @return The corresponding de-serialized statedump. Returns null if there
     *         are no statedump for this state system ID (or no statedump
     *         directory at all).
     */
    public static @Nullable Statedump retrieveStatedump(Path parentPath, String ssid) {
        Path sdPath = parentPath.resolve(STATEDUMP_DIRECTORY);
        if (!Files.exists(sdPath)) {
            return null;
        }

        String fileName = ssid + FILE_SUFFIX;
        Path filePath = sdPath.resolve(fileName);
        if (!Files.exists(filePath)) {
            return null;
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(filePath, StandardOpenOption.READ))) {
            int formatVersion = in.readInt();
            if (formatVersion != STATEDUMP_FORMAT_VERSION) {
                return null;
            }
            Statedump statedump = (Statedump) in.readObject();
            return statedump;

        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

}