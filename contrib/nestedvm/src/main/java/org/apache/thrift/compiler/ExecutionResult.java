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

/**
 * Struct for holding the results of a Thrift compiler execution.
 * @author Benjamin Gould (bcg)
 */
public final class ExecutionResult {

  public final String outString;
  public final String errString;
  public final int exitCode;
  public final boolean interrupted;
  public final Throwable throwable;

  public ExecutionResult(
      int exitCode, 
      boolean interrupted, 
      String outString, 
      String errString,
      Throwable throwable) {
    this.interrupted = interrupted;
    this.outString = outString;
    this.errString = errString;
    this.exitCode = exitCode;
    this.throwable = throwable;
  }

  public boolean successful() {
    return exitCode == 0 && !interrupted && throwable == null;
  }

  public String toString() {
    return String.format(
      "ExecutionResult[exit code: %s, stderr:%s, stdout:%s, interrupted:%s]",
      exitCode, errString, outString, interrupted
    );
  }

}
