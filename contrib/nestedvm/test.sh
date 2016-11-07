#!/bin/bash

THRIFT_CMD=../../compiler/cpp/thrift
NESTED_CMD="java -jar build/ant/thrift-compiler-0.10.0.jar"
GENERATORS=(
  "as3"
  "c_glib"
  "cocoa"
  "cpp"
  "csharp"
  "d"
  "dart"
  "delphi"
  "erl"
  "go"
  "haxe"
  "hs"
  "html"
  "java:generated_annotations=undated"
  "js:jquery"
  "js:node"
  "js:ts"
  "json"
  "lua"
  "ocaml"
  "perl"
  "php"
  "py"
  "rb"
  "swift"
  "xml"
  "xsd"
)

function generate() {
  local dir=$1
  local cmd="${@:2}"
  rm -rf gen-$dir/;
  for f in ../../test/*.thrift; do
    local filename=$(basename -s.thrift $f)
    for gen in "${GENERATORS[@]}"; do
      local gendir=gen-$dir/$filename/gen-$gen
      mkdir -p $gendir;
      echo $cmd -gen $gen -out $gendir $f;
      $cmd -gen $gen -out $gendir $f;
      local status=$?
      if [ ! $status = 0 ]; then
        rm -rf $gendir/*;
        echo $status > $gendir/error; 
      fi
    done;
  done
}

generate thrift $THRIFT_CMD;
generate nested $NESTED_CMD;

