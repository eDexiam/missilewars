/*
 * This file is part of MissileWars (https://github.com/Butzlabben/missilewars).
 * Copyright (c) 2018-2021 Daniel Nägele.
 *
 * MissileWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MissileWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MissileWars.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.butzlabben.missilewars.util;

import de.butzlabben.missilewars.Config;
import de.butzlabben.missilewars.Logger;
import de.butzlabben.missilewars.MissileWars;
import de.butzlabben.missilewars.game.Arenas;
import de.butzlabben.missilewars.wrapper.abstracts.Arena;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Butzlabben
 * @since 14.08.2018
 */
public class SetupUtil {

    private static final int BUFFER_SIZE = 4096;

    private SetupUtil() {
    }

    public static void checkShields() {
        for (Arena arena : Arenas.getArenas()) {
            File file = new File(MissileWars.getInstance().getDataFolder(), arena.getShieldConfiguration().getSchematic());
            if (!file.isFile()) {
                Logger.BOOT.log("Copying default shield schematic");

                String resource = "shield.schematic";
                copyFile(resource, file.getPath());

            }
        }
    }

    public static void checkMap(String worldName) {
        File file = new File(Config.getArenaFolder() + "/" + worldName);
        if (!file.isDirectory()) {

            Logger.WARN.log("There was no map found with the name \"" + worldName + "\"");
            Logger.BOOT.log("Copying default map");

            String resource = "MissileWars-Arena.zip";

            try {
                copyZip(resource, file.getPath());
            } catch (IOException e) {
                Logger.ERROR.log("Unable to copy new map");
                e.printStackTrace();
            }
        }
    }

    public static void checkMissiles() {
        File file = new File(MissileWars.getInstance().getDataFolder(), "missiles");
        if (!file.isDirectory()) {
            Logger.BOOT.log("Copying default missiles folder");

            String resource = "missiles.zip";

            try {
                copyZip(resource, file.getPath());
            } catch (IOException e) {
                Logger.ERROR.log("Unable to copy missiles");
                e.printStackTrace();
            }
        }
    }

    private static void copyFile(String resource, String out) {
        File file = new File(out);
        if (!file.exists()) {
            try {
                InputStream in = MissileWars.getInstance().getResource(resource);
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                System.err.println("Wasn't able to create Config");
                e.printStackTrace();
            }
        }
    }

    private static void copyFolder(String resource, String outputFolder) throws IOException {
        copyResourcesToDirectory(jarForClass(MissileWars.class, null), resource, outputFolder);
    }

    public static void copyZip(String resource, String outputFolder) throws IOException {
        File out = new File(MissileWars.getInstance().getDataFolder(), resource);

        InputStream in = JavaPlugin.getPlugin(MissileWars.class).getResource(resource);

        Files.copy(in, out.toPath());

        unzip(out.getPath(), outputFolder);
        out.deleteOnExit();
    }

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));

        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn
     * @param filePath
     *
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static JarFile jarForClass(Class<?> clazz, JarFile defaultJar) {
        String path = "/" + clazz.getName().replace('.', '/') + ".class";
        URL jarUrl = clazz.getResource(path);
        if (jarUrl == null) {
            return defaultJar;
        }

        String url = jarUrl.toString();
        int bang = url.indexOf("!");
        String JAR_URI_PREFIX = "jar:file:";
        if (url.startsWith(JAR_URI_PREFIX) && bang != -1) {
            try {
                return new JarFile(url.substring(JAR_URI_PREFIX.length(), bang));
            } catch (IOException e) {
                throw new IllegalStateException("Error loading jar file.", e);
            }
        } else {
            return defaultJar;
        }
    }

    /**
     * Copies a directory from a jar file to an external directory.
     */
    public static void copyResourcesToDirectory(JarFile fromJar, String jarDir, String destDir) throws IOException {
        for (Enumeration<JarEntry> entries = fromJar.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(jarDir + "/") && !entry.isDirectory()) {
                File dest = new File(destDir + "/" + entry.getName().substring(jarDir.length() + 1));
                File parent = dest.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                try (FileOutputStream out = new FileOutputStream(dest); InputStream in = fromJar.getInputStream(entry)) {
                    byte[] buffer = new byte[8 * 1024];

                    int s;
                    while ((s = in.read(buffer)) > 0) {
                        out.write(buffer, 0, s);
                    }
                } catch (IOException e) {
                    throw new IOException("Could not copy asset from jar file", e);
                }
            }
        }
    }
}
