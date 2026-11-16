package com.bap80.merchantrole;

import java.util.function.Function;

/**
 * A single task row. The description may contain %d, which is replaced with
 * the configured goal.
 */
public record TaskDefinition(String id,
                             String descriptionTemplate,
                             Function<ProgressContext, TaskProgress> evaluator) {

    public String description(TaskProgress progress) {
        if (descriptionTemplate.contains("%d")) {
            return String.format(descriptionTemplate, progress.goal());
        }
        return descriptionTemplate;
    }
}
