Pure Java build of the Thrift compiler based on NestedVM
========================================================

This module builds the Thrift compiler as a pure Java JAR file that can be run
as a command line program or called from a Java application.

To build, you will need a working [NestedVM][1] installation.  The official 
NestedVM repository hosted by Adam Megacz will not work for this build; the 
toolchain includes an old version of GCC that is unusable for this purpose.  

Fortunately a kind hacker named Henry Wertz has made available some patches for
NestedVM that make it [possible to build with a modern version of GCC][2].

At the moment perhaps the easiest way to grab a suitable NestedVM source tree
is to clone the repository below, which includes Henry Wertz's patches as well
as David Ehrmann's fixes for the UnixRuntime library that are necessary for 
Thrift to compile and run properly (importantly, proper handling the .rel.dyn 
section in Thrift's ELF file as well as bug fix for the realpath syscall).

    cd /usr/local/src
    git clone https://github.com/bgould/nestedvm
    cd nestedvm
    make cxxtest
    # grab a beverage and make a sandwich, the build will take a little while

Going forward the instructions will assume you have NestedVM installed at
`/usr/local/src/nestedvm`.  If you want to put it someplace else that is fine,
just substitute that path in the following the build steps.

Once you have a working installation of NestedVM, you may want to make sure
that you are able to build the Thrift compiler normally, perhaps like this:

    cd /usr/local/src/thrift && ./configure --disable-libs && make

If that works then you should be able to build the compiler with NestedVM.  If
it does not, refer to [Building From Source][3] to ensure that you have all of
the necessary prerequisites installed and configured.

Make sure that you set environment variables for your NestedVM and Java tools,
and then run the make file:

    export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
    export nestedvm=/usr/local/src/nestedvm
    cd /usr/local/src/thrift
    make

If/when that completes successfully, you can test the build:

    java -jar contrib/nestedvm/build/thrift-compiler-0.9.4-SNAPSHOT.jar -help

If you want to include the compiler in your Java application you can just add
the JAR file or if you use Maven you could add the dependency to your local
repository after building:

    make install

Once it is in you local repository you can add it to your Maven project:

    <dependency>
      <groupId>org.apache.thrift</groupId>
      <artifactId>thrift-compiler</artifactId>
      <version>0.9.4-SNAPSHOT</version>
    </dependency>

For an example of how to invoke the compiler from a Java program, you can
take a look at the [`ThriftCompilerTest`][5] class.

[1]: http://nestedvm.ibex.org/
[2]: https://lists.hcoop.net/pipermail/nestedvm/2014-September/000151.html
[3]: http://thrift.apache.org/docs/BuildingFromSource
[4]: http://maven.apache.org
[5]: src/test/java/org/apache/thrift/compiler/ThriftCompilerTest.java
