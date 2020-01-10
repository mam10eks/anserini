package io.anserini.search.similarity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.anserini.ltr.BaseFeatureExtractorTest;

public class TfSimilarityTest extends BaseFeatureExtractorTest<String> {

  public static final String
    DOC_1 = "dog cat dog cat cat",
    DOC_2 = "cat horse rabbit rat rabbit",
    DOC_3 = "cat fish horse",
    DOC_4 = "dog cat";
  
  public static List<String> DOCUMENTS = Arrays.asList(DOC_1, DOC_2, DOC_3, DOC_4);

  @Before
  public void setup() throws IOException {
    for(String doc : DOCUMENTS) {
      addTestDocument(doc);
    }
  }
  
  @Test
  public void testRetrievalOnQueryDragon() throws Exception {
    String query = "dragon";
    List<Pair<String, Float>> expected = Collections.emptyList();
    List<Pair<String, Float>> actual = searchWithTfSimilarity(query);
    
    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void testRetrievalOnQueryDog() throws Exception {
    String query = "dog";
    List<Pair<String, Float>> expected = Arrays.asList(
      Pair.of(DOC_1, 2.0f),
      Pair.of(DOC_4, 1.0f)
    );
    List<Pair<String, Float>> actual = searchWithTfSimilarity(query);
    
    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void testRetrievalOnQueryCat() throws Exception {
    String query = "cat";
    List<Pair<String, Float>> expected = Arrays.asList(
      Pair.of(DOC_1, 3.0f),
      Pair.of(DOC_2, 1.0f),
      Pair.of(DOC_3, 1.0f),
      Pair.of(DOC_4, 1.0f)
    );
    List<Pair<String, Float>> actual = searchWithTfSimilarity(query);
    
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRetrievalOnQueryRabbit() throws Exception {
    String query = "rabbit";
    List<Pair<String, Float>> expected = Arrays.asList(
      Pair.of(DOC_2, 2.0f)
    );
    List<Pair<String, Float>> actual = searchWithTfSimilarity(query);
    
    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void testRetrievalOnQueryHorse() throws Exception {
    String query = "horse";
    List<Pair<String, Float>> expected = Arrays.asList(
      Pair.of(DOC_2, 1.0f),
      Pair.of(DOC_3, 1.0f)
    );
    List<Pair<String, Float>> actual = searchWithTfSimilarity(query);
    
    Assert.assertEquals(expected, actual);
  }
  
  private List<Pair<String, Float>> searchWithTfSimilarity(String query) throws IOException, ParseException {
    IndexSearcher indexSearcher = makeTestContext(query).getIndexSearcher();
    indexSearcher.setSimilarity(new TfSimilarity());
    
    return Stream.of(indexSearcher.search(TEST_PARSER.parse(query), 100).scoreDocs)
    		.map(mapDocumentTextToScore(indexSearcher.getIndexReader()))
    		.collect(Collectors.toList());
  }
  
  private Function<ScoreDoc, Pair<String, Float>> mapDocumentTextToScore(IndexReader reader) {
    return scoreDoc -> {
      try {
        Document doc = reader.document(scoreDoc.doc);
        String docText = doc.get(BaseFeatureExtractorTest.TEST_FIELD_NAME);
          
        return Pair.of(docText, scoreDoc.score);
      }
      catch(Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
