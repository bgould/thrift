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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link ThriftCompiler} that uses a native Thrift executable.
 * @author Benjamin Gould (bcg)
 */
public class NativeThriftCompiler extends ThriftCompiler {

  private final String executable;

  public NativeThriftCompiler(String executable) {
    this.executable = executable;
  }

  public ExecutionResult execute(String... args) {
    if (args == null) {
      throw new IllegalArgumentException("args cannot be null");
    }
    final String[] proc_args = new String[args.length + 1];
    proc_args[0] = executable;
    System.arraycopy(args, 0, proc_args, 1, args.length);
    String outString = null;
    String errString = null;
    int exit = Integer.MIN_VALUE;
    boolean interrupted = false;
    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final ByteArrayOutputStream err = new ByteArrayOutputStream();
      final Process process = Runtime.getRuntime().exec(proc_args);
      final InputStream outstream = process.getInputStream();
      final InputStream errstream = process.getErrorStream();
      final byte[] buffer = new byte[1024];
      try {
        for (boolean exited = false; !exited; ) {
          final int readFromOut = nonBlockingCopy(outstream, out, buffer);
          final int readFromErr = nonBlockingCopy(errstream, err, buffer);
          if (readFromOut < 1 && readFromErr < 1) {
            try {
              exit = process.exitValue();
              exited = true;
            } catch (IllegalThreadStateException e) {
              // the process has not exited yet, so loop again
              try { Thread.sleep(1); } catch (InterruptedException e2) {}
            }
          }
        }
        blockingCopy(outstream, out, buffer);
        blockingCopy(errstream, err, buffer);
      } finally {
        try { outstream.close(); } catch (IOException e) {}
        try { errstream.close(); } catch (IOException e) {}
        process.destroy();
      }
      outString = out.toString();
      errString = err.toString();
      return new ExecutionResult(exit, interrupted, outString, errString, null);
    } catch (IOException e) {
      throw new ThriftCompilerException(e);
    }
  }

  public String getExecutable() {
    return executable;
  }

  @Override
  public boolean isNativeExecutable() {
    return true;
  }

  private final int nonBlockingCopy(
        final InputStream in, final OutputStream out, final byte[] buf
      ) throws IOException {
    final int bytesAvailable = in.available();
    if (bytesAvailable > 0) {
      final int bytesRead = in.read(buf, 0, Math.min(bytesAvailable, 1024));
      if (bytesRead > 0) {
        out.write(buf, 0, bytesRead);
      }
      return bytesRead;
    } else {
      return 0;
    }
  }

  private final int blockingCopy(
      final InputStream in, final OutputStream out, final byte[] buf
    ) throws IOException {
    int total = 0;
    for (int n; (n = in.read(buf)) > -1; total += n) {
      out.write(buf, 0, n);
    }
    return total;
  }

}
