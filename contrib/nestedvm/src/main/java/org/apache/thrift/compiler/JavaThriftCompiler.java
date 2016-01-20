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

import org.apache.thrift.compiler.internal.Runtime;

/**
 * Implementation of {@link ThriftCompiler} that runs Thrift using NestedVM.
 * @author Benjamin Gould (bcg)
 */
public class JavaThriftCompiler extends ThriftCompiler {

  public ExecutionResult execute(String... args) {
    final Runtime compiler = new Runtime();
    final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final String[] vm_args = new String[args.length + 1];
    vm_args[0] = "thrift";
    System.arraycopy(args, 0, vm_args, 1, args.length);
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
  public boolean isNativeExecutable() {
    return false;
  }

}
