package io.anserini.rerank.lib;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import ciir.umass.edu.learning.DataPoint;
import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.rerank.RerankerContext;

public class FeatureVectorFileRankLibFeatureExtractorTest<T> extends LuceneTestCase {
  final Document ARTIFICIAL_DOCUMENT_1 = createDocumentWithId("doc-id-1");
  final Document ARTIFICIAL_DOCUMENT_2 = createDocumentWithId("doc-id-2");
  final int NON_EXISTING_DUMMY_ID = -1;

  @Test(expected = RuntimeException.class)
  public void testThatDataPointExtractionThrowExceptionIfUnknownDocumentIsInserted() throws IOException {
    RankLibFeatureExtractor<T> fvRankLibFeatureExtractor = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(
        new File("src/test/resources/artificial_small.fv"));
    fvRankLibFeatureExtractor.convertToDataPoint(ARTIFICIAL_DOCUMENT_1, NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("51"));
  }

  @Test
  public void testIfDataPointIsCorrectlyExtractedFromDocument1() throws IOException {
    RankLibFeatureExtractor<T> fvRankLibFeatureExtractor = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(
        new File("src/test/resources/artificial_small.fv"));
    DataPoint expected = new DataPoint("0.0 qid:0 1:13.948377 2:2.0794415 3:6.0 4:6.0 5:1.0 #doc-id-1");
    DataPoint actual = fvRankLibFeatureExtractor.convertToDataPoint(ARTIFICIAL_DOCUMENT_1, NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("0"));
    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testIfDataPointIsCorrectlyExtractedFromDocument2() throws IOException {
    RankLibFeatureExtractor<T> fvRankLibFeatureExtractor = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(
        new File("src/test/resources/artificial_small.fv"));
    DataPoint expected = new DataPoint("0.0 qid:0 1:5.694401 2:2.0794415 3:8.0 4:1.0 5:1.0 #doc-id-2");
    DataPoint actual = fvRankLibFeatureExtractor.convertToDataPoint(ARTIFICIAL_DOCUMENT_2, NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("0"));
    assertEquals(expected.toString(), actual.toString());
  }

  private RerankerContext<T> createContextForQueryWithId(String string) {
    @SuppressWarnings("unchecked")
    RerankerContext<T> ret=Mockito.mock(RerankerContext.class);
    Mockito.when(ret.getQueryId()).then(i->{return string;});
    return ret;
  }

  public Document createDocumentWithId(String id) {
    Document doc = new Document();
    doc.add(new StringField(LuceneDocumentGenerator.FIELD_ID, id, Field.Store.YES));
    return doc;
  }

}
