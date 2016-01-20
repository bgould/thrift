package org.apache.thrift.compiler;

public class ThriftCompilerException extends RuntimeException {

  private static final long serialVersionUID = -940758464769076148L;

  public ThriftCompilerException() {
    super();
  }

  public ThriftCompilerException(String message, Throwable cause) {
    super(message, cause);
  }

  public ThriftCompilerException(String message) {
    super(message);
  }

  public ThriftCompilerException(Throwable cause) {
    super(cause);
  }

}
