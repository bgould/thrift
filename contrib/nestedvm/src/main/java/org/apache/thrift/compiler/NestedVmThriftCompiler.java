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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.thrift.compiler.internal.Runtime;

/**
 * Implementation of {@link ThriftCompiler} that runs Thrift using NestedVM.
 * @author Benjamin Gould (bcg)
 */
public class NestedVmThriftCompiler extends ThriftCompiler {

  @Override
  public ExecutionResult execute(String... args) {
    final Runtime compiler = new Runtime();
    final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final String[] vm_args = createVmArgs(args, isWindows());
    compiler.closeFD(0);
    compiler.closeFD(1);
    compiler.closeFD(2);
    compiler.addFD(new Runtime.InputOutputStreamFD(in));
    compiler.addFD(new Runtime.InputOutputStreamFD(out));
    compiler.addFD(new Runtime.InputOutputStreamFD(err));
    final int exitCode = compiler.run(vm_args);
    return new ExecutionResult(
      exitCode,
      false,
      out.toString(),
      err.toString(),
      null
    );
  }

  @Override
  public String toString() {
    return "[" + getClass().getSimpleName() + "]";
  }

  @Override
  public boolean isNativeExecutable() {
    return false;
  }

  protected boolean isWindows() {
    return File.separatorChar == '\\';
  }

  /**
   * <p>Converts an argument array into a format suitable for passing to the
   * NestedVM runtime.  Specifically, NestedVM expects a program name to be in
   * argv[0] as in the main function of a C program.</p>
   * <p>Also, NestedVM cannot handle absolute file paths in Windows properly,
   * so if <code>isWindows == true</code> then the args are inspected for
   * file paths that start with Windows drive letters (i.e., C:\foo.thrift)
   * and converted to Cygwin style paths (i.e., /cygdrive/c/foo.thrift),
   * which the <code>realpath</code> syscall in NestedVM is able to handle.</p>
   * @param args Arguments to pass to the Thrift compiler program
   * @param isWindows Flag to denote if Windows paths should be munged
   * @return A new array of arguments including the program and updated paths
   */
  static final String[] createVmArgs(String[] args, boolean isWindows) {
    final String[] vm_args = new String[args.length + 1];
    vm_args[0] = "thrift";
    if (!isWindows) {
      // on most platforms, we can just copy the arguments to the compiler
      System.arraycopy(args, 0, vm_args, 1, args.length);
    } else {
      // on Windows, file paths might need to be munged for NestedVM to work
      for (int i = 0, c = args.length, file_path_flag = 0; i < c; i++) {
        final String arg = args[i];
        // munge if the next arg should be a file path, and also the last arg
        if (file_path_flag > 0 || (i == (c - 1))) {
          vm_args[i + 1] = cygwinifyFilePath(arg);
          file_path_flag = 0;
        } else {
          // set the 'file path flag' to true for certain options
          if ("-out".equals(arg) || "-o".equals(arg) || "-I".equals(arg)) {
            file_path_flag = 1;
          } else {
            file_path_flag = 0;
          }
          vm_args[i + 1] = arg;
        }
      }
    }
    return vm_args;
  }

  /**
   * <p>Converts absolute Windows file paths (i.e., C:\somedir\somefile.thrift)
   * to Cygwin-style file paths (i.e., /cygdrive/c/somedir/somefile.thrift)</p>
   * @param filepath The file path to possibly munge
   * @return The modified file path
   */
  private static final String cygwinifyFilePath(String filepath) {
    final int start;
    final int strlen = filepath.length();
    final char c0 = filepath.charAt(0);
    final char c2 = filepath.charAt(2);
    final StringBuilder sb = new StringBuilder(filepath.length() + 15);
    if ( (':' == filepath.charAt(1)) &&
        ((c2 == '/') || (c2 == '\\')) &&
        ((c0 >= 65 && c0 <= 90) || (c0 >= 97 && c0 <= 122)) ) {
      sb.append("/cygdrive/").append(Character.toLowerCase(c0)).append('/');
      start = 3;
    } else {
      start = 0;
    }
    for (int i = start; i < strlen; i++) {
      final char c = filepath.charAt(i);
      switch (c) {
      case '\\':
        sb.append('/');
        break;
      default:
        sb.append(c);
      }
    }
    return sb.toString();
  }

}
