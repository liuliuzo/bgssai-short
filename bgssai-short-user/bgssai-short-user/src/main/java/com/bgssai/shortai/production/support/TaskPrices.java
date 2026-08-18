package com.bgssai.shortai.production.support;

public final class TaskPrices {

    private TaskPrices() {
    }

    public static int cost(String taskType) {
        if ("VIDEO".equals(taskType)) {
            return 150;
        }
        if ("IMAGE".equals(taskType)) {
            return 50;
        }
        if ("COMPOSE".equals(taskType)) {
            return 80;
        }
        if ("VOICE".equals(taskType)) {
            return 20;
        }
        return 15;
    }
}
