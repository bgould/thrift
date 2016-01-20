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
import java.util.Scanner;

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
    boolean processDestroyed = false;
    try {
//      final ByteArrayOutputStream out = new ByteArrayOutputStream();
//      final ByteArrayOutputStream err = new ByteArrayOutputStream();
      final Process process = Runtime.getRuntime().exec(proc_args);
//      final byte[] buffer = new byte[1024];
      try {
//        outString = asString(process.getInputStream());
//        errString = asString(process.getErrorStream());
        /*
        for (int e = 0, o = 0; (e > -1 && o > -1) ; ) {
          if (e > -1) {
            e = process.getErrorStream().read(buffer);
            if (e > -1) {
              err.write(buffer, 0, e);
            }
          }
          if (o > -1) {
            o = process.getInputStream().read(buffer);
            if (o > -1) {
              out.write(buffer, 0, o);
            }
          }
        }
        outString = out.toString();
        errString = out.toString();
        */
        final StreamConsumer outEater = new StreamConsumer(process.getInputStream());
        final StreamConsumer errEater = new StreamConsumer(process.getErrorStream());
        outEater.start();
        errEater.start();
        exit = process.waitFor();
        outString = outEater.getString();
        errString = errEater.getString();
        process.destroy();
        processDestroyed = true;
//        System.out.println("err: " + errString);
//        System.out.println("out: " + outString);
      } catch (InterruptedException e) {
        interrupted = true;
      } finally {
        if (!processDestroyed) {
          process.destroy();
        }
      }
      return new ExecutionResult(exit, interrupted, outString, errString, null);
    } catch (IOException e) {
      throw new ThriftCompilerException(e);
    }
  }

  public String getExecutable() {
    return executable;
  }

  private static String asString(final InputStream in) {
    try (final Scanner s = new Scanner(in)) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    }
  }

  @Override
  public boolean isNativeExecutable() {
    return true;
  }

  static class StreamConsumer extends Thread {
    final InputStream in;
    String output;
    boolean done = false;
    Throwable t;
    StreamConsumer(InputStream in) {
      this.in = in;
    }
    @Override
    public void run() {
      final byte[] buffer = new byte[1024];
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        for (int n; (n = in.read(buffer)) > -1; ) {
          out.write(buffer, 0, n);
        }
        output = out.toString();
      } catch (Throwable e) {
        t = e;
      } finally {
        done = true;
      }
    }
    public String getString() {
      while (!done) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          break;
        }
      }
      if (t != null) {
        throw new RuntimeException(t);
      } else {
        return output;
      }
    }
  }
}
