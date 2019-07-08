package io.anserini.rerank.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;

import ciir.umass.edu.learning.DataPoint;
import io.anserini.ltr.BaseFeatureExtractor;
import io.anserini.ltr.FeatureExtractorCli.FeatureExtractionArgs;
import io.anserini.ltr.feature.FeatureExtractors;
import io.anserini.rerank.RerankerContext;
import io.anserini.search.SearchArgs;

public interface RankLibFeatureExtractor<T> {

  public DataPoint convertToDataPoint(Document doc, int docId, RerankerContext<T> context);

  public static <T> RankLibFeatureExtractor<T> fromSearchErgs(SearchArgs searchArgs) {
    List<String> featureExtractionArgs = new ArrayList<>();
    for(Map.Entry<String, String> experimentalArgument : searchArgs.experimentalArgs.entrySet()) {
      featureExtractionArgs.add(experimentalArgument.getKey());
      featureExtractionArgs.add(experimentalArgument.getValue());
    }

    featureExtractionArgs.add("-out");
    featureExtractionArgs.add("PSEUDO-OUT-INJECTED-IN-SEARCH-ARGS");
    featureExtractionArgs.add("-index");
    featureExtractionArgs.add("PSEUDO-OUT-INJECTED-IN-SEARCH-ARGS");
    featureExtractionArgs.add("-qrel");
    featureExtractionArgs.add("PSEUDO-QREL-INJECTED-IN-SEARCH-ARGS");
    featureExtractionArgs.add("-topic");
    featureExtractionArgs.add("PSEUDO-TOPIC-INJECTED-IN-SEARCH-ARGS");

    BaseFeatureExtractor<T> config = FeatureExtractionArgs.getFeatureExtractorForConfigurationPurposesFromProgramArgumentsOrFail(featureExtractionArgs.toArray(new String[featureExtractionArgs.size()]));

    return new IndexReaderRankLibFeatureExtractor<>(config.getTermVectorField(), config.getExtractors());
  }  

  public static class IndexReaderRankLibFeatureExtractor<T> implements RankLibFeatureExtractor<T> {
    public final String termsField;
    public final FeatureExtractors extractors;

    public IndexReaderRankLibFeatureExtractor(String termsField, FeatureExtractors extractors) {
      this.termsField = termsField;
      this.extractors = extractors;
    }

    public DataPoint convertToDataPoint(Document doc, int docId, RerankerContext<T> context) {
      Terms terms = extractTermsForDocumendIdOrFail(docId, context);

      float[] features = this.extractors.extractAll(doc, terms, context);
      String rankLibEntryString = BaseFeatureExtractor.constructOutputString("0", 0, "0", features);

      return new DataPoint(rankLibEntryString); 
    }

    private Terms extractTermsForDocumendIdOrFail(int docId, RerankerContext<T> context) {
      try {
        return context.getIndexSearcher().getIndexReader().getTermVector(docId, this.termsField);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
