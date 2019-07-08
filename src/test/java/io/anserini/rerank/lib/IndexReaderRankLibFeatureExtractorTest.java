package io.anserini.rerank.lib;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.index.IndexReader.CacheHelper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import ciir.umass.edu.learning.DataPoint;
import io.anserini.ltr.feature.FeatureExtractors;
import io.anserini.ltr.feature.base.AvgIDFFeatureExtractor;
import io.anserini.ltr.feature.base.DocSizeFeatureExtractor;
import io.anserini.ltr.feature.base.QueryLength;
import io.anserini.ltr.feature.base.SumMatchingTf;
import io.anserini.ltr.feature.base.TFIDFFeatureExtractor;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.lib.RankLibFeatureExtractor.IndexReaderRankLibFeatureExtractor;
import io.anserini.search.SearchArgs;

public class IndexReaderRankLibFeatureExtractorTest extends LuceneTestCase {
	// inspired by IndexerTest
	private Path buildTestIndexWithDocuments(String... documents) throws IOException {
		Path ret = createTempDir();
		Directory dir = FSDirectory.open(ret);

		Analyzer analyzer = new EnglishAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		IndexWriter writer = new IndexWriter(dir, config);

		FieldType textOptions = new FieldType();
		textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		textOptions.setStored(true);
		textOptions.setTokenized(true);
		textOptions.setStoreTermVectors(true);
		textOptions.setStoreTermVectorPositions(true);

		for (String doc : documents) {
			Document a = new Document();
			a.add(new Field("content", doc, textOptions));

			writer.addDocument(a);
		}

		writer.commit();
		writer.forceMerge(1);
		writer.close();

		return ret;
	}

	private Path buildTestRankLibModelFileWithContent(String content) {
		Path ret = createTempDir().resolve("ranklib-model");
		// write string to ret
		return ret;
	}

	@Test
	public <T> void testSomeFeatureExtractors() throws IOException {
		Path p = buildTestIndexWithDocuments("Its raining cats ants dogs",
				"Three quick brown foxes jump over lazy dogs", "dog dog dog dog dog dog");

		FeatureExtractors fes = new FeatureExtractors();
		fes.add(new TFIDFFeatureExtractor<String>());
		fes.add(new AvgIDFFeatureExtractor<String>());
		fes.add(new DocSizeFeatureExtractor<String>());
		fes.add(new SumMatchingTf<String>());
		fes.add(new QueryLength<String>());
		IndexReaderRankLibFeatureExtractor<T> irrlfe = new IndexReaderRankLibFeatureExtractor<>("content", fes);

		@SuppressWarnings("unchecked")
		RerankerContext<T> r = Mockito.mock(RerankerContext.class);
		Mockito.when(r.getQueryTokens()).thenReturn(Arrays.asList("dog"));
		IndexSearcher indexSearcher = null;
		indexSearcher = new IndexSearcher(StandardDirectoryReader.open(new SimpleFSDirectory(p)));

		Mockito.when(r.getIndexSearcher()).thenReturn(indexSearcher);

		DataPoint d = irrlfe.convertToDataPoint(indexSearcher.doc(0), 0, r);
		assertEquals("0.0 id:0 1:5.694401 2:2.0794415 3:5.0 4:1.0 5:1.0 # 0", d.toString());
		d = irrlfe.convertToDataPoint(indexSearcher.doc(1), 1, r);
		assertEquals("0.0 id:0 1:5.694401 2:2.0794415 3:8.0 4:1.0 5:1.0 # 0", d.toString());
		d = irrlfe.convertToDataPoint(indexSearcher.doc(2), 2, r);
		assertEquals("0.0 id:0 1:13.948377 2:2.0794415 3:6.0 4:6.0 5:1.0 # 0", d.toString());

	}
}
