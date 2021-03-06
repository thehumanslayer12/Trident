/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2017 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tridentsdk.server.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.tridentsdk.config.ConfigSection;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implementation of a configuration section
 */
@ThreadSafe
public class TridentConfigSection implements ConfigSection {
    /**
     * The string literal for the separator
     */
    private static final String SEPARATOR_LITERAL = ".";
    /**
     * The separator which splits section name keys
     *
     * <p>Use this for regex splits only</p>
     */
    private static final String SECTION_SEPARATOR = Pattern.quote(SEPARATOR_LITERAL);

    /**
     * The elements of the the configuration file
     */
    private final ConcurrentLinkedStringMap<Object> elements = new ConcurrentLinkedStringMap<>();

    /**
     * Writes the config section and all of the associated
     * children to a new json object.
     *
     * @return this section and all its children's json
     */
    public JsonObject write() {
        JsonObject object = new JsonObject();

        // funky code because we need to make sure all the
        // elements remain in insertion order
        this.elements.forEach((k, v) -> {
            if (v instanceof ConfigSection) {
                TridentConfigSection section = (TridentConfigSection) v;
                object.add(k, section.write());
            } else {
                object.add(k, ConfigIo.asJson(v));
            }
        });
        return object;
    }

    /**
     * Loads the json from file into memory
     *
     * @param object the file json
     */
    public void read(JsonObject object) {
        object.entrySet().forEach(e -> {
            String key = e.getKey();
            JsonElement value = e.getValue();

            // special handling for json objects which are
            // config sections
            if (value.isJsonObject()) {
                TridentConfigSection section = this.createChild0(key, object);
                section.read(value.getAsJsonObject());
            } else {
                this.elements.put(key, ConfigIo.asObj(value, TridentAdapter.class));
            }
        });
    }

    /**
     * The name of this config section (empty if root)
     */
    private final String name;
    /**
     * The parent config section, or null if root
     */
    private final ConfigSection parent;
    /**
     * The root config section, or null if root
     */
    private final ConfigSection root;

