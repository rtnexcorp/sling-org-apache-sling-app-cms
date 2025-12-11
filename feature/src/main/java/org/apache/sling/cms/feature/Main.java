/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.cms.feature;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for Sling CMS Feature Model launcher.
 *
 * Supports multiple run profiles:
 * - standalone (default): Single instance with both authoring and rendering
 * - author: Authoring instance for content management
 * - renderer: Rendering/publishing instance for serving content
 *
 * Usage:
 *   java -jar slingcms.jar                           # Runs in standalone mode (default)
 *   java -jar slingcms.jar -P standalone             # Runs in standalone mode
 *   java -jar slingcms.jar -P author                 # Runs in author mode
 *   java -jar slingcms.jar -P renderer               # Runs in renderer mode
 *   java -jar slingcms.jar --profile=author          # Alternative syntax
 */
public class Main {

    private static final String DEFAULT_PROFILE = "standalone";
    private static final String[] VALID_PROFILES = {"standalone", "author", "renderer"};

    public static void main(String[] args) throws IOException {
        String profile = DEFAULT_PROFILE;
        List<String> remainingArgs = new ArrayList<>();

        // Parse profile argument
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-P") || arg.equals("--profile")) {
                // -P <profile> or --profile <profile>
                if (i + 1 < args.length) {
                    profile = args[++i].toLowerCase();
                } else {
                    System.err.println("Error: -P/--profile requires a value");
                    printUsage();
                    System.exit(1);
                }
            } else if (arg.startsWith("-P=") || arg.startsWith("--profile=")) {
                // -P=<profile> or --profile=<profile>
                profile = arg.substring(arg.indexOf('=') + 1).toLowerCase();
            } else if (arg.equals("-h") || arg.equals("--help")) {
                printUsage();
                System.exit(0);
            } else {
                remainingArgs.add(arg);
            }
        }

        // Validate profile
        if (!isValidProfile(profile)) {
            System.err.println("Error: Invalid profile '" + profile + "'");
            System.err.println("Valid profiles are: " + String.join(", ", VALID_PROFILES));
            System.exit(1);
        }

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           Apache Sling CMS Feature Model                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Profile: " + padRight(profile.toUpperCase(), 46) + " ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // Determine FAR file based on profile
        String farFileName = "lib/slingcms-" + profile + ".far";
        URL farUrl = Main.class.getClassLoader().getResource(farFileName);

        // Fallback to legacy single FAR file (for backward compatibility with old JARs)
        if (farUrl == null) {
            farUrl = Main.class.getClassLoader().getResource("lib/slingcms.far");
            if (farUrl != null) {
                System.out.println("Note: Using legacy single FAR file (profile selection not available)");
            }
        }

        if (farUrl == null) {
            System.err.println("Error: Could not find feature archive for profile: " + profile);
            System.err.println("Expected: " + farFileName);
            System.exit(1);
        }

        List<String> launcherArgs = new ArrayList<>(remainingArgs);
        launcherArgs.add("-f");
        launcherArgs.add(farUrl.toString());

        org.apache.sling.feature.launcher.impl.Main.main(launcherArgs.toArray(new String[0]));
    }

    private static boolean isValidProfile(String profile) {
        for (String valid : VALID_PROFILES) {
            if (valid.equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static void printUsage() {
        System.out.println("Apache Sling CMS - Feature Model Launcher");
        System.out.println();
        System.out.println("Usage: java -jar slingcms.jar [options]");
        System.out.println();
        System.out.println("Profile Options:");
        System.out.println("  -P, --profile <profile>  Run profile (standalone|author|renderer)");
        System.out.println("                           Default: standalone");
        System.out.println();
        System.out.println("Profiles:");
        System.out.println("  standalone  Single instance with both authoring and rendering (default)");
        System.out.println("  author      Authoring instance for content management");
        System.out.println("  renderer    Rendering/publishing instance for serving content");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar slingcms.jar                    # Run standalone");
        System.out.println("  java -jar slingcms.jar -P author          # Run as author");
        System.out.println("  java -jar slingcms.jar -P renderer        # Run as renderer");
        System.out.println("  java -jar slingcms.jar --profile=author   # Alternative syntax");
        System.out.println();
        System.out.println("Additional options are passed to the Sling Feature Launcher.");
    }
}
