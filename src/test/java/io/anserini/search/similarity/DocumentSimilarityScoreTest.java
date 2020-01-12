package io.anserini.search.similarity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelIn;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.ltr.BaseFeatureExtractorTest;
import static io.anserini.search.similarity.TfSimilarityTest.*;

public class DocumentSimilarityScoreTest extends BaseFeatureExtractorTest<String> {

  private final Map<String, Integer> documentTextToDocumentId = new HashMap<>();
  
  private final double delta = 1e-3;
  
  @Before
  public void setup() throws IOException {
    for(String doc : TfSimilarityTest.DOCUMENTS) {
      addTestDocument(doc);
    }
  }
  
  @Test
  public void checkThatDoc1HasScoreOfThreeForQueryCatWithTfSimilarity() throws Exception {    
    float expected = 3.0f;
    float actual = sim().calculate(new TfSimilarity(), "cat", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }
  
  @Test
  public void checkThatDoc3HasScoreOfZeroForQueryRabbitWithTfSimilarity() throws Exception {
    float expected = 0.0f;
    float actual = sim().calculate(new TfSimilarity(), "rabbit", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }

  @Test
  public void checkVariousDocumentsWithTfSimilarity() {
    Assert.assertEquals(3.0f, sim().tfSimilarity("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(1.0f, sim().tfSimilarity("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().tfSimilarity("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(1.0f, sim().tfSimilarity("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void checkVariousDocumentsWithTfSimilarityWithRM3() {
    Assert.assertEquals(1.5f, sim().tfSimilarityRm3("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.5f, sim().tfSimilarityRm3("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().tfSimilarityRm3("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.5f, sim().tfSimilarityRm3("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void checkScoreOfDoc1ForQueryCatWithTfIdfSimilarity() throws Exception {    
    float expected = 0.77459f;
    float actual = sim().calculate(new ClassicSimilarity(), "cat", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }

  @Test
  public void checkThatDoc3HasScoreOfZeroForQueryRabbitWithTfIdfSimilarity() throws Exception {
    float expected = 0.0f;
    float actual = sim().calculate(new ClassicSimilarity(), "rabbit", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }
  
  @Test
  public void checkVariousDocumentsWithTfIdfSimilarity() {
    Assert.assertEquals(0.77459f, sim().tfIdfSimilarity("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.44721f, sim().tfIdfSimilarity("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().tfIdfSimilarity("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(1.06831f, sim().tfIdfSimilarity("dog", idOf(DOC_4)), delta);
  }

  @Test
  public void checkVariousDocumentsWithTfIdfSimilarityWithRM3() {
    Assert.assertEquals(0.38729f, sim().tfIdfSimilarityRm3("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.22360f, sim().tfIdfSimilarityRm3("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().tfIdfSimilarityRm3("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.53415f, sim().tfIdfSimilarityRm3("dog", idOf(DOC_4)), delta);
  }

  @Test
  public void checkScoreOfDoc1ForQueryCatWithBM25Similarity() throws Exception {    
    float expected = 0.22431f;
    float actual = sim().calculate(new BM25Similarity(21f, 1f), "cat", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }

  @Test
  public void checkThatDoc3HasScoreOfZeroForQueryRabbitWithBM25Similarity() throws Exception {
    float expected = 0.0f;
    float actual = sim().calculate(new BM25Similarity(1f, 1f), "rabbit", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }
  
  @Test
  public void checkVariousDocumentsWithBM25Similarity() {
    Assert.assertEquals(0.14939f, sim().bm25Similarity("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.09910f, sim().bm25Similarity("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().bm25Similarity("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.76038f, sim().bm25Similarity("dog", idOf(DOC_4)), delta);
  }

  @Test
  public void checkVariousDocumentsWithBM25SimilarityWithRM3() {
    Assert.assertEquals(0.07469f, sim().bm25SimilarityRm3("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.04955f, sim().bm25SimilarityRm3("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().bm25SimilarityRm3("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.38019f, sim().bm25SimilarityRm3("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void checkScoreOfDoc1ForQueryCatWithPL2Similarity() throws Exception {    
    float expected = 0.107584f;
    float actual = sim().calculate(new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH2(1.0f)), "cat", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }

  @Test
  public void checkThatDoc3HasScoreOfZeroForQueryRabbitWithPL2Similarity() throws Exception {
    float expected = 0.0f;
    float actual = sim().calculate(new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH2(1.0f)), "rabbit", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }
  
  @Test
  public void checkVariousDocumentsWithPl2Similarity() {
    Assert.assertEquals(0.036236f, sim().pl2Similarity("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.014361f, sim().pl2Similarity("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().pl2Similarity("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.198671f, sim().pl2Similarity("dog", idOf(DOC_4)), delta);
  }

  @Test
  public void checkVariousDocumentsWithPl2SimilarityRM3() {
    Assert.assertEquals(0.018118f, sim().pl2SimilarityRm3("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.007180f, sim().pl2SimilarityRm3("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().pl2SimilarityRm3("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.099335f, sim().pl2SimilarityRm3("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void checkScoreOfDoc1ForQueryCatWithQueryLikelihoodSimilarity() throws Exception {    
    float expected = 0.30040f;
    float actual = sim().calculate(new LMDirichletSimilarity(0.3f), "cat", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }

  @Test
  public void checkThatDoc3HasScoreOfZeroForQueryRabbitWithQueryLikelihoodSimilarity() throws Exception {
    float expected = 0.0f;
    float actual = sim().calculate(new LMDirichletSimilarity(0.3f), "rabbit", idOf(DOC_1));
    
    Assert.assertEquals(expected, actual, delta);
  }
  
  @Test
  public void checkVariousDocumentsWithQlSimilarity() {
    Assert.assertEquals(0.001846f, sim().qlSimilarity("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.0f, sim().qlSimilarity("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().qlSimilarity("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.001994f, sim().qlSimilarity("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void checkVariousDocumentsWithQlSimilarityRM3() {
    Assert.assertEquals(0.000923f, sim().qlSimilarityRm3("cat", idOf(DOC_1)), delta);
    Assert.assertEquals(0.0f, sim().qlSimilarityRm3("cat", idOf(DOC_2)), delta);
    Assert.assertEquals(0.0f, sim().qlSimilarityRm3("dog", idOf(DOC_3)), delta);
    Assert.assertEquals(0.000997f, sim().qlSimilarityRm3("dog", idOf(DOC_4)), delta);
  }
  
  @Test
  public void multipleSameDocumentsAreFineInCaseOfDuplicates() {
    Assert.assertEquals(0.0f, sim().qlSimilarityRm3("cat", idOf(DOC_5)), delta);
    Assert.assertEquals(0.0f, sim().qlSimilarityRm3("cat", idOf(DOC_6)), delta);
    Assert.assertEquals(0.002f, sim().qlSimilarityRm3("animal", idOf(DOC_5)), delta);
    Assert.assertEquals(0.002f, sim().qlSimilarityRm3("animal", idOf(DOC_6)), delta);
    Assert.assertEquals(0.002f, sim().qlSimilarityRm3("human animal", idOf(DOC_5)), delta);
    Assert.assertEquals(0.002f, sim().qlSimilarityRm3("human animal", idOf(DOC_6)), delta);
  }
  
  private DocumentSimilarityScore sim() {
    return new DocumentSimilarityScore(reader());
  }
  
  private IndexReader reader() {
    return makeTestContext("NOT-USED")
      .getIndexSearcher()
      .getIndexReader();
  }
 
  @Override
  protected Document addTestDocument(String testText) throws IOException {
    FieldType fieldType = new FieldType();
    fieldType.setStored(true);
    fieldType.setStoreTermVectors(true);
    fieldType.setStoreTermVectorOffsets(true);
    fieldType.setStoreTermVectorPositions(true);
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    Field field = new Field(TEST_FIELD_NAME, testText, fieldType);
    Document doc = new Document();
    doc.add(field);
    
    Field idField = new StringField(LuceneDocumentGenerator.FIELD_ID, idOf(testText), Field.Store.YES);
    doc.add(idField);
    
    testWriter.addDocument(doc);
    testWriter.commit();
    return doc;
  }
  
  private synchronized String idOf(String testText) {
    if(documentTextToDocumentId.containsKey(testText)) {
      return String.valueOf(documentTextToDocumentId.get(testText));
    }

    int id = documentTextToDocumentId.values().stream().mapToInt(i -> i).max().orElseGet(() -> 0) +1;
    documentTextToDocumentId.put(testText, id);

    return String.valueOf(id);
  }
}
