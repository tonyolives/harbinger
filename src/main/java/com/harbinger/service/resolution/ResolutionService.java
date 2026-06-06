package com.harbinger.service.resolution;

import com.harbinger.model.Homeowner;
import com.harbinger.model.RawSignal;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

/**
 * The pipeline's centerpiece: collapses many messy {@link RawSignal}s back into one
 * {@link ResolvedCluster} per real homeowner. The strategy is
 * <em>block → fuzzy-match → cluster</em>:
 *
 * <ol>
 *   <li><b>Block</b> by normalized address so only signals at the same property are
 *       ever compared — cheap, and it stops different people at one address from being
 *       confused with each other across addresses.</li>
 *   <li><b>Fuzzy-match</b> names inside each block token-wise with Jaro-Winkler: the
 *       last names must be similar and the first names compatible — equal, Jaro-Winkler
 *       close, or one an initial of the other. This links forms plain normalization
 *       can't (e.g. {@code "j smith"} to {@code "john smith"}) while keeping different
 *       people at one address apart.</li>
 *   <li><b>Cluster</b> the linked signals transitively with union-find.</li>
 * </ol>
 *
 * <p>The {@link #resolve} path is pure (no I/O, no clock) and never reads
 * {@code trueOwnerId} — the cluster id is a deterministic synthetic key derived from the
 * resolved name and address.
 */
@Service
public class ResolutionService {

    /**
     * Minimum Jaro-Winkler similarity for two name tokens to count as the same. Generator
     * variants keep last names intact and abbreviate only first names, so a strict value
     * preserves precision; recall comes from the initial-compatibility rule below, not
     * from loosening this.
     */
    private static final double TOKEN_SIMILARITY_THRESHOLD = 0.90;

    private final NameNormalizer nameNormalizer;
    private final AddressNormalizer addressNormalizer;
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    public ResolutionService(NameNormalizer nameNormalizer, AddressNormalizer addressNormalizer) {
        this.nameNormalizer = nameNormalizer;
        this.addressNormalizer = addressNormalizer;
    }

    /**
     * @param signals messy raw signals (in any order)
     * @return one cluster per resolved homeowner, each holding its member signals
     */
    public List<ResolvedCluster> resolve(List<RawSignal> signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals must not be null");
        }
        if (signals.isEmpty()) {
            return List.of();
        }

        int n = signals.size();
        String[] normNames = new String[n];
        String[] normAddrs = new String[n];
        Map<String, List<Integer>> blocks = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            normNames[i] = nameNormalizer.normalize(signals.get(i).rawName());
            normAddrs[i] = addressNormalizer.normalize(signals.get(i).rawAddress());
            blocks.computeIfAbsent(normAddrs[i], k -> new ArrayList<>()).add(i);
        }

        // Within each address block, link every name-similar pair; union-find makes
        // the relation transitive (a~b and b~c puts a, b, c in one cluster).
        UnionFind unionFind = new UnionFind(n);
        for (List<Integer> block : blocks.values()) {
            for (int a = 0; a < block.size(); a++) {
                for (int b = a + 1; b < block.size(); b++) {
                    int i = block.get(a);
                    int j = block.get(b);
                    if (namesMatch(normNames[i], normNames[j])) {
                        unionFind.union(i, j);
                    }
                }
            }
        }

        // Group signal indices by their union-find root, preserving first-seen order.
        Map<Integer, List<Integer>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(unionFind.find(i), k -> new ArrayList<>()).add(i);
        }

        List<ResolvedCluster> resolved = new ArrayList<>(clusters.size());
        for (List<Integer> members : clusters.values()) {
            resolved.add(buildCluster(members, normNames, normAddrs, signals));
        }
        return resolved;
    }

    /**
     * Two normalized names refer to the same person when their last names are
     * Jaro-Winkler-close and their first names are compatible. Names without a clear
     * first/last split fall back to a whole-string Jaro-Winkler comparison.
     */
    private boolean namesMatch(String a, String b) {
        String[] ta = a.split(" ");
        String[] tb = b.split(" ");
        if (ta.length < 2 || tb.length < 2) {
            return jaroWinkler.apply(a, b) >= TOKEN_SIMILARITY_THRESHOLD;
        }
        String lastA = ta[ta.length - 1];
        String lastB = tb[tb.length - 1];
        if (jaroWinkler.apply(lastA, lastB) < TOKEN_SIMILARITY_THRESHOLD) {
            return false;
        }
        return firstNamesCompatible(ta[0], tb[0]);
    }

    /**
     * First names match when one is the single-letter initial of the other (e.g. {@code "j"}
     * vs {@code "john"}) or the two full forms are Jaro-Winkler-close.
     */
    private boolean firstNamesCompatible(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.equals(b);
        }
        if (a.length() == 1 || b.length() == 1) {
            return a.charAt(0) == b.charAt(0);
        }
        return jaroWinkler.apply(a, b) >= TOKEN_SIMILARITY_THRESHOLD;
    }

    private static ResolvedCluster buildCluster(
            List<Integer> members, String[] normNames, String[] normAddrs, List<RawSignal> signals) {
        // Representative name: the longest normalized form (favors "john smith" over the
        // initial-only "j smith"); first-seen order breaks ties deterministically.
        String repName = normNames[members.get(0)];
        for (int idx : members) {
            if (normNames[idx].length() > repName.length()) {
                repName = normNames[idx];
            }
        }
        // Every member shares the block address, so any member's is the cluster's.
        String address = normAddrs[members.get(0)];

        // Deterministic synthetic id (not trueOwnerId): same resolved identity ⇒ same id.
        UUID id = UUID.nameUUIDFromBytes((repName + "|" + address).getBytes(StandardCharsets.UTF_8));

        List<RawSignal> memberSignals = new ArrayList<>(members.size());
        for (int idx : members) {
            memberSignals.add(signals.get(idx));
        }
        return new ResolvedCluster(id, new Homeowner(id, repName, address), memberSignals);
    }

    /**
     * Classic disjoint-set forest over {@code [0, size)} with path compression and
     * union by rank — near-constant-time {@code find}/{@code union}.
     */
    private static final class UnionFind {

        private final int[] parent;
        private final int[] rank;

        UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]; // path compression
                x = parent[x];
            }
            return x;
        }

        void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA == rootB) {
                return;
            }
            if (rank[rootA] < rank[rootB]) {
                parent[rootA] = rootB;
            } else if (rank[rootA] > rank[rootB]) {
                parent[rootB] = rootA;
            } else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }
        }
    }
}
