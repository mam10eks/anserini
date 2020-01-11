package io.anserini.rerank.lib;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ciir.umass.edu.learning.DataPoint;
import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.rerank.RerankerContext;
import static org.junit.Assert.assertEquals;

public class FeatureVectorFileRankLibFeatureExtractorTest<T> {
  private static final Document ARTIFICIAL_DOCUMENT_1 = createDocumentWithId("doc-id-1");
  private static final Document ARTIFICIAL_DOCUMENT_2 = createDocumentWithId("doc-id-2");
  private static final int NON_EXISTING_DUMMY_ID = -1;

  RankLibFeatureExtractor<T> extractorOnArtificialFile;
  RankLibFeatureExtractor<T> extractorOnSparseFile;
  RankLibFeatureExtractor<T> extractorOnMQ2008Sample;
  
  @Before
  public void setUp() throws IOException {
    extractorOnArtificialFile = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(new File("src/test/resources/artificial_small.fv"));
    extractorOnSparseFile = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(new File("src/test/resources/sparse.fv"));
    extractorOnMQ2008Sample = new RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor<T>(new File("src/test/resources/million-query-2008-fold1-test-sample.fv"));
  }
  
  @Test(expected = RuntimeException.class)
  public void testThatDataPointExtractionThrowExceptionIfUnknownDocumentIsInserted() throws IOException {
    extractorOnArtificialFile.convertToDataPoint(ARTIFICIAL_DOCUMENT_1, NON_EXISTING_DUMMY_ID, createContextForQueryWithId("51"));
  }
  
  @Test(expected=RuntimeException.class)
  public void checkThatNonExistingDocumentThrowsException() throws IOException {
    Document doc = createDocumentWithId("GX182-60-2033498-DOES-NOT-EXIST");
    RerankerContext<T> context = createContextForQueryWithId("18526");

    extractorOnMQ2008Sample.convertToDataPoint(doc, NON_EXISTING_DUMMY_ID, context);
  }

  @Test
  public void checkDataPointIsExtractedAsExpectedFromDocument1() throws IOException {
    DataPoint expected = new DataPoint("0.0 qid:0 1:13.948377 2:2.0794415 3:6.0 4:6.0 5:1.0 #doc-id-1");
    DataPoint actual = extractorOnArtificialFile.convertToDataPoint(ARTIFICIAL_DOCUMENT_1, NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("0"));
    assertEquals(expected.toString(), actual.toString());
  }
  @Test
  public void checkDataPointIsExtractedAsExpectedFromSparse() throws IOException {
    DataPoint expected = new DataPoint("0.0 id:51 1:-1000.0 2:-1000.0 3:0.0 4:22.0 5:3.9999964 6:1.0  # clueweb09-en0006-61-04354");
    DataPoint actual = extractorOnSparseFile.convertToDataPoint(createDocumentWithId("clueweb09-en0006-61-04354"), NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("51"));
    assertEquals(expected.toString(), actual.toString());
    
    expected = new DataPoint("0.0 id:51 1:1.0 2:2.999997 3:1.0 4:-1000.0 5:-1000.0 6:0.0 # clueweb09-en0007-81-08143");
    actual = extractorOnSparseFile.convertToDataPoint(createDocumentWithId("clueweb09-en0007-81-08143"), NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("51"));
    assertEquals(expected.toString(), actual.toString());
  }
  @Test
  public void checkDataPointIsExtractedAsExpectedFromDocument2() throws IOException {
    DataPoint expected = new DataPoint("0.0 qid:0 1:5.694401 2:2.0794415 3:8.0 4:1.0 5:1.0 #doc-id-2");
    DataPoint actual = extractorOnArtificialFile.convertToDataPoint(ARTIFICIAL_DOCUMENT_2, NON_EXISTING_DUMMY_ID,
        createContextForQueryWithId("0"));
    assertEquals(expected.toString(), actual.toString());
  }
  
