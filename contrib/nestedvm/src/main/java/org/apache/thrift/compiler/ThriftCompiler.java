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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

/**
 * <p>Provides an entry point for invoking the Apache Thrift compiler.</p>
 * <p>This class is capable of invoking the Thrift compiler in two ways. First,
 * a pure Java implementation of the compiler can be invoked, requiring no
 * external dependencies. However, if a native build of the Thrift compiler
 * is available, it can be used instead of the pure Java version. This
 * may be advantageous if you need to use a specific version of Thrift, or also
 * because the Java version has significant startup overhead on the first
 * invocation (1-2 seconds, YMMV).</p>
 * <p>Instances of this class encapsulate the type of invocation to be
 * performed (native vs. pure Java, path to the executable, etc), and are
 * immutable and thread safe so it is fine to share across threads without
 * synchronization.</p>
 * <p>Instantiation is performed through the static <code>newCompiler</code>
 * factory methods defined by this class.  There are three variants.  With no
 * arguments,
 * </p>
 * <p>The first variant, which takes a {@link java.util.Properties} object as
 * its argument, allows the caller fine-grained control over how the compiler
 * will be invoked.  By default, the pure Java implementation of the compiler
 * will be invoked. To control this behavior, the following properties can be
 * specified:
 * </p>
 * <p>
 * <code>thrift.compiler.native</code>: set this property to <code>true</code>
 * in order to use a native executable (by default <code>thrift.exe</code> on
 * Windows, <code>thrift</code> on other systems)
 * </p>
 * <p>
 * <code>thrift.compiler.executable</code>: if
 * <code>thrift.compiler.native</code> is set to true, this property can be used
 * to provide the path to the specific Thrift executable to be used.
 * </p>
 * <p>
 * The above properties can be set on the {@link java.util.Properties} object
 * argument to the <code>newCompiler</code> method, or alternatively can be
 * specified as system properties.  Values passed in to the static factory
 * method take precedence over values specified as system properties.
 * </p>
 * <p>
 * The second variant of the <code>newCompiler</code> method takes a boolean
 * argument.  When set to false, this method is the equivalent of passing
 * <code>null</code> to the first variant of <code>newCompiler</code>.
 * When set to <code>true</code>, the environment's <code>PATH</code>
 * will be searched for an executable named 'thrift' (or 'thrift.exe' on
 * Windows).  If the output of 'thrift -v' matches the version of the pure Java
 * version of the compiler, the native executable is used.  Otherwise the
 * embedded pure Java version is invoked as a fallback.
 * </p>
 * <p>
 * The third variant of <code>newCompiler</code>, with no arguments, is the
 * equivalent of calling <code>newCompiler(true)</code>.
 * </p>
 * @author Benjamin Gould (bcg)
 */
public abstract class ThriftCompiler {

  public static void main(String... args) throws Throwable {
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

  public static final String PROPERTY_NATIVE = "thrift.compiler.native";

  public static final String PROPERTY_EXECUTABLE = "thrift.compiler.executable";

  public static final String WINDOWS_EXECUTABLE = "thrift.exe";

  public static final String DEFAULT_EXECUTABLE = "thrift";

  public static final ThriftCompiler newCompiler() {
    return newCompiler(true);
  }

  /**
   * <p>
   * Returns a new {@link ThriftCompiler}.  The exact implementation of the
   * compiler will be dictated by system properties, or by default will be the
   * {@link JavaThriftCompiler} if system properties are not specified.
   * </p>
   * @return A new instance of the Thrift compiler.
   */
  public static final ThriftCompiler newCompiler(boolean checkPathForNative) {
    final boolean nativeProp = System.getProperty(PROPERTY_NATIVE) == null;
    if (nativeProp && checkPathForNative) {
      return newCompiler(loadDefaultProperties());
    } else {
      return newCompiler(null);
    }
  }

  /**
   * <p>
   * Returns a new {@link ThriftCompiler}. The exact implementation of the
   * compiler will be dictated first by the supplied
   * {@link java.util.Properties} object, and then the system properties,
   * as a fallback.
   * </p>
   * @param properties Properties to consult when constructing the compiler.
   * @return A new instance of the Thrift compiler.
   */
  public static final ThriftCompiler newCompiler(Properties properties) {
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

  public static final Properties loadDefaultProperties() {
    return ThriftDefaultProperties.INSTANCE;
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
      final String dflt = getDefaultExecutableName();
      final ThriftCompiler nativeCompiler = new NativeThriftCompiler(dflt);
      String nativeVersion;
      try {
        nativeVersion = nativeCompiler.version();
      } catch (ThriftCompilerException e) {
        nativeVersion = null;
      }
      final String embeddedVer = embeddedThriftCompilerVersion();
      if (nativeVersion != null && nativeVersion.equals(embeddedVer)) {
        final Properties result = new Properties();
        result.setProperty(ThriftCompiler.PROPERTY_NATIVE, "true");
        result.setProperty(ThriftCompiler.PROPERTY_EXECUTABLE, dflt);
        return result;
      }
      final Thread loaderThread = new Thread(new Runnable() {
        @Override
        public void run() {
          final String pkg = ThriftCompiler.class.getPackage().getName();
          final String cls = pkg + ".internal.Runtime";
          try {
            Class.forName(cls);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      });
      loaderThread.start();
      return new Properties();
    }
  }

  private static final String embeddedThriftCompilerVersion() {
    final URL rsc = ThriftCompiler.class.getResource("thrift.compiler.version");
    if (rsc == null) {
      throw new IllegalStateException("thift.compiler.version not found");
    }
    try (final InputStream in = rsc.openStream()) {
      try (final Scanner s = new Scanner(in)) {
        return s.useDelimiter("\\A").hasNext() ? s.next().trim() : "";
      }
    } catch (IOException e) {
      throw new ThriftCompilerException(e);
    }
  }

}
