package com.bap80.merchantrole;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * All persistent state for one player. Counters are cumulative; "max"
 * counters remember the highest amount ever observed so spending items does
 * not remove progress.
 */
public final class PlayerProfile {
    private final UUID uuid;
    private String name = "";
    private Rank rank = Rank.PEASANTS;
    private int rankupCount;
    private final Map<String, Integer> counters = new HashMap<>();
    private final Set<String> flags = new HashSet<>();
    private final Map<String, Set<String>> sets = new HashMap<>();
    private boolean dirty;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            markDirty();
        }
    }

    public Rank rank() {
        return rank;
    }

    public void setRank(Rank rank) {
        if (rank != null && rank != this.rank) {
            this.rank = rank;
            markDirty();
        }
    }

    public int rankupCount() {
        return rankupCount;
    }

    public void setRankupCount(int rankupCount) {
        if (rankupCount != this.rankupCount) {
            this.rankupCount = Math.max(0, rankupCount);
            markDirty();
        }
    }

    public void incrementRankupCount() {
        rankupCount++;
        markDirty();
    }

    public int counter(String key) {
        return counters.getOrDefault(key, 0);
    }

    public void setCounter(String key, int value) {
        int safe = Math.max(0, value);
        if (counter(key) != safe) {
            counters.put(key, safe);
            markDirty();
        }
    }

    public void addCounter(String key, int amount) {
        if (amount <= 0) {
            return;
        }
        counters.merge(key, amount, Integer::sum);
        markDirty();
    }

    public void maxCounter(String key, int value) {
        if (value > counter(key)) {
            counters.put(key, value);
            markDirty();
        }
    }

    public boolean hasFlag(String key) {
        return flags.contains(key);
    }

    public void setFlag(String key) {
        if (flags.add(key)) {
            markDirty();
        }
    }

    public Set<String> set(String key) {
        Set<String> values = sets.get(key);
        return values == null ? Collections.emptySet() : Collections.unmodifiableSet(values);
    }

    public int setSize(String key) {
        return set(key).size();
    }

    public boolean addToSet(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        boolean added = sets.computeIfAbsent(key, ignored -> new HashSet<>()).add(value);
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean removeFromSet(String key, String value) {
        Set<String> values = sets.get(key);
        if (values == null) {
            return false;
        }
        boolean removed = values.remove(value);
        if (removed) {
            if (values.isEmpty()) {
                sets.remove(key);
            }
            markDirty();
        }
        return removed;
    }

    public Map<String, Integer> counters() {
        return Collections.unmodifiableMap(counters);
    }

    public Set<String> flags() {
        return Collections.unmodifiableSet(flags);
    }

    public Map<String, Set<String>> sets() {
        return Collections.unmodifiableMap(sets);
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }
}