  @Test
  public void checkDataPointIsExtractedAsExpectedFromNonRelevantDocumentOnMillionQuerySample() throws IOException {
    Document doc = createDocumentWithId("GX182-60-2033498");
    RerankerContext<T> context = createContextForQueryWithId("18526");
    DataPoint expected = new DataPoint("0 qid:18526 1:0.030324 2:0.000000 3:0.166667 4:1.000000 5:0.030964 6:0.000000 7:0.000000 8:0.000000 9:0.000000 10:0.000000 11:0.561114 12:0.000000 13:0.040259 14:0.475729 15:0.565402 16:0.007323 17:0.007246 18:0.233333 19:1.000000 20:0.007652 21:0.732982 22:0.659076 23:0.663432 24:0.541498 25:0.000000 26:0.000000 27:0.000000 28:0.000000 29:0.051438 30:0.014945 31:0.001970 32:0.016081 33:0.342056 34:0.017457 35:0.103221 36:0.067241 37:0.764300 38:0.748503 39:0.623426 40:0.699408 41:0.571429 42:1.000000 43:0.000000 44:0.003559 45:0.004396 46:0.206897 #docid = GX182-60-2033498 inc = 0.0528626767080871 prob = 0.0895801");
    
    DataPoint actual = extractorOnMQ2008Sample.convertToDataPoint(doc, NON_EXISTING_DUMMY_ID, context);
    
    assertEquals(expected.toString(), actual.toString());
  }
  
  @Test
  public void checkDataPointIsExtractedAsExpectedFromRelevantDocumentOnMillionQuerySample() throws IOException {
    Document doc = createDocumentWithId("GX243-98-10056047");
    RerankerContext<T> context = createContextForQueryWithId("18985");
    DataPoint expected = new DataPoint("0 qid:18985 1:1.000000 2:0.333333 3:0.500000 4:0.000000 5:1.000000 6:0.000000 7:0.000000 8:0.000000 9:0.000000 10:0.000000 11:1.000000 12:0.325082 13:0.492167 14:0.000000 15:1.000000 16:0.345224 17:0.619048 18:0.875000 19:0.285714 20:0.346123 21:1.000000 22:1.000000 23:1.000000 24:1.000000 25:0.574203 26:1.000000 27:0.000000 28:0.000000 29:0.468452 30:0.383335 31:0.406751 32:0.442242 33:0.000000 34:0.000000 35:0.000000 36:0.000000 37:1.000000 38:1.000000 39:1.000000 40:1.000000 41:0.000000 42:0.000000 43:0.000000 44:0.006605 45:0.066667 46:0.269841 #docid = GX243-98-10056047 inc = 1 prob = 0.869399");
    
    DataPoint actual = extractorOnMQ2008Sample.convertToDataPoint(doc, NON_EXISTING_DUMMY_ID, context);
    
    assertEquals(expected.toString(), actual.toString());
  }
  
  @Test
  public void checkDataPointIsExtractedAsExpectedFromHighlyRelevantDocumentOnMillionQuerySample() throws IOException {
    Document doc = createDocumentWithId("GX256-30-9230793");
    RerankerContext<T> context = createContextForQueryWithId("19536");
    DataPoint expected = new DataPoint("0 qid:19536 1:0.751678 2:0.000000 3:0.000000 4:0.000000 5:0.751678 6:0.000000 7:0.000000 8:0.000000 9:0.000000 10:0.000000 11:0.691113 12:0.000000 13:0.000000 14:0.000000 15:0.688217 16:1.000000 17:0.061224 18:0.000000 19:0.555556 20:1.000000 21:0.772899 22:0.709202 23:0.816525 24:0.604501 25:0.000000 26:0.000000 27:0.000000 28:0.000000 29:0.000000 30:0.000000 31:0.000000 32:0.000000 33:0.000000 34:0.000000 35:0.000000 36:0.000000 37:0.774340 38:0.754891 39:0.816933 40:0.644534 41:0.800000 42:0.762712 43:0.000000 44:0.000462 45:0.000269 46:0.000000 #docid = GX256-30-9230793 inc = 0.0207931768037056 prob = 0.181138");
    
    DataPoint actual = extractorOnMQ2008Sample.convertToDataPoint(doc, NON_EXISTING_DUMMY_ID, context);
    
    assertEquals(expected.toString(), actual.toString());
  }
  
  private static <T> RerankerContext<T> createContextForQueryWithId(String string) {
    @SuppressWarnings("unchecked")
    RerankerContext<T> ret=Mockito.mock(RerankerContext.class);
    Mockito.when(ret.getQueryId()).then(i->{return string;});
    return ret;
  }

  public static Document createDocumentWithId(String id) {
    Document doc = new Document();
    doc.add(new StringField(LuceneDocumentGenerator.FIELD_ID, id, Field.Store.YES));
    return doc;
  }
}
