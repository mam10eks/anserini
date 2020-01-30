package io.anserini.index;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bn.BengaliAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.WebisUUID;
import io.anserini.analysis.EnglishStemmingAnalyzer;
import io.anserini.analysis.TweetAnalyzer;
import io.anserini.collection.SourceDocument;
import io.anserini.index.IndexArgs;
import io.anserini.index.generator.JsoupGenerator;
import io.anserini.index.generator.LuceneDocumentGenerator;
import io.anserini.search.similarity.AccurateBM25Similarity;
import io.anserini.search.similarity.DocumentSimilarityScore;

/**
 * Hello world!
 *
 */
public class App {
	private static String INDEX_BASE_DIR = "/mnt/ceph/storage/data-in-progress/kibi9872/clueweb09-small/";

	// FIXME ADD anchor
	private static String 	FIELD_BODY = "body",
							FIELD_MAIN_CONTENT = "main-content",
							FIELD_TITLE = "title";
	
	private static List<String> FIELDS = Arrays.asList(FIELD_BODY, FIELD_MAIN_CONTENT, FIELD_TITLE);
	
	private static Map<String, String> FIELD_TO_INDSX = Map.of(
			FIELD_BODY, "lucene-index.cw09b.pos+docvectors", 
			FIELD_MAIN_CONTENT, "lucene-index.cw09b-main-content.pos+docvectors",
			FIELD_TITLE, "lucene-index.cw09b-titles.pos+docvectors");
	
	private static final Map<String, DocumentSimilarityScore> docSimilarityScores = new HashMap<>();
	
	private static String RESULT_FILE = "feature-vectors.jsonl";
	
	public static void main(String[] args) throws Exception {
		insertMissingDocuments();
		initializeSimilarityReaders();
		new ObjectMapper().writeValue(new File(RESULT_FILE), createFeatureVectors());
	}
	
