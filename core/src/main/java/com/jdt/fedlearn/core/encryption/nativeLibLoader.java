/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Loading native library (code adapted from TensorFlow's NativeLibrary class)
 */
public final class nativeLibLoader {
        private static final boolean DEBUG = false;
        private static final String JNI_LIBNAME = "Distpaillier";

        public static void load() {
            if (tryLoadLibrary()) {
                // Either:
                // (1) The native library has already been statically loaded, OR
                // (2) The required native code has been statically linked (through a custom launcher), OR
                // (3) The native code is part of another library (such as an application-level library)
                // that has already been loaded. For example, tensorflow/examples/android and
                // tensorflow/tools/android/inference_interface include the required native code in
                // differently named libraries.
                //
                // Doesn't matter how, but it seems the native code is loaded, so nothing else to do.
                return;
            }
            // Native code is not present, perhaps it has been packaged into the .jar file containing this.
            // Extract the JNI library itself
            final String jniLibName = System.mapLibraryName(JNI_LIBNAME);
            final String jniResourceName = makeResourceName(jniLibName);
            log("jniResourceName: " + jniResourceName);
            final InputStream jniResource =
                    nativeLibLoader.class.getClassLoader().getResourceAsStream(jniResourceName);
            // Extract the JNI's dependency
            final String frameworkLibName =
                    getVersionedLibraryName(System.mapLibraryName(jniLibName));
            final String frameworkResourceName = makeResourceName(frameworkLibName);
            log("frameworkResourceName: " + frameworkResourceName);
            final InputStream frameworkResource = nativeLibLoader.class.getClassLoader().getResourceAsStream(frameworkResourceName);
            // Do not complain if the framework resource wasn't found. This may just mean that we're
            // building with --config=monolithic (in which case it's not needed and not included).
            if (jniResource == null) {
                throw new UnsatisfiedLinkError(
                        String.format(
                                "Cannot find DistPaillier native library for OS: %s, architecture: %s.",
                                os(), architecture()));
            }
            try {
                // Create a temporary directory for the extracted resource and its dependencies.
                final File tempPath = createTemporaryDirectory();
                // Deletions are in the reverse order of requests, so we need to request that the directory be
                // deleted first, so that it is empty when the request is fulfilled.
                tempPath.deleteOnExit();
                final String tempDirectory = tempPath.getCanonicalPath();
                if (frameworkResource != null) {
                    extractResource(frameworkResource, frameworkLibName, tempDirectory);
                } else {
                    log(
                            frameworkResourceName
                                    + " not found. This is fine assuming "
                                    + jniResourceName
                                    + " is not built to depend on it.");
                }
                System.load(extractResource(jniResource, jniLibName, tempDirectory));
            } catch (IOException e) {
                throw new UnsatisfiedLinkError(
                        String.format(
                                "Unable to extract native library into a temporary file (%s)", e.toString()));
            }
        }

        private static boolean tryLoadLibrary() {
            try {
                System.loadLibrary(JNI_LIBNAME);
                return true;
            } catch (UnsatisfiedLinkError e) {
                log("tryLoadLibraryFailed: " + e.getMessage());
                return false;
            }
        }

        private static boolean resourceExists(String baseName) {
            return nativeLibLoader.class.getClassLoader().getResource(makeResourceName(baseName)) != null;
        }

        private static String getVersionedLibraryName(String libFilename) {
            final String versionName = getMajorVersionNumber();

            // If we're on darwin, the versioned libraries look like blah.1.dylib.
            final String darwinSuffix = ".dylib";
            if (libFilename.endsWith(darwinSuffix)) {
                final String prefix = libFilename.substring(0, libFilename.length() - darwinSuffix.length());
                if (versionName != null) {
                    final String darwinVersionedLibrary = prefix + "." + versionName + darwinSuffix;
                    if (resourceExists(darwinVersionedLibrary)) {
                        return darwinVersionedLibrary;
                    }
                } else {
                    // If we're here, we're on darwin, but we couldn't figure out the major version number. We
                    // already tried the library name without any changes, but let's do one final try for the
                    // library with a .so suffix.
                    final String darwinSoName = prefix + ".so";
                    if (resourceExists(darwinSoName)) {
                        return darwinSoName;
                    }
                }
            } else if (libFilename.endsWith(".so")) {
                // Libraries ending in ".so" are versioned like "libfoo.so.1", so try that.
                final String versionedSoName = libFilename + "." + versionName;
                if (versionName != null && resourceExists(versionedSoName)) {
                    return versionedSoName;
                }
            }

            // Otherwise, we've got no idea.
            return libFilename;
        }

        /**
         * Returns the major version number of this TensorFlow Java API, or {@code null} if it cannot be
         * determined.
         */
        private static String getMajorVersionNumber() {
            String version = nativeLibLoader.class.getPackage().getImplementationVersion();
            // expecting a string like 1.14.0, we want to get the first '1'.
            int dotIndex;
            if (version == null || (dotIndex = version.indexOf('.')) == -1) {
                return "1";
            }
            String majorVersion = version.substring(0, dotIndex);
            try {
                Integer.parseInt(majorVersion);
                return majorVersion;
            } catch (NumberFormatException unused) {
                return null;
            }
        }

        private static String extractResource(
                InputStream resource, String resourceName, String extractToDirectory) throws IOException {
            final File dst = new File(extractToDirectory, resourceName);
            dst.deleteOnExit();
            final String dstPath = dst.toString();
            log("extracting native library to: " + dstPath);
            final long nbytes = copy(resource, dst);
            log(String.format("copied %d bytes to %s", nbytes, dstPath));
            return dstPath;
        }

        private static String os() {
            final String p = System.getProperty("os.name").toLowerCase();
            if (p.contains("linux")) {
                return "linux";
            } else if (p.contains("os x") || p.contains("darwin")) {
                return "darwin";
            } else if (p.contains("windows")) {
                return "windows";
            } else {
                return p.replaceAll("\\s", "");
            }
        }

        private static String architecture() {
            final String arch = System.getProperty("os.arch").toLowerCase();
            return ("amd64".equals(arch)) ? "x86_64" : arch;
        }

        private static void log(String msg) {
            if (DEBUG) {
                System.err.println("org.tensorflow.loadNativeJNI: " + msg);
            }
        }

        private static String makeResourceName(String baseName) {
//            return "org/tensorflow/native/" + String.format("%s-%s/", os(), architecture()) + baseName;
            return baseName;
        }

        private static long copy(InputStream src, File dstFile) throws IOException {
            FileOutputStream dst = new FileOutputStream(dstFile);
            try {
                byte[] buffer = new byte[1 << 20]; // 1MB
                long ret = 0;
                int n = 0;
                while ((n = src.read(buffer)) >= 0) {
                    dst.write(buffer, 0, n);
                    ret += n;
                }
                return ret;
            } finally {
                dst.close();
                src.close();
            }
        }

        // Shamelessly adapted from Guava to avoid using java.nio, for Android API
        // compatibility.
        private static File createTemporaryDirectory() {
            File baseDirectory = new File(System.getProperty("java.io.tmpdir"));
            String directoryName = "native_libraries-" + System.currentTimeMillis() + "-";
            for (int attempt = 0; attempt < 1000; attempt++) {
                File temporaryDirectory = new File(baseDirectory, directoryName + attempt);
                if (temporaryDirectory.mkdir()) {
                    return temporaryDirectory;
                }
            }
            throw new IllegalStateException(
                    "Could not create a temporary directory (tried to make "
                            + directoryName
                            + "*) to extract native libraries.");
        }

        private nativeLibLoader() {}

}
