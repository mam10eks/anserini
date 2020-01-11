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
    if (searchArgs.experimentalArgs.containsKey("-rankLibFeatureVectorFile")) {
      return new FeatureVectorFileRankLibFeatureExtractor<>(
          new File(searchArgs.experimentalArgs.get("-rankLibFeatureVectorFile")));
    }

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

    // qid -> documentid -> Feature
    final Map<String, Map<String, String>> topicToDocumentToFeatureVector;
    String defaultFeatureVector = "";
    boolean sparse = false;

    public FeatureVectorFileRankLibFeatureExtractor(File featureVectorFile) {
      this(readFeatureVectors(featureVectorFile));
    }

    public FeatureVectorFileRankLibFeatureExtractor(List<String> featureVectors) {
      topicToDocumentToFeatureVector = new HashMap<String, Map<String, String>>();

      boolean inFeatureDescriptions = false;
      for (String featureVector : featureVectors) {
        if (shouldSkip(featureVector)) {
          if (featureVector.startsWith("# Begin Feature-Descriptions")) {
            inFeatureDescriptions = true;
            sparse = true;
          }
          if (featureVector.startsWith("# End Feature-Descriptions")) {
            inFeatureDescriptions = false;
          }
          if (inFeatureDescriptions && featureVector.matches("^# \\d+: Run.*$")) {
            defaultFeatureVector += featureVector.split(" ")[1];
            defaultFeatureVector += StringUtils.chop(featureVector.split("\\(default=")[1]);
            defaultFeatureVector += " ";
          }
          continue;
        }
        String qid = extractQueryID(featureVector);
        String docid = extractDocID(featureVector);
        String datapoint = extractDataPoint(featureVector);

        topicToDocumentToFeatureVector.putIfAbsent(qid, new HashMap<>());
        topicToDocumentToFeatureVector.get(qid).put(docid, datapoint);
      }
    }

    private String extractDataPoint(String featureVector) {
      return featureVector.replaceAll("^\\d+ qid:", "0 qid:");
    }

    private String extractDocID(String featureVector) {
      String comment = StringUtils.substringAfter(featureVector, "#");
      comment = comment.trim();
      if (comment.contains("docid = ")) {
        return StringUtils.substringBetween(comment, "docid = ", " ");
      }

      return comment;
    }

    private String extractQueryID(String featureVector) {
      return StringUtils.substringBetween(featureVector, "qid:", " ");
    }

    private boolean shouldSkip(String featureVector) {
      featureVector = featureVector.trim();
      return featureVector.isEmpty() || featureVector.startsWith("#");
    }

    private static List<String> readFeatureVectors(File featureVectorFile) {
      try {
        return Files.readAllLines(featureVectorFile.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private String fillFeatureVectors(String sparse) {
      String[] sparsePairs = sparse.split("#")[0].split(" ");
      String ret = sparsePairs[0] + " " + sparsePairs[1];
      int sparseIndex = 2;
      for (String pair : defaultFeatureVector.split(" ")) {
        String sparsePair = sparsePairs[sparseIndex];
        String defaultFeatureId = pair.split(":")[0];
        String sparseFeatureId = sparsePair.split(":")[0];
        if (defaultFeatureId.equals(sparseFeatureId)) {
          ret += " " + sparsePair;
          sparseIndex++;
        } else {
          ret += " " + pair;
        }
      }
      return ret+"#"+sparse.split("#")[1];
    }

    @Override
    public DataPoint convertToDataPoint(Document doc, int docId, RerankerContext<T> context) {
      String documentId = doc.get(LuceneDocumentGenerator.FIELD_ID);
      String queryId = context.getQueryId().toString();

      if (!topicToDocumentToFeatureVector.containsKey(queryId)) {
        throw new RuntimeException("Feature vector file is missing the qid: " + queryId);
      }
      if (!topicToDocumentToFeatureVector.get(queryId).containsKey(documentId)) {
        throw new RuntimeException(
            "Feature vector file has no document '" + documentId + "' for query '" + context.getQueryId() + "'.");
      }
      String sparse_datapoint = topicToDocumentToFeatureVector.get(queryId).get(documentId);
      String filled_data;
      if (sparse) {
        filled_data = fillFeatureVectors(sparse_datapoint);
      } else {
        filled_data = sparse_datapoint;
      }
      return new DataPoint(filled_data);
    }
  }
}
