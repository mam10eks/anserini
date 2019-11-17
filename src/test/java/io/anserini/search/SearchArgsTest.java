package io.anserini.search;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.anserini.ltr.feature.FeatureExtractors;
import io.anserini.rerank.lib.RankLibFeatureExtractor.FeatureVectorFileRankLibFeatureExtractor;
import io.anserini.rerank.lib.RankLibFeatureExtractor.IndexReaderRankLibFeatureExtractor;
import io.anserini.rerank.lib.RankLibFeatureExtractor;
import io.anserini.rerank.lib.RankLibReranker;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RankLibReranker.class, SearchArgs.class})
@PowerMockIgnore("javax.management.*")
public class SearchArgsTest {

  private static final String[] PROGRAM_ARGS_WITH_DESCENDING_CLICK_RERANKER = new String[] {
    "-topicreader", "Webxml", "-index", "my-index", "-topics", "my-topic-file-1",
    "my-topic-file-2", "my-topic-file-3", "my-topic-file-4", "-output", "run_file",
    "-bm25", "-rerankCutoff", "11", "-experimental.reranker.factory",
    "de.webis.anserini_ltr.WebisRerankerCascadeFactory",
    "-experimental.args", "semicolon-seperated-files=click-session-1;click-session-2"
  };

  private static final String[] PROGRAM_ARGS_WITH_RANKLIB_RERANKER = new String[] {
    "-topicreader", "Webxml", "-index", "my-index",
    "-topics", "my-topic-file-1", "my-topic-file-2", "-output",
    "run_file", "-bm25",
    "-experimental.args", "-collection=clueweb",
    "-model", "my-trained-regression-model"
  };
  
  private static final String[] PROGRAM_ARGS_WITH_FEATURE_FILE_RANKLIB_RERANKER = new String[] {
    "-topicreader", "Webxml", "-index", "my-index",
    "-topics", "my-topic-file-1", "my-topic-file-2", "-output",
    "run_file", "-bm25",
    "-experimental.args", "-rankLibFeatureVectorFile=src/test/resources/artificial_small.fv",
    "-model", "my-trained-regression-model"
  };
  
  private static final String[] PROGRAM_ARGS_WITH_NON_EXISTING_FEATURE_FILE_RANKLIB_RERANKER = new String[] {
    "-topicreader", "Webxml", "-index", "my-index",
    "-topics", "my-topic-file-1", "my-topic-file-2", "-output",
    "run_file", "-bm25",
    "-experimental.args", "-rankLibFeatureVectorFile=non-existing-file",
    "-model", "my-trained-regression-model"
  };

  @Test
  public void testParsingOfSearchArgsWithDescendingClickReranker() throws Exception {
    SearchArgs searchArgs = SearchCollection.parseSearchArgsOrFail(PROGRAM_ARGS_WITH_DESCENDING_CLICK_RERANKER);

    Map<String, String> expectedExperimentalArguments = new HashMap<String, String>();
    expectedExperimentalArguments.put("semicolon-seperated-files", "click-session-1;click-session-2");

    Assert.assertEquals("my-index", searchArgs.index);
    Assert.assertEquals("Webxml", searchArgs.topicReader);
    Assert.assertEquals("run_file", searchArgs.output);
    Assert.assertEquals("de.webis.anserini_ltr.WebisRerankerCascadeFactory", searchArgs.experimentalRerankerFactoryClass);
    Assert.assertEquals(expectedExperimentalArguments, searchArgs.experimentalArgs);
    
    Assert.assertEquals(11, searchArgs.rerankcutoff);
    Assert.assertEquals("", searchArgs.model);
    Assert.assertArrayEquals(new String[] {"my-topic-file-1", "my-topic-file-2", "my-topic-file-3", "my-topic-file-4"}, searchArgs.topics);
  }

  @Test(expected = RuntimeException.class)
  public void expectExceptionWhenAccessingFeature() throws Exception {
    SearchArgs searchArgs = SearchCollection.parseSearchArgsOrFail(PROGRAM_ARGS_WITH_DESCENDING_CLICK_RERANKER);
    searchArgs.rankLibReranker();
  }

  @Test
  public void testParsingOfSearchArgsWithRanklibReranker() throws Exception {
    SearchArgs searchArgs = SearchCollection.parseSearchArgsOrFail(PROGRAM_ARGS_WITH_RANKLIB_RERANKER);
    Map<String, Object> rankLibArguments = new HashMap<>();
    mockRankLibRerankerCreation(rankLibArguments);

    Assert.assertEquals("my-index", searchArgs.index);
    Assert.assertEquals("Webxml", searchArgs.topicReader);
    Assert.assertEquals("run_file", searchArgs.output);
    Assert.assertEquals("my-trained-regression-model", searchArgs.model);
    Assert.assertArrayEquals(new String[] {"my-topic-file-1", "my-topic-file-2"}, searchArgs.topics);

    searchArgs.rankLibReranker();
    Assert.assertEquals("contents", rankLibArguments.get("term-field"));
    Assert.assertTrue(((FeatureExtractors) rankLibArguments.get("features")).extractors.size() > 0);
  }
  
  @Test
  public <T> void testParsingOfSearchArgsWithRanklibRerankerFromFile() throws Exception {
    SearchArgs searchArgs = SearchCollection.parseSearchArgsOrFail(PROGRAM_ARGS_WITH_FEATURE_FILE_RANKLIB_RERANKER);

    Assert.assertEquals("my-index", searchArgs.index);
    Assert.assertEquals("Webxml", searchArgs.topicReader);
    Assert.assertEquals("run_file", searchArgs.output);
    Assert.assertEquals("my-trained-regression-model", searchArgs.model);
    Assert.assertArrayEquals(new String[] {"my-topic-file-1", "my-topic-file-2"}, searchArgs.topics);

    RankLibFeatureExtractor<T> featureExtractor = RankLibFeatureExtractor.fromSearchErgs(searchArgs);

    Assert.assertTrue(featureExtractor instanceof FeatureVectorFileRankLibFeatureExtractor);
  }
  
  @Test(expected=RuntimeException.class)
  public <T> void testParsingOfSearchArgsWithRanklibRerankerFromNonExistingFile() throws Exception {
    SearchArgs searchArgs = SearchCollection.parseSearchArgsOrFail(PROGRAM_ARGS_WITH_NON_EXISTING_FEATURE_FILE_RANKLIB_RERANKER);
    RankLibFeatureExtractor.fromSearchErgs(searchArgs);
  }

  private static void mockRankLibRerankerCreation(Map<String, Object> rankLibArguments) throws Exception {
    PowerMockito.mockStatic(RankLibReranker.class);

    PowerMockito.whenNew(RankLibReranker.class)
      .withAnyArguments()
      .thenAnswer(invocationOnMock -> {
        IndexReaderRankLibFeatureExtractor<?> featureExtractor = invocationOnMock.getArgumentAt(1, IndexReaderRankLibFeatureExtractor.class);
        
        rankLibArguments.put("term-field", featureExtractor.termsField);
        rankLibArguments.put("features", featureExtractor.extractors);
        return Mockito.mock(RankLibReranker.class);
      });
  }
}
