package com.allendowney.thinkdast;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;

/**
 * Represents the results of a search query.
 * Each result maps a URL to a relevance score (term frequency).
 */
public class WikiSearch {

    // Map from URL to relevance score
    private Map<String, Integer> map;

    /**
     * Constructor.
     *
     * @param map map of URLs to relevance scores
     */
    public WikiSearch(Map<String, Integer> map) {
        this.map = map;
    }

    /**
     * Returns the relevance of a given URL.
     *
     * @param url Wikipedia page URL
     * @return relevance score
     */
    public Integer getRelevance(String url) {
        Integer relevance = map.get(url);
        return relevance == null ? 0 : relevance;
    }

    /**
     * Prints search results sorted by relevance.
     */
    private void print() {
        List<Entry<String, Integer>> entries = sort();
        for (Entry<String, Integer> entry : entries) {
            System.out.println(entry);
        }
    }

    /**
     * OR query (union of results).
     * Relevance is the sum of term frequencies.
     */
    public WikiSearch or(WikiSearch that) {
        Map<String, Integer> union = new HashMap<>(map);

        for (String url : that.map.keySet()) {
            int relevance = totalRelevance(
                    this.getRelevance(url),
                    that.getRelevance(url)
            );
            union.put(url, relevance);
        }
        return new WikiSearch(union);
    }

    /**
     * AND query (intersection of results).
     * Relevance is the sum of term frequencies.
     */
    public WikiSearch and(WikiSearch that) {
        Map<String, Integer> intersection = new HashMap<>();

        for (String url : map.keySet()) {
            if (that.map.containsKey(url)) {
                int relevance = totalRelevance(
                        this.map.get(url),
                        that.map.get(url)
                );
                intersection.put(url, relevance);
            }
        }
        return new WikiSearch(intersection);
    }

    /**
     * MINUS query (difference of results).
     * Keeps pages in this search but not in the other.
     */
    public WikiSearch minus(WikiSearch that) {
        Map<String, Integer> difference = new HashMap<>(map);

        for (String url : that.map.keySet()) {
            difference.remove(url);
        }
        return new WikiSearch(difference);
    }

    /**
     * Computes combined relevance.
     * For Exercise 13, relevance is the sum of term frequencies.
     */
    protected int totalRelevance(Integer rel1, Integer rel2) {
        return rel1 + rel2;
    }

    /**
     * Sorts results by increasing relevance.
     *
     * @return sorted list of entries
     */
    public List<Entry<String, Integer>> sort() {
        List<Entry<String, Integer>> entries =
                new LinkedList<>(map.entrySet());

        Comparator<Entry<String, Integer>> comparator =
                (e1, e2) -> e1.getValue().compareTo(e2.getValue());

        Collections.sort(entries, comparator);
        return entries;
    }

    /**
     * Performs a search for a single term.
     */
    public static WikiSearch search(String term, JedisIndex index) {
        Map<String, Integer> map = index.getCounts(term);
        return new WikiSearch(map);
    }

    /**
     * Main method that runs all Exercise 13 queries.
     */
    public static void main(String[] args) throws IOException {

        // Create Redis index
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);

        String term1 = "java";
        String term2 = "programming";

        // Single-term search: java
        System.out.println("\n=== Query: " + term1 + " ===");
        WikiSearch search1 = search(term1, index);
        search1.print();

        // Single-term search: programming
        System.out.println("\n=== Query: " + term2 + " ===");
        WikiSearch search2 = search(term2, index);
        search2.print();

        // AND query
        System.out.println("\n=== Query: " + term1 + " AND " + term2 + " ===");
        WikiSearch andSearch = search1.and(search2);
        andSearch.print();

        // OR query
        System.out.println("\n=== Query: " + term1 + " OR " + term2 + " ===");
        WikiSearch orSearch = search1.or(search2);
        orSearch.print();

        // MINUS query
        System.out.println("\n=== Query: " + term1 + " MINUS " + term2 + " ===");
        WikiSearch minusSearch = search1.minus(search2);
        minusSearch.print();
    }
}
