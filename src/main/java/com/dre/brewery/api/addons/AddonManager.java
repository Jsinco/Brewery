/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.Logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

/**
 * Yep, you guessed it. This is the class that manages all the addons. It loads them, unloads them, reloads them, and keeps track of them.
 * <p>
 * Kind of similar to how the PluginManager in Bukkit works, but for addons.
 *
 * @see BreweryAddon
 * @see AddonInfo
 * @see AddonLogger
 * @see AddonFileManager
 * @see AddonConfigManager
 */
public class AddonManager {

    public static final ConcurrentLinkedQueue<BreweryAddon> LOADED_ADDONS = new ConcurrentLinkedQueue<>();

    private final BreweryPlugin plugin;
    private final File addonsFolder;

    public AddonManager(BreweryPlugin plugin) {
        this.plugin = plugin;
        this.addonsFolder = new File(plugin.getDataFolder(), "addons");
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }
    }

    public void unloadAddons() {
        if (LOADED_ADDONS.isEmpty()) {
            return;
        }

        Logging.log("Disabling " + LOADED_ADDONS.size() + " loaded addon(s)...");
        for (BreweryAddon addon : LOADED_ADDONS) {
            unloadAddon(addon);
        }
    }

    public void unloadAddon(BreweryAddon addon) {
        if (addon == null) {
            Logging.warningLog("Tried to unload an addon that doesn't exist.");
            return;
        }
        String n = addon.getAddonInfo() == null ? addon.getClass().getSimpleName() : addon.getAddonInfo().name();
        try {
            addon.onAddonDisable();
        } catch (Throwable t) {
            Logging.errorLog("Failed to disable addon " + n, t);
        }
        try {
            addon.unregisterListeners();
            addon.unregisterCommands();
        } catch (Throwable t) {
            Logging.errorLog("Failed to unregister listeners/commands for addon " + n, t);
        }
        try {
            Field field = BreweryAddon.class.getDeclaredField("classLoader");
            field.setAccessible(true);
            URLClassLoader classLoader = (URLClassLoader) field.get(addon);
            classLoader.close();
            field.set(addon, null);
            LOADED_ADDONS.remove(addon);
        } catch (Throwable t) {
            Logging.errorLog("Failed to unload addon " + n, t);
            Logging.warningLog("Addon " + n + "'s ClassLoader has not been closed properly!");
        }
    }


    public void reloadAddons() {
        for (BreweryAddon addon : LOADED_ADDONS) {
            try {
                addon.onBreweryReload();
            } catch (Throwable t) {
                Logging.errorLog("Failed to reload addon " + addon.getClass().getSimpleName(), t);
            }
        }
        int loaded = LOADED_ADDONS.size();
        if (loaded > 0) Logging.log("Reloaded " + loaded + " addon(s)");
    }

    public ConcurrentLinkedQueue<BreweryAddon> getAddons() {
        return LOADED_ADDONS;
    }


    public void loadAddons() {
        File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar")); // Get all files in the addons folder that end with .jar
        if (files == null) {
            return;
        }

        for (File file : files) {
            loadAddon(file); // Go read the documentation below to understand what this does.
        }

        int loaded = LOADED_ADDONS.size();
        if (loaded > 0) Logging.log("Loaded " + loaded + " addon(s)");
    }

    public void enableAddons() {
        for (BreweryAddon addon : LOADED_ADDONS) {
            try {
                addon.onAddonEnable(); // All done, let the addon know it's been enabled.
            } catch (Throwable t) {
                Logging.errorLog("Failed to enable addon " + addon.getAddonInfo().name(), t);
                unloadAddon(addon);
            }
        }
    }


    /**
     * Load the addon from a jar file.
     * onAddonPreEnable() will be called after the addon is loaded.
     *
     * @param file The jar file to load the addon from
     */
    public void loadAddon(File file) {
        try {

            // We have to use the same class loader used to load this class AKA, the 'PluginLoader' class provided by Bukkit.
            // Only classes loaded by the same ClassLoader can access each other.
            // So to prevent any issues,
            // we're using the same ClassLoader that loaded this class to load the classes from the jar.
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ file.toURI().toURL() },
                this.getClass().getClassLoader() // <-- PluginClassLoader
            );
            var mainClass = getMainClass(file, classLoader); // Get all our loaded classes.

            BreweryAddon addon;
            try {
                addon = mainClass.getConstructor().newInstance(); // Instantiate our main class, the class shouldn't have constructor args.
            } catch (Exception e) {
                Logging.errorLog("Failed to load addon: " + file.getName(), e);
                return;
            }
            try {

                // Set the logger and file manager

                Field infoField = BreweryAddon.class.getDeclaredField("addonInfo");
                infoField.setAccessible(true);
                infoField.set(addon, mainClass.getAnnotation(AddonInfo.class));

                if (addon.getAddonInfo() == null) { // This CAN be null for us. It's only annotated NotNull for addons.
                    Logging.errorLog("Addon " + mainClass.getSimpleName() + " is missing the AddonInfo annotation. It will not be loaded.");
                    return;
                }

                // Set all the fields for our addon reflectively.

                Field classLoaderField = BreweryAddon.class.getDeclaredField("classLoader");
                classLoaderField.setAccessible(true);
                Field loggerField = BreweryAddon.class.getDeclaredField("logger");
                loggerField.setAccessible(true);
                Field fileManagerField = BreweryAddon.class.getDeclaredField("addonFileManager");
                fileManagerField.setAccessible(true);
                Field addonConfigManagerField = BreweryAddon.class.getDeclaredField("addonConfigManager");
                addonConfigManagerField.setAccessible(true);
                Field addonFile = BreweryAddon.class.getDeclaredField("addonFile");
                addonFile.setAccessible(true);

                classLoaderField.set(addon, classLoader);
                loggerField.set(addon, new AddonLogger(addon.getAddonInfo()));
                fileManagerField.set(addon, new AddonFileManager(addon, file));
                addonConfigManagerField.set(addon, new AddonConfigManager(addon));
                addonFile.set(addon, file);


                addon.getAddonLogger().info("Loading &a" + addon.getAddonInfo().name() + " &f-&a v" + addon.getAddonInfo().version() + " &fby &a" + addon.getAddonInfo().author());
                LOADED_ADDONS.add(addon); // Add to our list of addons

                // let the addon know it has been loaded, it can do some pre-enable stuff here.
                addon.onAddonPreEnable();
            } catch (Exception e) {
                Logging.errorLog("Failed to load addon: " + file.getName(), e);
                unloadAddon(addon);
            }

        } catch (Throwable ex) {
            Logging.errorLog("Failed to load addon classes from jar " + file.getName(), ex);
        }
    }

    /**
     * Searches the addon's jar file for the main class that extends BreweryAddon.
     *
     * @param jarFile     The jar file to search
     * @param classLoader The class loader to use
     * @return The main class that extends BreweryAddon
     */
    private Class<? extends BreweryAddon> getMainClass(File jarFile, ClassLoader classLoader) {
        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) { // Just iterate through every file in the jar file and check if it's a compiled java class.
                if (jarEntry.getName().endsWith(".class")) {

                    // We have to replace the '/' with '.' and remove the '.class' extension to get the canonical name of the class. (org.example.Whatever)
                    String className = jarEntry.getName().replaceAll("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz;
                        try {
                            // We don't want to initialize any classes. We'll leave that up to the JVM.
                            clazz = Class.forName(className, false, classLoader);
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            Logging.errorLog("An exception occurred while trying to load a class from an addon", e);
                            continue;
                        }
                        if (BreweryAddon.class.isAssignableFrom(clazz)) {
                            // Found our main class, we're going to load it now.
                            classLoader.loadClass(className);
                            return clazz.asSubclass(BreweryAddon.class);
                        }

                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to load class " + className, e);
                    }
                }
            }


        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load classes from jar " + jarFile.getName(), e);
        }
        throw new IllegalStateException("No class extending BreweryAddon found in jar " + jarFile.getName());
    }

}
