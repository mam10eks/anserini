package io.anserini.rerank.lib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class IndexReaderRankLibFeatureExtractorTest extends LuceneTestCase {
	//inspired by IndexerTest
  private Path buildTestIndexWithDocuments(List<Document> documents) throws IOException {
    Path ret = createTempDir();
    Directory dir = FSDirectory.open(ret);

    Analyzer analyzer = new EnglishAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

    IndexWriter writer = new IndexWriter(dir, config);

    FieldType textOptions = new FieldType();
    textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    textOptions.setStored(true);
    textOptions.setTokenized(true);
    textOptions.setStoreTermVectors(true);
    textOptions.setStoreTermVectorPositions(true);

    for (Document doc : documents) {
      writer.addDocument(doc);
    }

    writer.commit();
    writer.forceMerge(1);
    writer.close();

    return ret;
  }

  private Path buildTestRankLibModelFileWithContent(String content) {
    Path ret = createTempDir().resolve("ranklib-model");
    // write string to ret
    return ret;
  }
}