	private static void initializeSimilarityReaders() throws Exception {
		for(String field : FIELDS) {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPathForField(field)));
			docSimilarityScores.put(field, new DocumentSimilarityScore(reader));
		}
	}

	static void insertMissingDocuments() throws Exception {
		for (String field : FIELDS) {
			for (String id : documentIds()) {
				if (!documentIsInIndex(indexPathForField(field), id)) {
					System.out.println("Index document " + id + " to field " + field);
//					insertDocument(id);
				} else {
					System.out.println("Document already indexed: " + id);
				}
			}
		}
	}

	private static void insertDocument(String id) throws Exception {
		System.out.println("Insert document" + id);
		String body = documentText(id);
		IndexWriter writer = indexWriter(null);
		JsoupGenerator s = new JsoupGenerator();
		@SuppressWarnings("unchecked")
		Document doc = s.createDocument(new SourceDocument() {
			@Override
			public boolean indexable() {
				return true;
			}
			
			@Override
			public String id() {
				return id;
			}
			
			@Override
			public String content() {
				return body;
			}
		});
		
		writer.addDocument(doc);
		
		writer.close();
	}

	private static IndexWriter indexWriter(String field) throws Exception {
		IndexArgs args = new IndexArgs();
		final Directory dir = FSDirectory.open(indexPathForField(field));
		final CJKAnalyzer chineseAnalyzer = new CJKAnalyzer();
		final ArabicAnalyzer arabicAnalyzer = new ArabicAnalyzer();
		final FrenchAnalyzer frenchAnalyzer = new FrenchAnalyzer();
		final HindiAnalyzer hindiAnalyzer = new HindiAnalyzer();
		final BengaliAnalyzer bengaliAnalyzer = new BengaliAnalyzer();
		final GermanAnalyzer germanAnalyzer = new GermanAnalyzer();
		final SpanishAnalyzer spanishAnalyzer = new SpanishAnalyzer();
		final EnglishStemmingAnalyzer analyzer = args.keepStopwords
				? new EnglishStemmingAnalyzer(args.stemmer, CharArraySet.EMPTY_SET)
				: new EnglishStemmingAnalyzer(args.stemmer);
		final TweetAnalyzer tweetAnalyzer = new TweetAnalyzer(args.tweetStemming);

		final IndexWriterConfig config;
		if (args.collectionClass.equals("TweetCollection")) {
			config = new IndexWriterConfig(tweetAnalyzer);
		} else if (args.language.equals("zh")) {
			config = new IndexWriterConfig(chineseAnalyzer);
		} else if (args.language.equals("ar")) {
			config = new IndexWriterConfig(arabicAnalyzer);
		} else if (args.language.equals("fr")) {
			config = new IndexWriterConfig(frenchAnalyzer);
		} else if (args.language.equals("hi")) {
			config = new IndexWriterConfig(hindiAnalyzer);
		} else if (args.language.equals("bn")) {
			config = new IndexWriterConfig(bengaliAnalyzer);
		} else if (args.language.equals("de")) {
			config = new IndexWriterConfig(germanAnalyzer);
		} else if (args.language.equals("es")) {
			config = new IndexWriterConfig(spanishAnalyzer);
		} else {
			config = new IndexWriterConfig(analyzer);
		}
		if (args.bm25Accurate) {
			config.setSimilarity(new AccurateBM25Similarity()); // necessary during indexing as the norm used in BM25 is
																// already determined at index time.
		} else {
			config.setSimilarity(new BM25Similarity());
		}
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		config.setRAMBufferSizeMB(args.memorybufferSize);
		config.setUseCompoundFile(false);
		config.setMergeScheduler(new ConcurrentMergeScheduler());

		return new IndexWriter(dir, config);
	}

	private static String documentText(String id) throws Exception {
		String chatNoirUrl = "https://chatnoir.eu/cache?uuid=" + new WebisUUID("clueweb09").generateUUID(id).toString()
				+ "&index=cw09&raw";
		return IOUtils.toString(new URL(chatNoirUrl), StandardCharsets.UTF_8);
	}

	private static boolean documentIsInIndex(Path p, String id) throws Exception {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(p));
		IndexSearcher searcher = new IndexSearcher(reader);
		ScoreDoc[] scoreDocs = searcher.search(new TermQuery(new Term(LuceneDocumentGenerator.FIELD_ID, id)),
				10).scoreDocs;

		if (scoreDocs.length == 0) {
			return Boolean.FALSE;
		}

		Document doc = searcher.doc(scoreDocs[0].doc);
		String actualId = doc.get(LuceneDocumentGenerator.FIELD_ID);

		if (id == null || !id.equals(actualId)) {
			throw new RuntimeException("FIX this");
		}

		return Boolean.TRUE;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Set<String> documentIds() throws Exception {
		Set<String> ret = new HashSet<>();
		for (Map<String, Map<String, Object>> queryToDocToJudgment : taskToTopicToDocumentToJudgment().values()) {
			for(Map<String, Object> a: queryToDocToJudgment.values()) {
				Object bla = ((Map) a).get("documentToJudgment");
				Map<String, String> docsToJudgments = (Map) bla;
				ret.addAll(docsToJudgments.keySet());
			}
		}

		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	private static Map<String, Map<String, Map<String, Float>>> createFeatureVectors() throws Exception {
		Map<String, Map<String, Map<String, Float>>> ret = new HashMap<>();
		
		for (Map<String, Map<String, Object>> queryToDocToJudgment : taskToTopicToDocumentToJudgment().values()) {
			for(Map<String, Object> a: queryToDocToJudgment.values()) {
				String topicNumber = (String) a.get("topicNumber");
				String topicQuery = (String) a.get("topicQuery");
				if(ret.containsKey(topicNumber)) {
					throw new RuntimeException("FIX THIS");
				}
				Map<String, Map<String, Float>> results = new HashMap<>();
				Map<String, String> documentToJudgments = (Map)(a.get("documentToJudgment"));
				for(String docId: documentToJudgments.keySet()) {
					results.put(docId, calculateFeaturesForDocument(topicQuery, docId));
				}
				
				ret.put(topicNumber, results);
			}
		}
		
		return ret;
	}
	
	private static Map<String, Float> calculateFeaturesForDocument(String query, String documentId) throws Exception {
		Map<String, Float> ret = new HashMap<>();
		System.out.println("Calculate scores for query '" + query + "' for doc '" + documentId + "'.");
		
		for(String field : FIELDS) {
			ret.putAll(calculateFeaturesForDocumentOnField(query, documentId, field));
		}
		
		return ret;
	}
	
	private static Map<String, Float> calculateFeaturesForDocumentOnField(String query, String documentId, String field) throws Exception {
		DocumentSimilarityScore sim = docSimilarityScores.get(field);
		Map<String, Float> ret = new HashMap<>();
		ret.putAll(Map.of(
			field + "-bm25-similarity", sim.bm25Similarity(query, documentId),
			field + "-f2exp-similarity", sim.f2expSimilarity(query, documentId),
			field + "-spl-similarity", sim.splSimilarity(query, documentId),
			field + "-pl2-similarity", sim.pl2Similarity(query, documentId),
			field + "-ql-similarity", sim.qlSimilarity(query, documentId),
			field + "-qljm-similarity", sim.qljmSimilarity(query, documentId),
			field + "-f2log-similarity", sim.f2logSimilarity(query, documentId),
			field + "-tf-idf-similarity", sim.tfIdfSimilarity(query, documentId),
			field + "-tf-similarity", sim.tfSimilarity(query, documentId)
		));
		
//		ret.putAll(Map.of(
//			field + "-bm25-similarity-rm3", sim.bm25SimilarityRm3(query, documentId),
//			field + "-spl-similarity-rm3", sim.splSimilarityRm3(query, documentId),
//			field + "-pl2-similarity-rm3", sim.pl2SimilarityRm3(query, documentId),
//			field + "-f2exp-similarity-rm3", sim.f2expSimilarityRm3(query, documentId),
//			field + "-ql-similarity-rm3", sim.qlSimilarityRm3(query, documentId),
//			field + "-f2log-similarity-rm3", sim.f2logSimilarityRm3(query, documentId),
//			field + "-tf-idf-similarity-rm3", sim.tfIdfSimilarityRm3(query, documentId),
//			field + "-qljm-similarity-rm3", sim.qljmSimilarityRm3(query, documentId),
//			field + "-tf-similarity-rm3", sim.tfSimilarityRm3(query, documentId)
//		));
		
		return ret;
	}
	
	private static Map<String, Map<String, Map<String, Object>>> taskToTopicToDocumentToJudgment() throws Exception {
		return new ObjectMapper().readValue(App.class.getResourceAsStream("/clueweb.json"),
				new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {
				});
	}
	
	private static Path indexPathForField(String field) {
		return Paths.get(INDEX_BASE_DIR + FIELD_TO_INDSX.get(field));
	}
}