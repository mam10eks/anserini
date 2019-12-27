package io.anserini.search.similarity;

import org.apache.lucene.search.similarities.ClassicSimilarity;

public class TfSimilarity extends ClassicSimilarity {
  @Override
  public float idf(long docFreq, long docCount) {
    return 1.0f;
  }
  
  @Override
  public float lengthNorm(int numTerms) {
    return 1.0f;
  }
  
  @Override
  public float tf(float freq) {
    return freq;
  }
}
