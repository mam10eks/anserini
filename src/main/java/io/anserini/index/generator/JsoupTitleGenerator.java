package io.anserini.index.generator;

import org.jsoup.Jsoup;

import io.anserini.index.IndexArgs;
import io.anserini.index.IndexCollection;
import io.anserini.index.transform.StringTransform;

public class JsoupTitleGenerator extends LuceneDocumentGenerator {
  public JsoupTitleGenerator() {
    super(jsoupTitleTransformer());
  }

  public JsoupTitleGenerator(IndexArgs args, IndexCollection.Counters counters) {
    super(jsoupTitleTransformer(), args, counters);
  }

  private static StringTransform jsoupTitleTransformer() {
    return new StringTransform() {
      @Override
      public String apply(String html) {
        return Jsoup.parse(html).title();
      }
    };
  }
}
