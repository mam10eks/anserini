package io.anserini.collection;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.anserini.collection.TrecwebCollection.Document;

public class ClueWeb09AnchorTextCollectionTest {
  private static final List<String> EXPECTED_PART_01 = Arrays.asList(
    "{'id': 'clueweb09-en0098-17-00000', 'content': 'http://0--------0.deviantart.com/	0--------0	0--------0	0--------0	0--------0	0--------00--------0	0--------0	0--------0	 	0--------0	    Profile'}", 
    "{'id': 'clueweb09-en0112-10-00043', 'content': 'http://0-0-0checkmate.com/Bugs/AntWorks_Sculpture_Illuminator.html	 	Click here for more detail and pricing.	 	AntWorks Sculpture Kit and Illuminator	Click here for more detail and pricing.	 	AntWorks Sculpture Kit and Illuminator'}",
    "{'id': 'clueweb09-en0112-10-00087', 'content': 'http://0-0-0checkmate.com/Cool/Dancing_Fairies_Pewter_Stand_Crystal_Ball_Combo_BI6801.html	 	Click here for more detail and pricing.	Dancing Fairies Crystal Ball Combination'}"
  );
  
  private static final List<String> EXPECTED_PART_02 = Arrays.asList(
    "{'id': 'clueweb09-en0125-52-07147', 'content': 'http://zzzest.blogdrive.com/archive/40.html	hmm...	Next Entry	Previous Entry	Permalink'}",
    "{'id': 'clueweb09-en0069-51-18859', 'content': 'http://zzzhongyu.en.alibaba.com/product/209777828-50374794/pet_bowls.html	pet bowls	 	pet bowls  	  	more	pet bowls'}"
  );
  
  @Test
  public void testClueWeb09AnchorsForExamplePart01() throws IOException {
    ClueWeb09AnchorTextCollection collection = new ClueWeb09AnchorTextCollection();
    FileSegment<Document> segment = collection.createFileSegment(Paths.get("src/test/resources/sample_cw09_anchor_test/part-01.gz"));
    List<String> actual = segmentToStrings(segment);

    Assert.assertEquals(EXPECTED_PART_01, actual);
  }

  @Test
  public void testClueWeb09AnchorsForExamplePart02() throws IOException {
    ClueWeb09AnchorTextCollection collection = new ClueWeb09AnchorTextCollection();
    FileSegment<Document> segment = collection.createFileSegment(Paths.get("src/test/resources/sample_cw09_anchor_test/part-02.gz"));
    List<String> actual = segmentToStrings(segment);

    Assert.assertEquals(EXPECTED_PART_02, actual);
  }

  private List<String> segmentToStrings(FileSegment<Document> segment) {
    List<String> ret = new ArrayList<>();
    for(Document doc: segment) {
      ret.add("{'id': '" + doc.id() + "', 'content': '" + doc.content() + "'}");
    }
    
    return ret;
  }
}