    /**
     * Creates a new config section.
     *
     * @param name the name of the new config section
     * @param parent the parent of the child section
     * @param root the root section
     */
    public TridentConfigSection(String name, ConfigSection parent, ConfigSection root) {
        this.name = name;
        this.parent = parent;
        this.root = root;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ConfigSection getRoot() {
        return this.root;
    }

    @Override
    public ConfigSection getParent() {
        return this.parent;
    }

    @Override
    public ConfigSection createChild(String key) {
        String[] split = key.split(SECTION_SEPARATOR);

        TridentConfigSection section = this;
        if (split.length > 0) {
            for (String aSplit : split) {
                section = section.createChild0(aSplit, null);
            }
        }

        return section;
    }

    @Nonnull
    @Override
    public ConfigSection getChild(String key) {
        return this.findSection(key.split(SECTION_SEPARATOR), false);
    }

    @Override
    public boolean removeChild(String key) {
        String[] split = key.split(SECTION_SEPARATOR);
        String finalKey = split.length > 0 ? split[split.length - 1] : key;

        TridentConfigSection parent = this.findSection(split, true);
        return parent.elements.remove(finalKey) != null;
    }

    @Override
    public Stream<ConfigSection> getChildren(boolean deep) {
        Set<ConfigSection> set = new LinkedHashSet<>();
        this.children0(set, deep);
        return Collections.unmodifiableSet(set).stream();
    }

    @Override
    public Stream<String> getKeys(boolean deep) {
        Set<String> set = new LinkedHashSet<>();
        this.iterate("", set, (s, e) -> this.handlePath(s, e.getKey()), deep);
        return Collections.unmodifiableSet(set).stream();
    }

    @Override
    public Stream<Object> getValues(boolean deep) {
        LinkedList<Object> list = new LinkedList<>();
        this.iterate("", list, (s, e) -> e.getValue(), deep);
        return Collections.unmodifiableCollection(list).stream();
    }

    @Override
    public Stream<Map.Entry<String, Object>> getEntries(boolean deep) {
        Set<Map.Entry<String, Object>> set = new LinkedHashSet<>();
        this.iterate("", set, this::concatKey, deep);
        return Collections.unmodifiableSet(set).stream();
    }

    @Override
    public Object get(String key) {
        return this.getElement(key);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object element = this.getElement(key);
        if (!(element instanceof ConfigSection)) {
            return (T) element;
        }

        throw new IllegalArgumentException(key + " is a config section");
    }

    @Override
    public void set(String key, Object value) {
        String[] split = key.split(SECTION_SEPARATOR);
        String finalKey = key;

        TridentConfigSection section = this;
        if (split.length > 0) {
            finalKey = split[split.length - 1];
            for (int i = 0; i < split.length - 1; i++) {
                section = (TridentConfigSection) section.createChild(split[i]);
            }
        }

        section.elements.put(finalKey, value);
    }

    @Override
    public boolean remove(String key) {
        String[] split = key.split(SECTION_SEPARATOR);
        String finalKey = split.length == 0 ? key : split[split.length - 1];
        TridentConfigSection section = this.findSection(split, true);
        return section.elements.remove(finalKey) != null;
    }

    @Override
    public boolean hasKey(String key) {
        return this.elements.containsKey(key);
    }

    @Override
    public int getInt(String key) {
        return this.get(key, Number.class).intValue();
    }

    @Override
    public void setInt(String key, int value) {
        this.set(key, value);
    }

    @Override
    public short getShort(String key) {
        return this.get(key, Number.class).shortValue();
    }

    @Override
    public void setShort(String key, short value) {
        this.set(key, value);
    }

    @Override
    public long getLong(String key) {
        return this.get(key, Number.class).longValue();
    }

    @Override
    public void setLong(String key, long value) {
        this.set(key, value);
    }

    @Override
    public byte getByte(String key) {
        return this.get(key, Number.class).byteValue();
    }

    @Override
    public void setByte(String key, byte value) {
        this.set(key, value);
    }

    @Override
    public float getFloat(String key) {
        return this.get(key, Number.class).floatValue();
    }

    @Override
    public void setFloat(String key, float value) {
        this.set(key, value);
    }

    @Override
    public double getDouble(String key) {
        return this.get(key, Number.class).doubleValue();
    }

    @Override
    public void setDouble(String key, double value) {
        this.set(key, value);
    }

    @Override
    public char getChar(String key) {
        return (char) this.getElement(key);
    }

    @Override
    public void setChar(String key, char value) {
        this.set(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return (boolean) this.getElement(key);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        this.set(key, value);
    }

    @Nonnull
    @Override
    public String getString(String key) {
        return (String) this.getElement(key);
    }

    @Override
    public void setString(String key, String value) {
        this.set(key, value);
    }

    @Override
    public <T, C extends Collection<T>> void getCollection(String key, C collection) {
        Object o = this.getElement(key);
        if (o instanceof Collection) {
            collection.addAll((Collection) o);
            return;
        }

        throw new NoSuchElementException(String.format("%s is not a collection (%s)", key, o.getClass()));
    }

    // The following two methods are necessary in order to
    // prevent recursing over the same method which creates
    // a collection each time
    // Instead, only the first collection will collect all
    // of the elements instead of creating a new collection
    // each time and copying

    /**
     * Gets all the children config sections.
     *
     * @param col the collection to append
     * @param deep {@code true} to get the children
     * children
     */
    private void children0(Collection<ConfigSection> col, boolean deep) {
        this.elements.values().stream()
                .filter(o -> o instanceof ConfigSection)
                .map(o -> (TridentConfigSection) o)
                .forEach(cs -> {
                    if (deep) {
                        cs.children0(col, true);
                    }
                    col.add(cs);
                });
    }

    /**
     * Iterates over the elements in this config section,
     * performing the given operations in order to append
     * the elements to the given collection.
     *
     * @param base the base string key
     * @param col the collection to append entries
     * @param function extracts the entry value
     * @param deep {@code true} to get children elements
     * @param <T> the type appended to the collection
     */
    private <T> void iterate(String base, Collection<T> col, BiFunction<String, Map.Entry<String, Object>, T> function, boolean deep) {
        this.elements.entrySet().forEach(e -> {
                    Object val = e.getValue();
                    if (deep) {
                        if (val instanceof ConfigSection) {
                            TridentConfigSection section = (TridentConfigSection) val;
                            section.iterate(this.handlePath(base, section.name), col, function, true);
                            return;
                        }
                    }

                    col.add(function.apply(base, e));
                });
    }

    /**
     * Internal child config section creation method,
     * useful for bypassing the . key based key creation
     * when we know that the name is not . separated.
     *
     * @param name the name of the new child
     * @param object the json object that makes the
     * section,
     * or {@code null} if it is just created
     * @return the created section
     */
    private TridentConfigSection createChild0(String name, JsonObject object) {
        TridentConfigSection section = new TridentConfigSection(name, this, this.getRoot());
        this.elements.put(name, section);
        return section;
    }

    /**
     * Handles whether or not to append the current path to
     * the base path for relative keys.
     *
     * @param path the base path
     * @param cur the current path
     * @return the path
     */
    private String handlePath(String path, String cur) {
        if (path.isEmpty()) return cur;
        return path + SEPARATOR_LITERAL + cur;
    }

    /**
     * Obtains a config section with a key split with .
     *
     * @param split the split key
     * @param hasValue {@code true} if the key contains a
     * value, {@code false} if the key contains only config
     * sections
     * @return the config section
     */
    @Nonnull
    private TridentConfigSection findSection(String[] split, boolean hasValue) {
        TridentConfigSection section = this;
        if (split.length > 1) {
            for (int i = 0; i < (hasValue ? split.length - 1 : split.length); i++) {
                String sectionName = split[i];
                Object o = section.elements.get(sectionName);
                if (!(o instanceof ConfigSection)) {
                    throw new NoSuchElementException(String.format("Section \"%s\" cannot be found in \"%s\"", sectionName, Arrays.toString(split)));
                }

                section = (TridentConfigSection) o;
            }
        } else if (!hasValue) {
            return (TridentConfigSection) this.elements.get(split[0]);
        }

        return section;
    }

    /**
     * Obtains the element given the . split key
     *
     * @param key the key at which to find the element
     * @return the element
     */
    @Nonnull
    private Object getElement(String key) {
        String[] split = key.split(SECTION_SEPARATOR);
        String finalKey = key;

        TridentConfigSection section = this.findSection(split, true);
        if (section != this) {
            finalKey = split[split.length - 1];
        }

        // if all goes well, we have the final key at the
        // last element
        // try to get the value from the last child section
        // before the final key
        // if null, throw serverException
        Object element = section.elements.get(finalKey);
        if (element == null) {
            throw new NoSuchElementException(String.format("Key \"%s\" in your key \"%s\" cannot be found", finalKey, key));
        }

        return element;
    }

    /**
     * Concats the key to the given string from the given
     * entry.
     *
     * @param s the base string
     * @param entry the entry to concat
     * @return the entry with the concatenated key
     */
    private Map.Entry<String, Object> concatKey(String s, Map.Entry<String, Object> entry) {
        return new Map.Entry<String, Object>() {
            @Override
            public String getKey() {
                return TridentConfigSection.this.handlePath(s, entry.getKey());
            }

            @Override
            public Object getValue() {
                return entry.getValue();
            }

            @Override
            public Object setValue(Object value) {
                return entry.setValue(value);
            }

            @Override
            public boolean equals(Object o) {
                return entry.equals(o);
            }

            @Override
            public int hashCode() {
                return entry.hashCode();
            }
        };
    }
}
