package com.bap80.merchantrole;

/**
 * The current state of one task. Some tasks (such as the raw-ore task) have
 * their own completion rule, so complete is stored explicitly.
 */
public record TaskProgress(int current, int goal, String detail, boolean complete) {
    public static TaskProgress of(int current, int goal) {
        return new TaskProgress(current, goal, "", current >= goal);
    }

    public int shownCurrent() {
        return goal > 0 ? Math.min(current, goal) : current;
    }
}
