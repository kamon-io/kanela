package kamon.agent.utils;

import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.util.Closer;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class PluginCacheUtils {
    private final Map<String, Map<String, PluginEntry>> categories =
            new LinkedHashMap<>();

    /**
     * Returns all categories of plugins in this cache.
     *
     * @return all categories of plugins in this cache.
     * @since 2.1
     */
    public Map<String, Map<String, PluginEntry>> getAllCategories() {
        return categories;
    }

    /**
     * Gets or creates a category of plugins.
     *
     * @param category name of category to look up.
     * @return plugin mapping of names to plugin entries.
     */
    public Map<String, PluginEntry> getCategory(final String category) {
        final String key = category.toLowerCase();
        if (!categories.containsKey(key)) {
            categories.put(key, new LinkedHashMap<String, PluginEntry>());
        }
        return categories.get(key);
    }

    /**
     * Stores the plugin cache to a given OutputStream.
     *
     * @param os destination to save cache to.
     * @throws IOException
     */
    // NOTE: if this file format is to be changed, the filename should change and this format should still be readable
    public void writeCache(final OutputStream os) throws IOException {
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os));
        try {
            // See PluginManager.readFromCacheFiles for the corresponding decoder. Format may not be changed
            // without breaking existing Log4j2Plugins.dat files.
            out.writeInt(categories.size());
            for (final Map.Entry<String, Map<String, PluginEntry>> category : categories.entrySet()) {
                out.writeUTF(category.getKey());
                final Map<String, PluginEntry> m = category.getValue();
                out.writeInt(m.size());
                for (final Map.Entry<String, PluginEntry> entry : m.entrySet()) {
                    final PluginEntry plugin = entry.getValue();
                    out.writeUTF(plugin.getKey());
                    out.writeUTF(plugin.getClassName());
                    out.writeUTF(plugin.getName());
                    out.writeBoolean(plugin.isPrintable());
                    out.writeBoolean(plugin.isDefer());
                }
            }
        } finally {
            Closer.closeSilently(out);
        }
    }

    /**
     * Loads and merges all the Log4j plugin cache files specified. Usually, this is obtained via a ClassLoader.
     *
     * @param resources URLs to all the desired plugin cache files to load.
     * @throws IOException
     */

    public void loadCacheFiles(final Enumeration<URL> resources) throws IOException {
        categories.clear();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            final DataInputStream in = new DataInputStream(new BufferedInputStream(url.openStream()));
            try {
                final int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    final String category = in.readUTF();
                    final Map<String, PluginEntry> m = getCategory(category);
                    final int entries = in.readInt();
                    for (int j = 0; j < entries; j++) {
                        final PluginEntry entry = new PluginEntry();
                        entry.setKey(in.readUTF());
                        String className = in.readUTF();
                        if(className.contains("kamon.")) {
                            entry.setClassName(className);
                            System.out.println(className);
                        }else {
                            entry.setClassName("kamon.agent.libs." + className);
                            System.out.println("kamon.agent.libs."+ className);

                        }
                        entry.setName(in.readUTF());
                        entry.setPrintable(in.readBoolean());
                        entry.setDefer(in.readBoolean());
                        entry.setCategory(category);
                        if (!m.containsKey(entry.getKey())) {
                            m.put(entry.getKey(), entry);
                        }
                    }
                }
            } finally {
                Closer.closeSilently(in);
            }
        }
    }

    /**
     * Gets the number of plugin categories registered.
     *
     * @return number of plugin categories in cache.
     */
    public int size() {
        return categories.size();
    }}
