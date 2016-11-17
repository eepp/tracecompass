/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.SafeByteBufferFactory;
import org.eclipse.tracecompass.internal.statesystem.core.Activator;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String FILE_SUFFIX = ".statedump.json"; //$NON-NLS-1$

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

    /*
    private static final String SD_VERSION_KEY = "statedump-version"; //$NON-NLS-1$
    private static final String STATES_ARRAY_KEY = "states"; //$NON-NLS-1$

    private static final String ATTRIBUTE_KEY = "attribute"; //$NON-NLS-1$
    private static final String STATE_VALUE_KEY = "state-value"; //$NON-NLS-1$
    */

    private static final String VALUE_KEY = "value"; //$NON-NLS-1$
    private static final String TYPE_KEY = "type"; //$NON-NLS-1$
    private static final String CHILDREN_KEY = "children"; //$NON-NLS-1$
    private static final String STATE_KEY = "state"; //$NON-NLS-1$
    private static final String ID_KEY = "id"; //$NON-NLS-1$
    private static final String VERSION_KEY = "version"; //$NON-NLS-1$
    private static final String CUSTOM_TYPE = "custom"; //$NON-NLS-1$
    private static final String DOUBLE_TYPE = "double"; //$NON-NLS-1$
    private static final String INT_TYPE = "int"; //$NON-NLS-1$
    private static final String LONG_TYPE = "long"; //$NON-NLS-1$
    private static final String NULL_TYPE = "null"; //$NON-NLS-1$
    private static final String STRING_TYPE = "string"; //$NON-NLS-1$
    private static final String UNKNOWN_TYPE = "unknown"; //$NON-NLS-1$
    private static final String DOUBLE_NAN = "nan"; //$NON-NLS-1$
    private static final String DOUBLE_POS_INF = "+inf"; //$NON-NLS-1$
    private static final String DOUBLE_NEG_INF = "-inf"; //$NON-NLS-1$

    private static void insertStateValueInStateNode(JSONObject stateNode, ITmfStateValue stateValue) throws JSONException {
        Object type = null;
        Object value = null;

        switch (stateValue.getType()) {
        case CUSTOM:
            type = CUSTOM_TYPE;
            CustomStateValue customValue = (CustomStateValue) stateValue;
            stateNode.put(ID_KEY, customValue.getCustomTypeId());

            /* Custom state values are serialized via a ByteBuffer */
            int size = customValue.getSerializedSize();
            ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.clear();
            ISafeByteBufferWriter sbbw = SafeByteBufferFactory.wrapWriter(buffer, size);
            customValue.serialize(sbbw);
            byte[] serializedValue = buffer.array();
            value = Base64.getEncoder().encodeToString(serializedValue);
            break;
        case DOUBLE:
            type = DOUBLE_TYPE;
            double doubleValue = stateValue.unboxDouble();

            if (Double.isNaN(doubleValue)) {
                value = DOUBLE_NAN;
            } else if (Double.isInfinite(doubleValue)) {
                if (doubleValue < 0) {
                    value = DOUBLE_NEG_INF;
                } else {
                    value = DOUBLE_POS_INF;
                }
            }

            value = doubleValue;
            break;
        case INTEGER:
            type = INT_TYPE;
            value = stateValue.unboxInt();
            break;
        case LONG:
            type = LONG_TYPE;
            value = stateValue.unboxLong();
            break;
        case NULL:
            type = NULL_TYPE;
            value = JSONObject.NULL;
            break;
        case STRING:
            type = STRING_TYPE;
            value = stateValue.unboxStr();
            break;
        default:
            type = UNKNOWN_TYPE;
            value = stateValue.toString();
            break;
        }

        assert(type != null);
        assert(value != null);
        stateNode.put(TYPE_KEY, type);
        stateNode.put(VALUE_KEY, value);
    }

    private static void insertFrom(JSONObject stateNode, String[] attr, int attrIndex, ITmfStateValue stateValue) throws JSONException {
        assert(attr.length > 0);
        assert(stateNode.has(CHILDREN_KEY));

        JSONObject nodeChildren = stateNode.getJSONObject(CHILDREN_KEY);
        String curAttrElement = attr[attrIndex];

        if (!nodeChildren.has(curAttrElement)) {
            JSONObject newNode = new JSONObject();
            newNode.put(CHILDREN_KEY, new JSONObject());
            nodeChildren.put(curAttrElement, newNode);
        }

        JSONObject nearestChild = nodeChildren.getJSONObject(curAttrElement);

        if (attrIndex == attr.length - 1) {
            // end of recursion!
            insertStateValueInStateNode(nearestChild, stateValue);
            return;
        }

        insertFrom(nearestChild, attr, attrIndex + 1, stateValue);
    }

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
     * @throws
     */
    public static void dumpState(Path parentPath, String ssid, Statedump statedump) throws IOException {
        // Create directory if it does not exist
        Path sdPath = parentPath.resolve(STATEDUMP_DIRECTORY);
        if (!Files.exists(sdPath)) {
            Files.createDirectory(sdPath);
        }

        // Create state dump file
        String fileName = ssid + FILE_SUFFIX;
        Path filePath = sdPath.resolve(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        Files.createFile(filePath);

        JSONObject root = new JSONObject();

        try {
            // Create the root object
            root.put(ID_KEY, ssid);
            root.put(VERSION_KEY, statedump.getVersion());

            // Create the root state node
            JSONObject rootNode = new JSONObject();
            rootNode.put(CHILDREN_KEY, new JSONObject());
            root.put(STATE_KEY, rootNode);

            // Insert all the paths, types, and values
            for (int i = 0; i < statedump.getAttributes().size(); i++) {
                String[] attribute = statedump.getAttributes().get(i);
                ITmfStateValue sv = statedump.getStates().get(i);

                insertFrom(rootNode, attribute, 0, sv);
            }
        } catch (JSONException e) {
            /*
             * This should never happen. Any JSON exception means
             * that there's a bug in this code.
             */
            throw new IllegalStateException(e);
        }

        try (Writer bw = Files.newBufferedWriter(filePath, Charsets.UTF_8)) {
            bw.write(root.toString());
        }
    }

    private static @Nullable ITmfStateValue stateValueFromJsonObject(JSONObject node) {
        String type;
        ITmfStateValue stateValue = null;

        try {
            type = node.getString(TYPE_KEY);
        } catch (JSONException e) {
            Activator.getDefault().logWarning(String.format("Missing \"%s\" property in state node object", TYPE_KEY)); //$NON-NLS-1$
            return null;
        }

        switch (type) {
        case NULL_TYPE:
            stateValue = TmfStateValue.nullValue();
            break;

        case INT_TYPE:
        case LONG_TYPE:
            long longValue;

            try {
                longValue = node.getLong(VALUE_KEY);
            } catch (JSONException e) {
                Activator.getDefault().logWarning(String.format("Invalid or missing \"%s\" property (expecting a number) in state node object", VALUE_KEY)); //$NON-NLS-1$
                return null;
            }

            if (type == INT_TYPE) {
                stateValue = TmfStateValue.newValueInt((int) longValue);
            } else {
                stateValue = TmfStateValue.newValueLong(longValue);
            }
            break;

        case DOUBLE_TYPE:
            Double doubleValue = null;
            Object nodeValue;

            try {
                nodeValue = node.get(VALUE_KEY);
            } catch (JSONException e) {
                Activator.getDefault().logWarning(String.format("Missing \"%s\" property in state node object", VALUE_KEY)); //$NON-NLS-1$
                return null;
            }

            if (nodeValue instanceof Double) {
                doubleValue = (Double) nodeValue;
            }

            if (nodeValue instanceof String) {
                String strValue = (String) nodeValue;

                if (strValue.equals(DOUBLE_NAN)) {
                    doubleValue = Double.NaN;
                } else if (strValue.equals(DOUBLE_NEG_INF)) {
                    doubleValue = Double.NEGATIVE_INFINITY;
                } else if (strValue.equals(DOUBLE_POS_INF)) {
                    doubleValue = Double.POSITIVE_INFINITY;
                }
            }

            if (doubleValue == null) {
                Activator.getDefault().logWarning(String.format("Invalid \"%s\" property in state node object", VALUE_KEY)); //$NON-NLS-1$
                return null;
            }

            stateValue = TmfStateValue.newValueDouble(doubleValue);
            break;

        case STRING_TYPE:
        case UNKNOWN_TYPE:
            String stringValue;

            try {
                stringValue = node.getString(VALUE_KEY);
            } catch (JSONException e) {
                Activator.getDefault().logWarning(String.format("Invalid or missing \"%s\" property (expecting a string) in state node object", VALUE_KEY)); //$NON-NLS-1$
                return null;
            }

            stateValue = TmfStateValue.newValueString(stringValue);
            break;

        case CUSTOM_TYPE:
            // TODO
            break;

        default:
            Activator.getDefault().logWarning(String.format("Unknown \"%s\" property (\"%s\") in state node object", TYPE_KEY, type)); //$NON-NLS-1$
            return null;
        }

        assert(stateValue != null);

        return stateValue;
    }

    private static boolean visitStateNode(JSONObject stateNode, List<String> attrStack,
            List<String[]> attributes, List<ITmfStateValue> values) {
        ITmfStateValue stateValue = null;
        String[] attribute = (String[]) attrStack.toArray();

        // Ignore if it's the root node
        if (attribute.length > 0) {
            stateValue = stateValueFromJsonObject(stateNode);

            if (stateValue == null) {
                Activator.getDefault().logWarning(String.format("Cannot rebuild state value for attribute \"%s\"", Arrays.toString(attribute))); //$NON-NLS-1$
                return false;
            }

            // Insert at the same position
            attributes.add(attribute);
            values.add(stateValue);
        }

        if (stateNode.has(CHILDREN_KEY)) {
            JSONObject childrenNode;

            try {
                childrenNode = stateNode.getJSONObject(CHILDREN_KEY);
            } catch (JSONException e) {
                Activator.getDefault().logWarning(String.format("At attribute \"%s\": expecting an object for the \"%s\" property", //$NON-NLS-1$
                        Arrays.toString(attribute), CHILDREN_KEY));
                return false;
            }

            Iterator<String> keyIt = childrenNode.keys();

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                JSONObject childStateNode;

                try {
                    childStateNode = childrenNode.getJSONObject(key);
                } catch (JSONException e) {
                    Activator.getDefault().logWarning(String.format("At attribute \"%s\": in \"%s\" node: expecting an object for the \"%s\" property", //$NON-NLS-1$
                            Arrays.toString(attribute), CHILDREN_KEY, key));
                    return false;
                }

                attrStack.add(key);

                if (!visitStateNode(childStateNode, attrStack, attributes, values)) {
                    Activator.getDefault().logWarning(String.format("At attribute \"%s\": in \"%s\" node: failed to visit the \"%s\" property", //$NON-NLS-1$
                            Arrays.toString(attribute), CHILDREN_KEY, key));
                    return false;
                }

                attrStack.remove(attrStack.size() - 1);
            }
        }

        return true;
    }

    private static @Nullable Statedump stateDumpFromJsonObject(JSONObject root, String expectSsid) {
        List<String[]> attributes = new ArrayList<>();
        List<ITmfStateValue> values = new ArrayList<>();
        String ssid;
        int version;
        JSONObject rootStateNode;

        // Read state system ID property
        try {
            ssid = root.getString(ID_KEY);
        } catch (JSONException e) {
            Activator.getDefault().logWarning(String.format("Missing \"%s\" property in state dump (root) object", ID_KEY)); //$NON-NLS-1$
            return null;
        }

        // Check that the state system ID matches the expected one
        if (!expectSsid.equals(ssid)) {
            Activator.getDefault().logWarning(String.format("State system ID mismatch: expecting \"%s\", got \"%s\"", expectSsid, ssid)); //$NON-NLS-1$
            return null;
        }

        // Read version property
        try {
            version = root.getInt(VERSION_KEY);
        } catch (JSONException e) {
            Activator.getDefault().logWarning(String.format("Missing \"%s\" property in state dump (root) object", VERSION_KEY)); //$NON-NLS-1$
            return null;
        }

        // Read state property (root state node)
        try {
            rootStateNode = root.getJSONObject(STATE_KEY);

            // For type safety: should never happen
            if (rootStateNode == null) {
                return null;
            }
        } catch (JSONException e) {
            Activator.getDefault().logWarning(String.format("Missing \"%s\" property in state dump (root) object", STATE_KEY)); //$NON-NLS-1$
            return null;
        }

        if (!visitStateNode(rootStateNode, new ArrayList<String>(), attributes, values)) {
            Activator.getDefault().logWarning("Failed to visit the root state node object"); //$NON-NLS-1$
            return null;
        }

        return new Statedump(attributes, values, version);
    }

    /**
     * Retrieve a previously-saved statedump.
     *
     * @param parentPath
     *            The expected location of the statedump file. Like the
     *            corresponding parameter in {@link #dumpState}, this is the
     *            parent path of the TC-specific subdirectory.
     * @param ssid
     *            The ID of the state system to retrieve
     * @return The corresponding de-serialized statedump. Returns null if there
     *         are no statedump for this state system ID (or no statedump
     *         directory at all).
     */
    public static @Nullable Statedump loadState(Path parentPath, String ssid) {
        // Find the state dump directory
        Path sdPath = parentPath.resolve(STATEDUMP_DIRECTORY);
        if (!Files.isDirectory(sdPath)) {
            Activator.getDefault().logWarning(String.format("\"%s\" is not an existing directory", sdPath)); //$NON-NLS-1$
            return null;
        }

        // Find the state dump file
        String fileName = ssid + FILE_SUFFIX;
        Path filePath = sdPath.resolve(fileName);
        if (!Files.exists(filePath)) {
            Activator.getDefault().logWarning(String.format("\"%s\" is not an existing state dump file", filePath)); //$NON-NLS-1$
            return null;
        }

        try (InputStreamReader in = new InputStreamReader(Files.newInputStream(filePath, StandardOpenOption.READ))) {
            BufferedReader bufReader = new BufferedReader(in);
            String json = bufReader.lines().collect(Collectors.joining("\n")); //$NON-NLS-1$
            JSONObject root = new JSONObject(json);

            return stateDumpFromJsonObject(root, ssid);
        } catch (IOException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

}
