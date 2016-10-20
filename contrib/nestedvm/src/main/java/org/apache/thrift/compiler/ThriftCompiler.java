/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.thrift.compiler;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <p>Provides a Java API for invoking the Apache Thrift compiler.</p>
 * <p>This class is capable of invoking the Thrift compiler in two ways. First,
 * a pure Java implementation of the compiler can be used, requiring no
 * external dependencies. However, if a native build of the Thrift compiler
 * is available on the host system, it can be used instead of the pure Java
 * version. This may be advantageous if you need to use a specific version of
 * Thrift, or also because the Java version executes more slowly and has
 * significant startup overhead on the first invocation (1-2 seconds, YMMV).</p>
 * <p>Instances of this class encapsulate the type of invocation to be
 * performed (native vs. pure Java, path to the executable, etc), and are
 * immutable and thread safe so it is fine to share across threads without
 * synchronization. Instantiation is performed through the static
 * <code>newCompiler</code> factory methods defined by this class.
 * </p>
 * @author Benjamin Gould (bcg)
 */
public abstract class ThriftCompiler {

  public static void main(String... args) throws Throwable {
    if ((args.length > 0) &&
        (args[0].equals(OPT_EXPORT_SRC) || args[0].equals(OPT_UNZIP_SRC))) {
      if (args.length == 2) {
        final File destDir = new File(args[1]);
        try {
          if (OPT_EXPORT_SRC.equals(args[0])) {
            ThriftCompiler.exportSource(destDir);
          } else if (OPT_UNZIP_SRC.equals(args[0])) {
            ThriftCompiler.unzipSource(destDir);
          } else {
            throw new IllegalStateException();
          }
          System.exit(0);
        } catch (Exception e) {
          System.err.println("An error occurred: " + e.getMessage());
          System.exit(2);
        }
      } else {
        System.err.println("thrift " + args[0] + " <destination directory>");
        System.exit(1);
      }
    }
    final ThriftCompiler compiler = ThriftCompiler.newCompiler();
    final ExecutionResult result = compiler.execute(args);
    if (result.throwable != null) {
      throw result.throwable;
    }
    if (result.outString != null) {
      System.out.print(result.outString);
    }
    if (result.errString != null) {
      System.err.print(result.errString);
    }
    System.exit(result.exitCode);
  }

  public static File exportSource(File destDir) throws IOException {
    final File destFile = new File(requireNonNull(destDir), THRIFT_SRC_RSRC);
    if (!destDir.isDirectory()) {
      throw new IOException(
        "Destination for " + THRIFT_SRC_RSRC + " must be a directory.");
    }
    if (destFile.exists()) {
      throw new IOException(
        "Destination file for " + THRIFT_SRC_RSRC + " already exists.");
    }
    try (final InputStream libsIn = thriftLibsResource().openStream()) {
      try (final FileOutputStream out = new FileOutputStream(destFile)) {
        final byte[] buffer = new byte[2048];
        for (int n = -1; (n = libsIn.read(buffer)) > -1; ) {
          out.write(buffer, 0, n);
        }
      }
    }
    return destFile;
  }

