package org.molgenis.armadillo.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import org.molgenis.armadillo.storage.JarDownloader;

/**
 * Test double for {@link JarDownloader}. Records every invocation so tests can assert on what was
 * requested, and optionally simulates writing a file and/or reporting progress, so behavior can be
 * configured per test without touching the real network.
 */
public class RecordingJarDownloader implements JarDownloader {

  private final List<Call> calls = new ArrayList<>();

  /** Content to write to outputFile on each call. Null means "don't write anything". */
  private String contentToWrite = "Hello world!!";

  /** Progress values to report before completing, e.g. List.of(50L, 100L). */
  private List<Long> progressToReport = List.of(100L);

  /** If set, this exception is thrown instead of doing anything else. */
  private InterruptedException exceptionToThrow;

  @Override
  public void downloadFile(String url, String outputFile) throws InterruptedException {
    downloadFile(url, outputFile, progress -> {});
  }

  @Override
  public void downloadFile(String url, String outputFile, LongConsumer progressCallback)
      throws InterruptedException {
    calls.add(new Call(url, outputFile));

    if (exceptionToThrow != null) {
      throw exceptionToThrow;
    }

    if (contentToWrite != null) {
      try {
        Files.writeString(Path.of(outputFile), contentToWrite);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    progressToReport.forEach(progressCallback::accept);
  }

  public void throwInterruptedException() {
    this.exceptionToThrow = new InterruptedException("simulated interruption");
  }

  public void withContent(String content) {
    this.contentToWrite = content;
  }

  public void withProgress(List<Long> progress) {
    this.progressToReport = progress;
  }

  public List<Call> getCalls() {
    return List.copyOf(calls);
  }

  public boolean wasCalled() {
    return !calls.isEmpty();
  }

  public record Call(String url, String outputFile) {}
}
