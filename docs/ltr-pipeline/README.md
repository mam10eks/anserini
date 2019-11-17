# Anserini LTR (WIP)

**This document has the goal to sketch a possible LTR pipeline for Anserini from the perspective of a user. I have derived some tiny resources from the Million Query LETOR dataset for this document.**

We have a run file `sample-run-file.txt`, a qrel file `sample-qrels.txt`, and have calculated LTR features for all documents in the run file in a RankLib feature vector file `sample-million-query-2008.fv`.

Compile Anserini:
```
mvn -Dmaven.javadoc.skip=true -DskipTests -Dgpg.skip -f ../../pom.xml clean package appassembler:assemble install
```

We can calculate some evaluation measures of the initial run file:
```
../../target/appassembler/bin/Eval -qrels sample-qrels.txt -run sample-run-file.txt 
num_ret               	all	58
num_rel               	all	5
num_rel_ret           	all	5
map                   	all	0.1640
p.5                   	all	0.0000
p.10                  	all	0.2000
p.20                  	all	0.1000
p.30                  	all	0.1000
ndcg.10               	all	0.2339
ndcg.20               	all	0.2339
```

Next, we can train a LTR model with the feature vector file.

## Example with ListNet

To train a LTR model with ListNet, execute:

```
../../target/appassembler/bin/RankLibCli -train sample-million-query-2008.fv -ranker 7 -metric2t NDCG@20 -save listNetModel
```

Now we can use the `listNetModel` to rerank the run file `sample-run-file.txt`:
```
../../target/appassembler/bin/RerankExistingRunfile -output reranked-with-list-net-model.txt -runFileToRerank sample-run-file.txt -topicreader Trec -topics sample-topic.txt -bm25 -arbitraryScoreTieBreak -model listNetModel -experimental.args -rankLibFeatureVectorFile=sample-million-query-2008.fv -runtag rerankedWithListNet
```

Evaluate `reranked-with-list-net-model` and obtain results worse than the initial ranking:
```
../../target/appassembler/bin/Eval -qrels sample-qrels.txt -run reranked-with-list-net-model.txt
```



## Example with Mart

We can redo the steps and use Mart as LTR algorithm to obtain almost perfect results (attention: we did no train-test-split):
```
../../target/appassembler/bin/RankLibCli -train sample-million-query-2008.fv -ranker 0 -metric2t NDCG@20 -save martModel
```

Now we can use `martModel` to rerank the run file `sample-run-file.txt`:
```
../../target/appassembler/bin/RerankExistingRunfile -output reranked-with-mart-model.txt -runFileToRerank sample-run-file.txt -topicreader Trec -topics sample-topic.txt -bm25 -arbitraryScoreTieBreak -model martModel -experimental.args -rankLibFeatureVectorFile=sample-million-query-2008.fv -runtag rerankedWithMart
```

Now we evaluate the run file `reranked-with-mart-model.txt`:
```
../../target/appassembler/bin/Eval -qrels sample-qrels.txt -run reranked-with-mart-model.txt
num_ret               	all	50
num_rel               	all	5
num_rel_ret           	all	5
map                   	all	1.0000
p.5                   	all	1.0000
p.10                  	all	0.5000
p.20                  	all	0.2500
p.30                  	all	0.1667
ndcg.10               	all	1.0000
ndcg.20               	all	1.0000
```

