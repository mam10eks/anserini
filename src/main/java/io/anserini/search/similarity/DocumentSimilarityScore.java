package io.anserini.search.similarity;

import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_BODY;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
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
import org.apache.lucene.store.FSDirectory;

import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.lib.Rm3Reranker;
import io.anserini.search.SearchArgs;
import io.anserini.search.query.BagOfWordsQueryGenerator;

public class DocumentSimilarityScore {

  private final IndexReader indexReader;
  
  private final SearchArgs args;
  
  public DocumentSimilarityScore(IndexReader indexReader) {
    this.indexReader = indexReader;
    this.args = new SearchArgs();
  }

  public float tfSimilarity(String query, String documentId) {
    return calculate(new TfSimilarity(), query, documentId);
  }
  
  public float tfSimilarityRm3(String query, String documentId) {
    return calculateWithRm3(new TfSimilarity(), query, documentId);
  }
  
  public float tfIdfSimilarity(String query, String documentId) {
    return calculate(new ClassicSimilarity(), query, documentId);
  }
  
  public float tfIdfSimilarityRm3(String query, String documentId) {
    return calculateWithRm3(new ClassicSimilarity(), query, documentId);
  }

  public float bm25Similarity(String query, String documentId) {
    return calculate(bm25(), query, documentId);
  }

  public float bm25SimilarityRm3(String query, String documentId) {
    return calculateWithRm3(bm25(), query, documentId);
  }
  
  private BM25Similarity bm25() {
    return new BM25Similarity(uniqueFloat(args.k1), uniqueFloat(args.b));
  }

  public float pl2Similarity(String query, String documentId) {
    return calculate(pl2(), query, documentId);
  }
  
  public float pl2SimilarityRm3(String query, String documentId) {
    return calculateWithRm3(pl2(), query, documentId);
  }
  
  private DFRSimilarity pl2() {
    return new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH2(uniqueFloat(args.pl2_c)));
  }

  public float qlSimilarity(String query, String documentId) {
    return calculate(ql(), query, documentId);
  }

  public float qlSimilarityRm3(String query, String documentId) {
    return calculateWithRm3(ql(), query, documentId);
  }
  
  private LMDirichletSimilarity ql() {
    return new LMDirichletSimilarity(uniqueFloat(args.mu));
  }

  public float calculateWithRm3(Similarity similarity, String query, String documentId) {
    Rm3Reranker reranker = reranker();
    IndexSearcher searcher = searcher(similarity, indexReader);
    Query feedbackQuery = reranker.feedbackQuery(similarityInBody(query), rerankerContext(searcher, query));

    return calculate(searcher, feedbackQuery, documentId);
  }
  
  public float calculate(Similarity similarity, String query, String documentId) {
    return calculate(searcher(similarity, indexReader), query, documentId);
  }
  
  public float calculate(IndexSearcher searcher, String query, String documentId) {
    return calculate(searcher, similarityInBody(query), documentId);
  }

  public static float calculate(IndexSearcher searcher, Query query, String documentId) {
    BooleanQuery scoreForDoc = new BooleanQuery.Builder()
      .add(idIs(documentId), BooleanClause.Occur.FILTER)
      .add(query, BooleanClause.Occur.MUST)
      .build();

    return retrieveScoreOrFail(searcher, scoreForDoc, documentId);
  }

  private static IndexSearcher searcher(Similarity similarity, IndexReader reader) {
    IndexSearcher ret = new IndexSearcher(reader);
    ret.setSimilarity(similarity);
    
    return ret;
  }

  private static float retrieveScoreOrFail(IndexSearcher searcher, Query query, String docId) {
    try {
      return retrieveScore(searcher, query, docId);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static float retrieveScore(IndexSearcher searcher, Query query, String docId) throws IOException {
    TopDocs ret = searcher.search(query, 10);

    if(ret.scoreDocs.length == 0) {
      return 0;
    } else if (ret.scoreDocs.length == 1) {
      return ret.scoreDocs[0].score;
    }
    
    Document firstDoc = searcher.getIndexReader().document(ret.scoreDocs[0].doc);
    String actualId = firstDoc.get(LuceneDocumentGenerator.FIELD_ID);

    if(docId == null || !docId.equals(actualId)) {
      throw new RuntimeException("I expected a document with id '" + docId + "', but got '" + actualId + "'.");
    }

    return ret.scoreDocs[0].score;
  }
  
  private static Query idIs(String documentId) {
    return new TermQuery(new Term(LuceneDocumentGenerator.FIELD_ID, documentId));
  }

  private Query similarityInBody(String queryString) {
    return new BagOfWordsQueryGenerator().buildQuery(FIELD_BODY, args.analyzer(), queryString);
  }
  
  private Rm3Reranker reranker() {
    int fbTerms = uniqueInt(args.rm3_fbTerms);
    int fbDocs = uniqueInt(args.rm3_fbDocs);
    float originalQueryWeight = uniqueFloat(args.rm3_originalQueryWeight);
    
    return new Rm3Reranker(args.analyzer(), LuceneDocumentGenerator.FIELD_BODY, fbTerms, fbDocs, originalQueryWeight, args.rm3_outputQuery);
  }
  
  private RerankerContext<?> rerankerContext(IndexSearcher searcher, String query) {
    try {
      return new RerankerContext<>(searcher, null, null, null, query, null, null, args);
	} catch (IOException e) {
      throw new RuntimeException(e);
	}
  }
  
  private static int uniqueInt(String[] choices) {
    return Integer.valueOf(uniqueElementOfFail(choices));
  }
  
  private static float uniqueFloat(String[] choices) {
    return Float.valueOf(uniqueElementOfFail(choices));
  }
  
  private static String uniqueElementOfFail(String[] choices) {
    if(choices.length != 1) {
      throw new RuntimeException("Cant extract unique element from array of length " + choices.length);
    }
    
    return choices[0];
  }
  
  public static void main(String[] args) throws Exception {
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("/home/maik/workspace/web-search-axiomatic-reranking/axiomatic-explainability/experiments/robust04/lucene-index.robust04.pos+docvectors+rawdocs+transformedDocs")));

    System.out.println(new DocumentSimilarityScore(reader).bm25Similarity("intern organ crime", "FBIS3-1638"));
  }
}
