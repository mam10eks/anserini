package io.anserini.search.similarity;

import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_BODY;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelIn;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.Similarity;

import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.lib.Rm3Reranker;
import io.anserini.search.SearchArgs;
import io.anserini.search.query.BagOfWordsQueryGenerator;

public class DocumentSimilarityScore {

  public static float tfSimilarity(IndexReader indexReader, String query, String documentId) {
    return calculate(new TfSimilarity(), indexReader, query, documentId);
  }
  
  public static float tfSimilarityRm3(IndexReader indexReader, String query, String documentId) {
    Rm3Reranker reranker = reranker();
    IndexSearcher searcher = searcher(new TfSimilarity(), indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }
  
  public static float tfIdfSimilarity(IndexReader indexReader, String query, String documentId) {
    return calculate(new ClassicSimilarity(), indexReader, query, documentId);
  }
  
  public static float tfIdfSimilarityRm3(IndexReader indexReader, String query, String documentId) {
    Rm3Reranker reranker = reranker();
    IndexSearcher searcher = searcher(new ClassicSimilarity(), indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }

  public static float bm25Similarity(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    float k1 = Float.valueOf(args.k1[0]);
    float b = Float.valueOf(args.b[0]);
    
    return calculate(new BM25Similarity(k1, b), indexReader, query, documentId);
  }

  public static float bm25SimilarityRm3(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    Rm3Reranker reranker = reranker();
    float k1 = Float.valueOf(args.k1[0]);
    float b = Float.valueOf(args.b[0]);
    IndexSearcher searcher = searcher(new BM25Similarity(k1, b), indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }

  public static float pl2Similarity(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    float c = Float.valueOf(args.inl2_c[0]);

    return calculate(new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH2(c)), indexReader, query, documentId);
  }
  
  public static float pl2SimilarityRm3(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    float c = Float.valueOf(args.inl2_c[0]);

    Rm3Reranker reranker = reranker();
    IndexSearcher searcher = searcher(new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH2(c)), indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }

  public static float qlSimilarity(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    float mu = Float.valueOf(args.mu[0]);

    return calculate(new LMDirichletSimilarity(mu), indexReader, query, documentId);
  }

  public static float qlSimilarityRm3(IndexReader indexReader, String query, String documentId) {
    SearchArgs args = new SearchArgs();
    float mu = Float.valueOf(args.mu[0]);

    Rm3Reranker reranker = reranker();
    IndexSearcher searcher = searcher(new LMDirichletSimilarity(mu), indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }

  public static float calculate(Similarity similarity, IndexReader indexReader, String query, String documentId) {
    return calculate(searcher(similarity, indexReader), query, documentId);
  }
  
  public static float calculate(IndexSearcher searcher, String query, String documentId) {
    return calculate(searcher, similarityInBody(query), documentId);
  }

  public static float calculate(IndexSearcher searcher, Query query, String documentId) {
    BooleanQuery scoreForDoc = new BooleanQuery.Builder()
      .add(idIs(documentId), BooleanClause.Occur.FILTER)
      .add(query, BooleanClause.Occur.MUST)
      .build();

    return retrieveScoreOrFail(searcher, scoreForDoc);
  }

  private static IndexSearcher searcher(Similarity similarity, IndexReader reader) {
    IndexSearcher ret = new IndexSearcher(reader);
    ret.setSimilarity(similarity);
    
    return ret;
  }

  private static float retrieveScoreOrFail(IndexSearcher searcher, Query query) {
    try {
      return retrieveScore(searcher, query);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static float retrieveScore(IndexSearcher searcher, Query query) throws IOException {
    TopDocs ret = searcher.search(query, 10);

    if(ret.scoreDocs.length == 0) {
      return 0;
    }

    if(ret.scoreDocs.length != 1) {
      throw new RuntimeException("Fix this " + ret.scoreDocs.length);
    }

    return ret.scoreDocs[0].score;
  }
  
  private static Query idIs(String documentId) {
    return new TermQuery(new Term(LuceneDocumentGenerator.FIELD_ID, documentId));
  }

  private static Query similarityInBody(String queryString) {
    return new BagOfWordsQueryGenerator().buildQuery(FIELD_BODY, new SearchArgs().analyzer(), queryString);
  }
  
  private static Rm3Reranker reranker() {
    SearchArgs args = new SearchArgs();
    int fbTerms = Integer.valueOf(args.rm3_fbTerms[0]);
    int fbDocs = Integer.valueOf(args.rm3_fbDocs[0]);
    float originalQueryWeight = Float.valueOf(args.rm3_originalQueryWeight[0]);
    
    return new Rm3Reranker(args.analyzer(), LuceneDocumentGenerator.FIELD_BODY, fbTerms, fbDocs, originalQueryWeight, args.rm3_outputQuery);
  }
  
  private static RerankerContext<?> rerankerContext(IndexSearcher searcher, String query) {
    try {
      return new RerankerContext<>(searcher, null, null, null, query, null, null, new SearchArgs());
	} catch (IOException e) {
      throw new RuntimeException(e);
	}
  }
}
