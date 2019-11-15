package io.anserini.rerank.lib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;

import com.ctc.wstx.util.StringUtil;

import ciir.umass.edu.learning.DataPoint;
import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.ltr.BaseFeatureExtractor;
import io.anserini.ltr.FeatureExtractorCli.FeatureExtractionArgs;
import io.anserini.ltr.feature.FeatureExtractors;
import io.anserini.rerank.RerankerContext;
import io.anserini.search.SearchArgs;

public interface RankLibFeatureExtractor<T> {

  public DataPoint convertToDataPoint(Document doc, int docId, RerankerContext<T> context);

  public static <T> RankLibFeatureExtractor<T> fromSearchErgs(SearchArgs searchArgs) {
    List<String> featureExtractionArgs = new ArrayList<>();
    for (Map.Entry<String, String> experimentalArgument : searchArgs.experimentalArgs.entrySet()) {
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

    BaseFeatureExtractor<T> config = FeatureExtractionArgs
        .getFeatureExtractorForConfigurationPurposesFromProgramArgumentsOrFail(
            featureExtractionArgs.toArray(new String[featureExtractionArgs.size()]));

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

  public static class FeatureVectorFileRankLibFeatureExtractor<T> implements RankLibFeatureExtractor<T> {
    public FeatureVectorFileRankLibFeatureExtractor(File featureVectorFile) throws IOException {
      topicToDocumentToFeatureVector=new HashMap<>();
      List<String> featureVectors = Files.readAllLines(featureVectorFile.toPath());
      for (String featureVector : featureVectors) {
        if (shouldSkipLine(featureVector)) {
          continue;
        }
        String qid = extractQueryID(featureVector);
        String docid = extractDocID(featureVector);
        DataPoint datapoint = extractDataPoint(featureVector);
        topicToDocumentToFeatureVector.putIfAbsent(qid, new HashMap<>());
        topicToDocumentToFeatureVector.get(qid).put(docid, datapoint);
      }
    }

    private DataPoint extractDataPoint(String featureVector) {
      return new DataPoint(featureVector);
    }

    private String extractDocID(String featureVector) {
      return StringUtils.substringAfter(featureVector, "#");
    }

    private String extractQueryID(String featureVector) {
      return StringUtils.substringBetween(featureVector, "qid:", " ");
    }

    private boolean shouldSkipLine(String featureVector) {
      featureVector = featureVector.trim();
      return featureVector.isEmpty() || featureVector.startsWith("#");
    }

    // qid -> documentid -> Feature
    Map<String, Map<String, DataPoint>> topicToDocumentToFeatureVector;

    @Override
    public DataPoint convertToDataPoint(Document doc, int docId, RerankerContext<T> context) {
      if(!topicToDocumentToFeatureVector.containsKey(context.getQueryId())) {
        throw new RuntimeException("feature vector file contained no qid like this: "+context.getQueryDocId());
      }
      return topicToDocumentToFeatureVector.get(context.getQueryId()).get(doc.get(LuceneDocumentGenerator.FIELD_ID));
    }

  }

}
