package io.anserini.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class RerankExistingRunfileTest {
  private static final String[] PROGRAM_ARGS_WITH_DESCENDING_CLICK_RERANKER(Path run_file) {
    return new String[] { "-topicreader", "Trec", "-topics", "./src/main/resources/topics-and-qrels/topics.51-100.txt",
        "-output", run_file.toAbsolutePath().toString(), "-runFileToRerank", "./src/test/resources/TestRunFileReranker",
        "-bm25", "-arbitraryScoreTieBreak", "-runtag", "runTag"};
  };

  @Test
  public void testRerankExistingRunfile() throws IOException, CmdLineException {
    Path runPath = Files.createTempFile("aus", "");
    RerankExistingRunfile.main(PROGRAM_ARGS_WITH_DESCENDING_CLICK_RERANKER(runPath));
    String runfileContend = new String(Files.readAllBytes(runPath), StandardCharsets.UTF_8);
    String expected = "51 Q0 clueweb09-en0011-41-17287 1 2.000000 runTag\n"
        + "52 Q0 clueweb09-en0011-72-16411 1 1.000000 runTag\n" + "53 Q0 clueweb09-en0006-14-40112 1 5.997700 runTag\n"
        + "53 Q0 clueweb09-en0007-69-07526 2 5.997500 runTag\n" + "55 Q0 clueweb09-en0007-73-31430 1 0.997900 runTag\n"
        + "55 Q0 clueweb09-en0003-97-02000 2 0.997800 runTag\n" + "56 Q0 clueweb09-en0009-37-06884 1 9.999900 runTag\n"
        + "56 Q0 clueweb09-en0003-56-22553 2 9.999600 runTag\n" + "57 Q0 clueweb09-en0007-01-40578 1 1.999600 runTag\n"
        + "57 Q0 clueweb09-en0000-43-29781 2 1.999599 runTag\n" + "57 Q0 clueweb09-en0003-29-11468 3 1.999598 runTag\n"
        + "58 Q0 clueweb09-en0000-84-02833 1 0.999900 runTag\n" + "58 Q0 clueweb09-en0010-26-16067 2 0.999899 runTag\n"
        + "59 Q0 clueweb09-en0004-53-03139 1 1.999700 runTag\n" + "59 Q0 clueweb09-en0007-29-03335 2 1.999699 runTag\n"
        + "59 Q0 clueweb09-en0004-01-28494 3 1.999698 runTag\n" + "60 Q0 clueweb09-en0003-97-28463 1 1.000000 runTag\n"
        + "60 Q0 clueweb09-en0000-07-17830 2 0.999999 runTag\n" + "61 Q0 clueweb09-en0003-62-19365 1 0.999900 runTag\n"
        + "61 Q0 clueweb09-en0003-11-25615 2 0.999899 runTag\n" + "62 Q0 clueweb09-enwp01-31-18410 1 5.999500 runTag\n"
        + "62 Q0 clueweb09-enwp01-31-18370 2 5.999499 runTag\n" + "62 Q0 clueweb09-enwp01-17-18260 3 5.999498 runTag\n"
        + "63 Q0 clueweb09-en0001-40-17253 1 0.999900 runTag\n" + "63 Q0 clueweb09-en0007-47-26264 2 0.999899 runTag\n"
        + "63 Q0 clueweb09-en0008-04-20990 3 0.999898 runTag\n" + "65 Q0 clueweb09-en0002-67-09195 1 1.999500 runTag\n"
        + "65 Q0 clueweb09-en0002-67-09215 2 1.999499 runTag\n" + "65 Q0 clueweb09-en0002-67-09259 3 1.999498 runTag\n"
        + "65 Q0 clueweb09-en0002-67-09258 4 1.999497 runTag\n" + "66 Q0 clueweb09-en0011-26-32584 1 9.971900 runTag\n"
        + "66 Q0 clueweb09-en0000-59-12978 2 9.970200 runTag\n" + "67 Q0 clueweb09-en0000-13-09799 1 2.000000 runTag\n"
        + "67 Q0 clueweb09-en0008-65-28819 2 1.999999 runTag\n" + "67 Q0 clueweb09-en0009-73-33499 3 1.999998 runTag\n"
        + "68 Q0 clueweb09-en0010-65-12481 1 1.000000 runTag\n" + "69 Q0 clueweb09-en0011-59-17161 1 1.999900 runTag\n"
        + "69 Q0 clueweb09-en0009-36-35960 2 1.999899 runTag\n" + "69 Q0 clueweb09-en0006-46-04671 3 1.999898 runTag\n"
        + "70 Q0 clueweb09-en0005-92-13499 1 0.839400 runTag\n" + "70 Q0 clueweb09-en0004-60-18881 2 0.838800 runTag\n"
        + "71 Q0 clueweb09-en0000-07-18267 1 1.996800 runTag\n" + "72 Q0 clueweb09-en0004-05-00037 1 0.983900 runTag\n"
        + "72 Q0 clueweb09-en0004-05-00041 2 0.983899 runTag\n" + "72 Q0 clueweb09-en0009-64-07437 3 0.983798 runTag\n"
        + "73 Q0 clueweb09-en0008-68-30880 1 1.999900 runTag\n" + "74 Q0 clueweb09-enwp03-19-14595 1 1.000000 runTag\n"
        + "75 Q0 clueweb09-en0006-64-10679 1 1.000000 runTag\n" + "76 Q0 clueweb09-en0009-13-39043 1 1.999500 runTag\n"
        + "76 Q0 clueweb09-en0002-11-21202 2 1.999499 runTag\n" + "76 Q0 clueweb09-en0002-62-09694 3 1.999398 runTag\n"
        + "76 Q0 clueweb09-en0002-62-09711 4 1.999397 runTag\n" + "77 Q0 clueweb09-enwp02-27-04911 1 1.000000 runTag\n"
        + "78 Q0 clueweb09-en0002-73-18871 1 0.998000 runTag\n" + "79 Q0 clueweb09-en0011-99-17304 1 1.000000 runTag\n"
        + "80 Q0 clueweb09-en0009-40-37867 1 1.999600 runTag\n" + "80 Q0 clueweb09-en0008-49-09141 2 1.999599 runTag\n"
        + "81 Q0 clueweb09-enwp01-72-17844 1 0.999200 runTag\n" + "81 Q0 clueweb09-enwp01-92-01047 2 0.999199 runTag\n"
        + "81 Q0 clueweb09-en0006-54-23131 3 0.999098 runTag\n" + "82 Q0 clueweb09-en0001-35-16898 1 0.997100 runTag\n"
        + "82 Q0 clueweb09-en0009-76-31527 2 0.997000 runTag\n" + "83 Q0 clueweb09-en0002-59-08429 1 0.991300 runTag\n"
        + "83 Q0 clueweb09-en0009-29-35142 2 0.991299 runTag\n" + "83 Q0 clueweb09-en0009-29-34632 3 0.991298 runTag\n"
        + "84 Q0 clueweb09-en0004-27-23942 1 2.000000 runTag\n" + "84 Q0 clueweb09-en0011-39-26948 2 1.999999 runTag\n"
        + "84 Q0 clueweb09-en0009-26-14871 3 1.999998 runTag\n" + "85 Q0 clueweb09-en0005-73-36955 1 5.999900 runTag\n"
        + "85 Q0 clueweb09-enwp01-46-01055 2 5.999899 runTag\n" + "85 Q0 clueweb09-enwp01-59-01123 3 5.999898 runTag\n"
        + "86 Q0 clueweb09-en0002-79-20807 1 2.000000 runTag\n" + "86 Q0 clueweb09-en0003-85-21504 2 1.999999 runTag\n"
        + "87 Q0 clueweb09-en0011-13-40168 1 1.999600 runTag\n" + "88 Q0 clueweb09-en0010-72-22284 1 2.000000 runTag\n"
        + "88 Q0 clueweb09-en0000-29-19279 2 1.999999 runTag\n" + "88 Q0 clueweb09-en0005-79-13244 3 1.999998 runTag\n"
        + "88 Q0 clueweb09-en0000-65-01958 4 1.999997 runTag\n" + "89 Q0 clueweb09-en0010-80-32336 1 1.000000 runTag\n"
        + "90 Q0 clueweb09-enwp02-27-09491 1 1.000000 runTag\n" + "90 Q0 clueweb09-enwp01-36-17926 2 0.999999 runTag\n"
        + "91 Q0 clueweb09-en0011-24-19652 1 5.992400 runTag\n" + "91 Q0 clueweb09-en0008-79-08907 2 5.992300 runTag\n"
        + "93 Q0 clueweb09-enwp01-80-13508 1 1.000000 runTag\n" + "94 Q0 clueweb09-en0003-45-10110 1 0.999900 runTag\n"
        + "94 Q0 clueweb09-en0001-57-15097 2 0.999899 runTag\n" + "95 Q0 clueweb09-en0010-87-27179 1 5.976700 runTag\n"
        + "95 Q0 clueweb09-en0003-07-27449 2 5.975300 runTag\n" + "95 Q0 clueweb09-en0003-24-31446 3 5.975199 runTag\n"
        + "97 Q0 clueweb09-enwp01-22-01120 1 1.998600 runTag\n" + "98 Q0 clueweb09-en0009-03-16875 1 0.983300 runTag\n"
        + "98 Q0 clueweb09-en0011-03-35594 2 0.983000 runTag\n" + "98 Q0 clueweb09-en0008-71-29099 3 0.982899 runTag\n"
        + "98 Q0 clueweb09-en0007-53-09335 4 0.982798 runTag\n" + "99 Q0 clueweb09-en0005-11-00190 1 0.998500 runTag\n"
        + "99 Q0 clueweb09-en0006-94-31564 2 0.998400 runTag\n" + "100 Q0 clueweb09-en0004-17-32971 1 6.000000 runTag\n"
        + "100 Q0 clueweb09-en0007-18-18119 2 5.999999 runTag\n"
        + "100 Q0 clueweb09-en0009-49-38379 3 5.999998 runTag\n" + "100 Q0 clueweb09-en0009-49-38380 4 5.999997 runTag";
    assertEquals(runfileContend.trim(), expected.trim());
  }
}