  public static File unzipSource(File destDir) throws IOException {
    final File resultFile = new File(destDir, "lib");
    if (resultFile.exists()) {
      throw new IOException(
        "Destination " + resultFile.getAbsolutePath() + " already exists.");
    }
    try (final InputStream libsIn = thriftLibsResource().openStream()) {
      try (final ZipInputStream zipIn = new ZipInputStream(libsIn)) {
        final byte[] buffer = new byte[2048];
        for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
          final File file = new File(destDir, entry.getName());
          if (!entry.getName().startsWith("thrift-src/")) {
            throw new IllegalStateException(
              "entry should start with thrift-src/: " + entry.getName());
          }
          if (entry.isDirectory()) {
            file.mkdirs();
          } else {
            try (final FileOutputStream out = new FileOutputStream(file)) {
              for (int n = -1; (n = zipIn.read(buffer)) > -1; ) {
                out.write(buffer, 0, n);
              }
            }
          }
        }
      }
    }
    return resultFile;
  }

  public static final String OPT_UNZIP_SRC = "--unzip-source";

  public static final String OPT_EXPORT_SRC = "--export-source";

  public static final String PROPERTY_DEBUG = "thrift.compiler.debug";

  public static final String PROPERTY_NATIVE = "thrift.compiler.native";

  public static final String PROPERTY_EXECUTABLE = "thrift.compiler.executable";

  public static final String WINDOWS_EXECUTABLE = "thrift.exe";

  public static final String DEFAULT_EXECUTABLE = "thrift";

  public static final String THRIFT_SRC_RSRC = "thrift-src.zip";

  /**
   * <p>
   * Instantiates an instance of {@link ThriftCompiler} based on a
   * {@link java.util.Properties} object that it takes as its argument to allow
   * the caller fine-grained control over the implementation of the compiler
   * that will be used.  By default, an instance of the pure Java implementation
   * of the compiler will be created. To control or alter this behavior, the
   * following properties can be specified:
   * </p>
   * <p>
   * <code>thrift.compiler.native</code>: set this property to <code>true</code>
   * in order to use a native executable (by default <code>thrift.exe</code> on
   * Windows, <code>thrift</code> on other systems)
   * </p>
   * <p>
   * <code>thrift.compiler.executable</code>: if
   * <code>thrift.compiler.native</code> is set to true, this property can be
   * used to provide the path to the specific Thrift executable to be used.
   * </p>
   * <p>
   * The above properties can be set on the {@link java.util.Properties} object
   * passed as an argument, or alternatively can be specified as system
   * properties.  Values passed in via the argument take precedence over
   * values specified as system properties.
   * </p>
   * <p>
   * Returns a new {@link ThriftCompiler}. The exact implementation of the
   * compiler will be dictated first by the supplied
   * {@link java.util.Properties} object, and then the system properties,
   * as a fallback if properties are not specified on the argument object.
   * Passing null as the argument to this method is same as passing an empty
   * {@link java.util.Properties} object.
   * </p>
   * @param properties Properties to consult when constructing the compiler.
   * @return A new instance of the Thrift compiler.
   */
  public static final ThriftCompiler newCompiler(Properties properties) {
    debug("newCompiler(Properties) called");
    if (properties == null) {
      properties = new Properties();
    }
    final String nativeProp = properties.getProperty(PROPERTY_NATIVE,
                                  System.getProperty(PROPERTY_NATIVE, ""));
    final boolean useNative = Boolean.valueOf(nativeProp);
    if (useNative) {
      String executable = properties.getProperty(PROPERTY_EXECUTABLE,
                              System.getProperty(PROPERTY_EXECUTABLE, ""));
      if ("".equals(executable.trim())) {
        executable = getDefaultExecutableName();
      }
      return new NativeThriftCompiler(executable);
    } else {
      return new JavaThriftCompiler();
    }
  }

  /**
   * <p>
   * "Automagically" chooses an implementation of the Thrift compiler based on
   * the host environment.  If the <code>thrift.compiler.native</code> system
   * property is set to a non-null value, this method is the equivalent of
   * instantiating a compiler with a <code>null</code>
   * {@link java.util.Properties} object. Otherwise, the environment's
   * <code>PATH</code> will be searched for an executable named 'thrift' (or
   * 'thrift.exe' on Windows).  If the output of '<code>thrift -v</code>'
   * matches the version string of the embedded pure Java implementation, the
   * native executable is used as an optimization. If the 'thrift' command
   * cannot be executed or if the version does not match, the embedded pure
   * Java version is used as a fallback.
   * </p>
   * <p>
   * Returns a new {@link ThriftCompiler}.  The exact implementation of the
   * compiler will be dictated by system properties, or by default will be the
   * {@link JavaThriftCompiler} if system properties are not specified.
   * </p>
   * @return A new instance of the Thrift compiler.
   */
  public static final ThriftCompiler newCompiler() {
    debug("newCompiler() called");
    final boolean nativeProp = System.getProperty(PROPERTY_NATIVE) != null;
    if (!nativeProp) {
      debug("native property not set; loading default properties");
      return newCompiler(loadDefaultProperties());
    } else {
      debug("native property is set; skipping default properties");
      return newCompiler(null);
    }
  }

  /**
   * Finds the default executable name for the host platform.
   * (<code>thrift.exe</code> on Windows, <code>thrift</code> on all others).
   * @return The default executable name for the host platform.
   */
  public static final String getDefaultExecutableName() {
    return System.getProperty("os.name").startsWith("Windows")
      ? WINDOWS_EXECUTABLE
      : DEFAULT_EXECUTABLE;
  }

  /**
   * <p>Executes the compiler with the supplied arguments.</p>
   * @param args The arguments to pass in to the Thrift compiler.
   * @return {@link ExecutionResult} encapsulating the details of the execution.
   */
  public abstract ExecutionResult execute(String... args);

  /**
   * <p>Determine if a native Thrift compiler is being used.</p>
   * @return True if the compiler is using a native executable.
   */
  public abstract boolean isNativeExecutable();

  /**
   * <p>Executes the Thrift compiler to get the version string.</p>
   * @return The output of the compiler with the <code>-version</code> flag
   */
  public String version() {
    final ExecutionResult result = execute("-version");
    return result.outString.trim();
  }

  /**
   * <p>Executes the Thrift compiler to get the help string.</p>
   * @return The output of the compiler with the <code>-help</code> flag
   */
  public String help() {
    final ExecutionResult result = execute("-help");
    return result.errString.trim();
  }

  private static URL thriftLibsResource() throws IOException {
    final URL rsrc = ThriftCompiler.class.getResource(THRIFT_SRC_RSRC);
    if (rsrc == null) {
      throw new IOException(
          "Embedded " + THRIFT_SRC_RSRC + " resource not found.");
    }
    return rsrc;
  }

  /**
   * <p>Retrieves the version string of embedded Java Thrift compiler</p>
   * @return The version string
   */
  private static final String embeddedThriftCompilerVersion() {
    final URL rsc = ThriftCompiler.class.getResource("thrift.compiler.version");
    if (rsc == null) {
      throw new IllegalStateException("thift.compiler.version not found");
    }
    try (final InputStream in = rsc.openStream()) {
      try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        final byte[] buffer = new byte[64];
        for (int n = -1; (n = in.read(buffer)) > -1; ) {
          baos.write(buffer, 0, n);
        }
        return baos.toString("UTF-8").trim();
      }
    } catch (IOException e) {
      throw new ThriftCompilerException(e);
    }
  }

  /**
   * <p>Lazy loads and returns the default properties object for use when no
   * properties are specified for initializing the Thrift compiler</p>
   * @return The default properties
   */
  private static final Properties loadDefaultProperties() {
    return ThriftDefaultProperties.INSTANCE;
  }

  private static final boolean isDebug() {
    return System.getProperty(PROPERTY_DEBUG, "").equals("true");
  }

  private static final void debug(String fmt, Object... args) {
    if (isDebug()) {
      System.err.format("[ThriftCompiler] " + fmt + "%n", args);
    }
  }

  private static final class ThriftDefaultProperties {

    private static final Properties INSTANCE = defaultProperties();

    /**
     * <p>
     * This method is only intended to run once during the JVM lifecycle...
     * its purpose is lazy load an instance of the default properties to use
     * for {@link ThriftCompiler} when properties are not otherwise specified.
     * </p>
     * <p>
     * First it tries to run "-version" using the default executable name
     * (either "thrift" or "thrift.exe" depending on platform).  If that leads
     * to any error, it is assumed that the pure Java version of the compiler
     * should be used as a fallback.  In that case, a Thread is launch to load
     * the class in the background (it is large and can take 1-2 secs to load).
     * </p>
     * @return
     */
    private static final Properties defaultProperties() {
      debug("defaultProperties() called");
      final String dflt = getDefaultExecutableName();
      final ThriftCompiler nativeCompiler = new NativeThriftCompiler(dflt);
      debug("attempting to run native compiler: %s", nativeCompiler);
      String nativeVersion;
      try {
        nativeVersion = nativeCompiler.version();
      } catch (ThriftCompilerException e) {
        nativeVersion = null;
      }
      debug("native version string: %s", nativeVersion);
      final String embeddedVer = embeddedThriftCompilerVersion();
      debug("embedded Java version: %s", embeddedVer);
      if (nativeVersion != null && nativeVersion.equals(embeddedVer)) {
        final Properties result = new Properties();
        result.setProperty(ThriftCompiler.PROPERTY_NATIVE, "true");
        result.setProperty(ThriftCompiler.PROPERTY_EXECUTABLE, dflt);
        debug("using native version for default: %s", result);
        return result;
      }
      final Thread loaderThread = new Thread(new Runnable() {
        @Override
        public void run() {
          debug("loading embedded Java Thrift compiler");
          final String pkg = ThriftCompiler.class.getPackage().getName();
          final String cls = pkg + ".internal.Runtime";
          try {
            Class.forName(cls);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
          debug("embedded Java Thrift compiler class loaded");
        }
      });
      loaderThread.start();
      return new Properties();
    }
  }

}
