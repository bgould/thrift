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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Benjamin Gould (bcg)
 */
public class ThriftCompilerTest {

  private final ThriftCompiler compiler = ThriftCompiler.newCompiler(false);

  @Test
  public void testVersion() {
    final String version = compiler.version();
    assertTrue(
      "version string should start with 'Thrift version'",
      version.startsWith("Thrift version")
    );
  }

  @Test
  public void testHelp() {
    final String help = compiler.help();
    assertTrue(
      "help string should start with 'Usage: thrift [options] file'",
      help.startsWith("Usage: thrift [options] file")
    );
  }

  @Test
  public void testGenerate() throws XPathException, IOException {

    final File out = new File("build/ant/tests/generate");
    final File xml = new File(out, "gen-xml/tutorial.xml");
    out.mkdirs();
    if (xml.exists() && !xml.delete()) {
      throw new IllegalStateException("could not delete: " + xml);
    }

    final ExecutionResult result = compiler.execute(
        "-gen", "xml:merge",
        "-o", "build/ant/tests/generate",
        "../../tutorial/tutorial.thrift"
    );
    assertEquals("exit code should be 0", 0, result.exitCode);
    assertFalse("stdout should be empty", result.errString.length() > 0);
    assertFalse("stderr should be empty", result.errString.length() > 0);
    assertTrue("XML file should have been generated.", xml.exists());

    final Set<String> documents = new LinkedHashSet<String>();
    final XPathFactory xpathFactory = XPathFactory.newInstance();
    final String ex = "/*[local-name()='idl']/*[local-name()='document']/@name";
    final XPath xpath = xpathFactory.newXPath();
    final XPathExpression expression = xpath.compile(ex);
    try (final FileInputStream in = new FileInputStream(xml)) {
      final NodeList nodelist = (NodeList) expression.evaluate(
        new InputSource(in), XPathConstants.NODESET
      );
      for (int i = 0, c = nodelist.getLength(); i < c; i++) {
        documents.add(nodelist.item(i).getNodeValue());
      }
    }

    assertEquals("should be 2 documents", 2, documents.size());
    assertTrue(documents.contains("tutorial"));
    assertTrue(documents.contains("shared"));

  }

  @Test
  public void testNativeProperties() {

    final Properties props = new Properties();
    props.setProperty("thrift.compiler.native", "true");
    props.setProperty("thrift.compiler.executable", "/fake/test/thrift");

    final ThriftCompiler compiler = ThriftCompiler.newCompiler(props);
    assertTrue("should be native", compiler instanceof NativeThriftCompiler);
    assertTrue("isNativeExecutable == true", compiler.isNativeExecutable());
    assertEquals("custom executable path should be set", 
      ((NativeThriftCompiler) compiler).getExecutable(), "/fake/test/thrift");

  }

  @Test
  public void testNestedVmWindowsArgs() {
    final String[] inputs = new String[] {
      "-debug",
      "-o", "A:\\test\\o",
      "-I", "Z:/test/I",
      "-I", "include",
      "-I", "@:\\test\\offbyone",
      "-I", "[:\\test\\offbyone",
      "-I", "`:\\test\\offbyone",
      "-I", "{:\\test\\offbyone",
      "-out", "a:/test\\out",
      "z:\\test/test.thrift"
    };
    final String[] vm_args1 = JavaThriftCompiler.createVmArgs(inputs, true);
    int i = 0;
    assertEquals("thrift", vm_args1[i++]);
    assertEquals("-debug", vm_args1[i++]);
    assertEquals("-o", vm_args1[i++]);
    assertEquals("/cygdrive/a/test/o", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("/cygdrive/z/test/I", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("include", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("@:/test/offbyone", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("[:/test/offbyone", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("`:/test/offbyone", vm_args1[i++]);
    assertEquals("-I", vm_args1[i++]);
    assertEquals("{:/test/offbyone", vm_args1[i++]);
    assertEquals("-out", vm_args1[i++]);
    assertEquals("/cygdrive/a/test/out", vm_args1[i++]);
    assertEquals("/cygdrive/z/test/test.thrift", vm_args1[i++]);
  }
}
