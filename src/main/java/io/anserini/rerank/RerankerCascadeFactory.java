package io.anserini.rerank;

import java.util.List;

import io.anserini.search.SearchArgs;

public interface RerankerCascadeFactory {
	public List<Reranker<?>> instantiateRerankersForCascade(SearchArgs args);
}
