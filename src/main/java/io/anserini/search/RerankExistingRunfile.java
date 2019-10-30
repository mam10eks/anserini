package io.anserini.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.mockito.Matchers;
import org.mockito.Mockito;

import io.anserini.eval.RankingResults;
import io.anserini.eval.ResultDoc;
import io.anserini.rerank.RerankerCascade;
import io.anserini.search.SearchCollection.SearcherThread;
import io.anserini.search.similarity.TaggedSimilarity;
import io.anserini.search.topicreader.TopicReader;

public class RerankExistingRunfile {

	public static void main(String[] args) throws IOException {
		IndexSearcher mocked = Mockito.mock(IndexSearcher.class);

		RankingResults rr = new RankingResults(new TreeMap<String, List<ResultDoc>>());
		Map<String, List<ResultDoc>> run = rr.readResultsFile(
				"/home/lukas/git/TREC-combining-all-results/data/trec-19-web-run-files/trec19/web/input.blv79y00prob.gz",
				false, false);

		Mockito.when(mocked.search(Matchers.any(), Matchers.anyInt())).then(i -> {
			return search(i.getArgumentAt(0, Query.class), i.getArgumentAt(1, Integer.class), run);
		});

		SearchArgs sargs = new SearchArgs();
		sargs.index = "";
		sargs.topics = new String[] { "/home/lukas/git/TREC-combining-all-results/third-party/anserini/src/main/resources/topics-and-qrels/topics.51-100.txt" };
		sargs.output = "";
		sargs.topicReader = "Trec";
		sargs.bm25 = true;
		sargs.arbitraryScoreTieBreak = true;
		@SuppressWarnings("resource")
		SearchCollection s = new SearchCollection(sargs);
		SortedMap<Object, Map<String, String>> topics = readTopicFiles(sargs);
		
		List<TaggedSimilarity> similarities = s.constructSimiliries();
		Map<String, RerankerCascade> cascades = s.constructRerankerCascades();
		System.out.println(cascades.values().iterator().next());
		System.out.println(similarities);
		SearcherThread<Object> k = s.new SearcherThread<Object>(mocked, topics, similarities.get(0), "cascadeTag",
				cascades.values().iterator().next(), "outputPath", "runTag");

		k.run();
	}

	public static TopDocs search(Query query, int n, Map<String, List<ResultDoc>> run) {
		List<ResultDoc> results = run.get(query + "");
		int len = Math.max(results.size(), n);
		ScoreDoc[] scoreDocs = new ScoreDoc[len];
		for (int i = 0; i < len; i++) {
			scoreDocs[i] = new ScoreDoc(Integer.parseInt(query + ""), (float) results.get(i).getScore());
		}
		return new TopDocs(new TotalHits(len, Relation.EQUAL_TO), scoreDocs);
	}
	public static <K>SortedMap<K, Map<String, String>> readTopicFiles(SearchArgs args) {
		TopicReader<K> tr;
		SortedMap<K, Map<String, String>> topics = new TreeMap<>();
		for (String singleTopicsFile : args.topics) {
			Path topicsFilePath = Paths.get(singleTopicsFile);
			if (!Files.exists(topicsFilePath) || !Files.isRegularFile(topicsFilePath)
					|| !Files.isReadable(topicsFilePath)) {
				throw new IllegalArgumentException(
						"Topics file : " + topicsFilePath + " does not exist or is not a (readable) file.");
			}
			try {
				tr = (TopicReader<K>) Class
						.forName("io.anserini.search.topicreader." + args.topicReader + "TopicReader")
						.getConstructor(Path.class).newInstance(topicsFilePath);
				topics.putAll(tr.read());
			} catch (Exception e) {
				throw new IllegalArgumentException("Unable to load topic reader: " + args.topicReader);
			}
		}
		return topics;
	}
}
