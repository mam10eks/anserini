package io.anserini.index;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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

/**
 * Hello world!
 *
 */
public class App {
	private static String INDEX = "/mnt/ceph/storage/data-in-progress/kibi9872/clueweb09-small/lucene-index.cw09b.pos+docvectors+rawdocs";

	public static void main(String[] args) throws Exception {
		for (String id : documentIds()) {
			if (!documentIsInIndex(Paths.get(INDEX), id)) {
				insertDocument(id);
			}
		}
	}

	private static void insertDocument(String id) throws Exception {
		System.out.println("Insert document" + id);
		String body = documentText(id);
		IndexWriter writer = indexWriter();
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

	private static IndexWriter indexWriter() throws Exception {
		IndexArgs args = new IndexArgs();
		final Directory dir = FSDirectory.open(Paths.get(INDEX));
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

	private static Set<String> documentIds() throws Exception {
		Set<String> ret = new HashSet<>();
		for (Map<String, Map<String, String>> queryToDocToJudgment : taskToTopicToDocumentToJudgment().values()) {
			queryToDocToJudgment.values().forEach(i -> i.keySet().forEach(ret::add));
		}

		return ret;
	}

	private static Map<String, Map<String, Map<String, String>>> taskToTopicToDocumentToJudgment() throws Exception {
		return new ObjectMapper().readValue(App.class.getResourceAsStream("/clueweb.json"),
				new TypeReference<Map<String, Map<String, Map<String, String>>>>() {
				});
	}
}