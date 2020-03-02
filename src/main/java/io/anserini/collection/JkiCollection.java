package io.anserini.collection;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JkiCollection extends DocumentCollection<JkiCollection.Document> {

  public JkiCollection(Path path){
    this.path = path;
    this.allowedFileSuffix = new HashSet<>(Arrays.asList(".json"));
  }

  @Override
  public FileSegment<Document> createFileSegment(Path p) throws IOException {
    try {
      List<Map<String, Object>> entries = new ObjectMapper().readValue(p.toFile(), new TypeReference<List<Map<String, Object>>>(){});
      Iterator<Map<String, Object>> iterator = entries.iterator();
      
      return new FileSegment<JkiCollection.Document>(p) {
        @Override
        protected void readNext() throws IOException, ParseException, NoSuchElementException {
          if(!iterator.hasNext()) {
            atEOF = true;
            throw new NoSuchElementException("Reached end of JsonNode iterator");
          } else {
            bufferedRecord = new JkiCollection.Document(iterator.next());
            if(!iterator.hasNext()) {
              atEOF = true;
            }
          }
        }
      };
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static class Document implements SourceDocument {
    private final int id;
    private final String title, abstr;
    
    public Document(Map<String, Object> parsed) {
      try {
        id = (int) parsed.get("nummer");
        title = (String) parsed.get("titel");
        if (parsed.get("abstract") instanceof Integer) {
          abstr = "";
        } else {
          abstr = (String) parsed.get("abstract");
        }
      } catch (Exception e) {
    	throw new RuntimeException("Fix " + parsed, e);
      }
    }
    
    @Override
    public String id() {
      return String.valueOf(id);
    }

    @Override
    public String content() {
      return title + "\n\n" + abstr;
    }

    @Override
    public boolean indexable() {
      return true;
	}
  }
}
