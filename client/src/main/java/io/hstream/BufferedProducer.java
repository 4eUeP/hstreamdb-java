package io.hstream;

import java.io.Closeable;

/** The interface for the HStream BuffetedProducer. */
public interface BufferedProducer extends Producer, Closeable {

  /** explicitly flush buffered records. */
  public void flush();

  /** closes the buffered producer, will call flush() first */
  public void close();
}
