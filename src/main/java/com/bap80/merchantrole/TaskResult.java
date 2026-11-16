package com.bap80.merchantrole;

public record TaskResult(TaskDefinition definition, TaskProgress progress) {
    public boolean complete() {
        return progress.complete();
    }
}
