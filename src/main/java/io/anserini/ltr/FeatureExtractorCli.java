/**
 * Anserini: A Lucene toolkit for replicable information retrieval research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.ltr;

import io.anserini.ltr.feature.FeatureExtractors;
import io.anserini.search.topicreader.MicroblogTopicReader;
import io.anserini.search.topicreader.TopicReader;
import io.anserini.search.topicreader.TrecTopicReader;
import io.anserini.search.topicreader.WebxmlTopicReader;
import io.anserini.util.Qrels;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.SortedMap;

/**
 * Main class for feature extractors feed in command line arguments to dump features
 */
public class FeatureExtractorCli {
  private static final Logger LOG = LogManager.getLogger(FeatureExtractorCli.class);

  public static class FeatureExtractionArgs {
    @Option(name = "-index", metaVar = "[path]", required = true, usage = "Lucene index directory")
    public String indexDir;

    @Option(name = "-qrel", metaVar = "[path]", required = true, usage = "Qrel File")
    public String qrelFile;

    @Option(name = "-topic", metaVar = "[path]", required = true, usage = "Topic File")
    public String topicsFile;

    @Option(name = "-out", metaVar = "[path]", required = true, usage = "Output File")
    public String outputFile;

    @Option(name = "-collection", metaVar = "[path]", required = true, usage = "[clueweb|gov2|twitter]")
    public String collection;

    @Option(name = "-extractors", metaVar = "[path]", required = false, usage = "FeatureExtractors File")
    public String extractors = null;

    @SuppressWarnings("unchecked")
    public <K> TopicReader<K> buildTopicReaderForCollection() throws Exception {
      if ("clueweb".equals(collection)) {
        return new WebxmlTopicReader(Paths.get(topicsFile));
      } else if ("gov2".equals(collection)) {
        return new TrecTopicReader(Paths.get(topicsFile));
      } else if ("twitter".equals(collection)) {
        return (TopicReader<K>) new MicroblogTopicReader(Paths.get(topicsFile));
      }

      throw new RuntimeException("Unrecognized collection " + collection);
    }

    private FeatureExtractors loadFeatureExtractorsOrFail() {
      try {
        return extractors != null ? FeatureExtractors.loadExtractor(extractors) : null;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    @SuppressWarnings("unchecked")
    public <K> BaseFeatureExtractor<K> buildBaseFeatureExtractorOrFail(IndexReader reader, Qrels qrels, Map<K, Map<String, String>> topics) {
      FeatureExtractors featureExtractors = loadFeatureExtractorsOrFail();

      if ("clueweb".equals(collection) || "gov2".equals(collection)) {
        return new WebFeatureExtractor(reader, qrels, topics, featureExtractors);
      } else if ("twitter".equals(collection)) {
        return (BaseFeatureExtractor<K>) new TwitterFeatureExtractor(reader, qrels, (Map<Integer, Map<String, String>>) topics, featureExtractors);
      }

      throw new RuntimeException("Unrecognized collection " + collection);
    }
    
    public static <K> BaseFeatureExtractor<K> getFeatureExtractorForConfigurationPurposesFromProgramArgumentsOrFail(String args[]) {
      return getFeatureExtractorForConfigurationPurposes(FeatureExtractionArgs.parseFeatureExtractionArgsOrFail(args));
    }

    public static <K> BaseFeatureExtractor<K> getFeatureExtractorForConfigurationPurposes(FeatureExtractionArgs parsedArgs) {
      return parsedArgs.buildBaseFeatureExtractorOrFail(null, null, null);
    }
    
    public static FeatureExtractionArgs parseFeatureExtractionArgsOrFail(String args[]) {
      FeatureExtractionArgs parsedArgs = new FeatureExtractionArgs();
      CmdLineParser parser= new CmdLineParser(parsedArgs, ParserProperties.defaults().withUsageWidth(90));

      try {
        parser.parseArgument(args);
        
        return parsedArgs;
      } catch (CmdLineException e) {
        System.err.println(e.getMessage());
        parser.printUsage(System.err);
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * requires the user to supply the index directory and also the directory containing the qrels and topics
   * @param args  indexDir, qrelFile, topicFile, outputFile
   */
  public static<K> void main(String args[]) throws Exception {
    FeatureExtractionArgs parsedArgs = FeatureExtractionArgs.parseFeatureExtractionArgsOrFail(args);

    Directory indexDirectory = FSDirectory.open(Paths.get(parsedArgs.indexDir));
    IndexReader reader = DirectoryReader.open(indexDirectory);
    Qrels qrels = new Qrels(parsedArgs.qrelFile);

    // Query parser needed to construct the query object for feature extraction in the loop
    PrintStream out = new PrintStream (new FileOutputStream(new File(parsedArgs.outputFile)));

    TopicReader<K> tr = parsedArgs.buildTopicReaderForCollection();
    SortedMap<K, Map<String, String>> topics = tr.read();
    LOG.debug(String.format("%d topics found", topics.size()));

    BaseFeatureExtractor<K> extractor = parsedArgs.buildBaseFeatureExtractorOrFail(reader, qrels, topics);
    extractor.printFeatures(out);
  }
}
