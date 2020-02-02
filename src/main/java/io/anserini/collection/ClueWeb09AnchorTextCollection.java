package io.anserini.collection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

public class ClueWeb09AnchorTextCollection extends DocumentCollection<TrecwebCollection.Document> {
  @Override
  public FileSegment<TrecwebCollection.Document> createFileSegment(Path p) throws IOException {
    return new Segment(p);
  }
  
  public static class Segment extends FileSegment<TrecwebCollection.Document> {
    private final LineIterator lineIterator;

    public Segment(Path segmentPath) {
      super(segmentPath);
      try {
        InputStream input = new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ));
        lineIterator = IOUtils.lineIterator(input, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void readNext() throws IOException, ParseException, NoSuchElementException {
      if(!lineIterator.hasNext()) {
        throw new NoSuchElementException("FIXME");
      }

      parseNextEntry(lineIterator.next());
    }

	private void parseNextEntry(String next) {
      bufferedRecord = new TrecwebCollection.Document();
      bufferedRecord.id = StringUtils.substringBefore(next, "\t");
      bufferedRecord.content = StringUtils.substringAfter(next, "\t").trim();
	}
  }
}

