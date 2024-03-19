package org.molgenis.armadillo.metadata;

import java.io.*;

/**
 * Reads a block or frame between 2 '\n' from given position.
 *
 * <p>It does this by seeking previous and next new lines.
 */
public class TextBlockReader {
  private RandomAccessFile raf;

  public TextBlockReader(String filePath) throws FileNotFoundException {
    this.raf = new RandomAccessFile(filePath, "r");
  }

  long[] updatePositions(long startPosition, long endPosition) throws IOException {
    long updatedStartPosition = startPosition;
    long updatedEndPosition = endPosition;

    // Seek back for first occurrence of '\n'
    raf.seek(updatedStartPosition);
    while (updatedStartPosition > 0 && raf.readByte() != '\n') {
      updatedStartPosition--;
      raf.seek(updatedStartPosition);
    }

    // Seek forward for first occurrence of '\n'
    raf.seek(updatedEndPosition);
    while (updatedEndPosition < raf.length() && raf.readByte() != '\n') {
      updatedEndPosition++;
      raf.seek(updatedEndPosition);
    }

    return new long[] {updatedStartPosition, updatedEndPosition};
  }

  public BufferedReader readBlock(long startPosition, long endPosition) throws IOException {
    long[] positions = updatePositions(startPosition, endPosition);
    long start = positions[0];
    long end = positions[1];

    InputStream is =
        new InputStream() {
          long current = start;

          @Override
          public int read() throws IOException {
            if (current < end) {
              raf.seek(current++);
              return raf.readByte();
            } else {
              return -1; // end of stream
            }
          }
        };

    // Create a BufferedReader from the InputStream
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

    return reader;
  }
}
