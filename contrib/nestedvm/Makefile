#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

ifeq ($(nestedvm),)
 $(error "please specify the location of the NestedVM installation")	
endif

ifeq ($(JAVA_HOME),)
	$(error "JAVA_HOME environment variable must be set and must point to a JDK")
endif

ver=0.9.4-SNAPSHOT
work=.

ifeq ($(build),)
build=$(work)/build
endif

java_cmd=$(JAVA_HOME)/bin/java
java_package_name=org.apache.thrift.compiler
java_package_fldr=org/apache/thrift/compiler

nestedvm_src=$(build)/nestedvm/src
nestedvm_bin=$(build)/nestedvm/bin
nestedvm_jar=$(build)/nestedvm/nestedvm.jar
nestedvm_pkg=$(nestedvm_src)/$(java_package_fldr)/internal/ibex

thrift=$(realpath ../..)
thrift_src=$(build)/thrift/src
thrift_bin=$(build)/thrift/classes
thrift_exe=$(build)/thrift/thrift.mips
thrift_jar=$(build)/thrift/thrift.jar
thrift_dist_jar=$(build)/ant/thrift-compiler-$(ver).jar
thrift_preverified_jar=$(build)/thrift/thrift_preverified.jar
thrift_xsd_file=$(thrift_bin)/$(java_package_fldr)/thrift-idl.xsd
thrift_libs_zip=$(thrift_bin)/$(java_package_fldr)/thrift-libs.zip
thrift_class_name=$(java_package_name).internal.Runtime
thrift_class_file=$(thrift_bin)/$(java_package_fldr)/internal/Runtime.class

proguard_ver=5.2.1
proguard_url=http://central.maven.org/maven2/net/sf/proguard/proguard-base/$(proguard_ver)/proguard-base-$(proguard_ver).jar
proguard_jar=$(build)/tools/proguard-base-$(proguard_ver).jar

usr=$(nestedvm)/upstream/install
MIPS_CC=$(usr)/bin/mips-unknown-elf-gcc
MIPS_CXX=$(usr)/bin/mips-unknown-elf-g++

# Be VERY careful about changing any of these as they can break binary 
# compatibility and create hard to find bugs
MIPS_FLAGS=-march=mips1 -specs=$(usr)/mips-unknown-elf/lib/crt0-override.spec
MIPS_OPTFLAGS=-O3 \
    -mmemcpy \
    -ffunction-sections -fdata-sections \
    -falign-functions=512 \
    -fno-rename-registers \
    -fno-schedule-insns \
    -fno-delayed-branch

MIPS_CFLAGS=$(MIPS_OPTFLAGS) $(MIPS_FLAGS) \
    -I$(usr)/mips-unknown-elf/include/ -Wall -Wno-unused -DPATH_MAX=4096
MIPS_CXXFLAGS=$(MIPS_CFLAGS) -std=gnu++11

.PHONY: all
all: $(thrift_dist_jar)

.PHONY: clean
clean:
	rm -rf $(build)

.PHONY: install
install: $(thrift_dist_jar)
	ant mvn.install

.PHONY: libthrift
libthrift:
	ant build.lib mvn.install.lib

$(thrift_dist_jar): $(thrift_preverified_jar)
	ant package

$(thrift_preverified_jar): $(thrift_jar) $(proguard_jar) $(nestedvm_jar)
	echo "Preverifying generated java class to speed up classloading"
	$(java_cmd) -jar $(proguard_jar) \
    -injars $(thrift_jar) -target 1.7 \
    -dontobfuscate -dontoptimize -dontshrink \
    -libraryjars $(nestedvm_jar):$(JAVA_HOME)/jre/lib/rt.jar \
    -outjars $(thrift_preverified_jar)

$(proguard_jar): | $(build)/tools
	cd $(build)/tools && wget $(proguard_url)

.PHONY: nestedvm
nestedvm: $(nestedvm_jar)

.PHONY: clean-nestedvm
clean-nestedvm:
	rm -rf $(build)/nestedvm

$(nestedvm_jar): $(nestedvm_src)
	ant nestedvm-jar

$(nestedvm_src): | $(build)
	rm -rf $(nestedvm_src)
	mkdir -p $(nestedvm_pkg)
	cp -R $(nestedvm)/src/org/ibex/* $(nestedvm_pkg)
	cp -R $(nestedvm)/upstream/build/classgen/src/org/ibex/* $(nestedvm_pkg)
	find $(nestedvm_src) -name '*.java' -exec \
    sed -i 's/org\.ibex/$(java_package_name).internal.ibex/g' {} \;

$(nestedvm)/nestedvm.jar:
	(cd $(nestedvm) && make nestedvm.jar)

.PHONY: thrift
thrift: $(thrift_jar)

.PHONY: clean-thrift
clean-thrift:
	rm -rf $(build)/thrift

$(thrift_jar): $(thrift_class_file) $(thrift_xsd_file) $(thrift_libs_zip) 
	jar cf $(thrift_jar) -C $(thrift_bin) .

$(thrift_class_file): $(nestedvm_jar) $(thrift_exe) | $(thrift_bin) 
	echo "translating MIPS binary to java class"
	$(java_cmd) -cp $(nestedvm_jar) \
    org.apache.thrift.compiler.internal.ibex.nestedvm.Compiler \
    -o printStats,lessConstants,maxInsnPerMethod=2048,unixRuntime \
    -d $(thrift_bin) $(thrift_class_name) $(thrift_exe)

$(thrift_libs_zip): | $(thrift_bin)
	(cd ../.. && git archive -o contrib/nestedvm/$(thrift_libs_zip) HEAD ./lib)

$(thrift_xsd_file): | $(thrift_bin)/$(java_package_fldr)
	cp ../../lib/xml/thrift-idl.xsd $(thrift_xsd_file)

$(thrift_exe): $(thrift_src)/compiler/cpp/thrift
	cp $(thrift_src)/compiler/cpp/thrift $(thrift_exe)

$(thrift_src)/compiler/cpp/thrift: $(thrift_src)
	cd $(thrift_src) && \
	./bootstrap.sh && \
	./configure --disable-libs --host=mips-unknown-elf \
    CC="$(MIPS_CC)" CXXFLAGS="$(MIPS_CXXFLAGS)" \
    CXX="$(MIPS_CXX)" CFLAGS="$(MIPS_CFLAGS)" && \
    cd compiler/cpp && make

$(thrift_src): | $(build)
	rm -rf $(thrift_src)_tmp $(thrift_src)
	mkdir -p $(thrift_src)_tmp
	(cd ../.. && git archive HEAD .) | tar -x -C $(thrift_src)_tmp
	mv $(thrift_src)_tmp $(thrift_src)

$(thrift_bin)/$(java_package_fldr):
	mkdir -p $(thrift_bin)/$(java_package_fldr)

$(thrift_bin):
	mkdir -p $(thrift_bin)

$(classes_dir):
	mkdir -p $(classes_dir)

$(build)/tools:
	mkdir -p build/tools

$(build):
	mkdir build
