package io.anserini.index.generator;


import java.util.stream.Collectors;

import de.aitools.aq.web.extractor.PotthastJerichoExtractor;
import io.anserini.index.IndexArgs;
import io.anserini.index.IndexCollection;
import io.anserini.index.transform.StringTransform;

public class PotthastJerichoMainContentExtractor extends LuceneDocumentGenerator {
  public PotthastJerichoMainContentExtractor() {
    super(potthastJerichoExtractor());
  }

  public PotthastJerichoMainContentExtractor(IndexArgs args, IndexCollection.Counters counters) {
    super(potthastJerichoExtractor(), args, counters);
  }

  private static StringTransform potthastJerichoExtractor() {
    return new StringTransform() {
      @Override
      public String apply(String html) {
        PotthastJerichoExtractor extractor = extractor();
        try {
          return extractor.extractSentences(html).stream().collect(Collectors.joining(" "));
        } catch(Exception e) {
          return "";
        }
      }
    };
  }

  private static PotthastJerichoExtractor extractor() {
    PotthastJerichoExtractor ret = new PotthastJerichoExtractor();
    ret.setMinParagraphLengthInCharacters(50);
    ret.setTimeoutInSeconds(20);
    ret.setExtractLanguages("en");
    ret.setExtractAltTexts(false);

    return ret;
  }
}
