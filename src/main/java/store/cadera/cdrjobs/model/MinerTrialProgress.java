package store.cadera.cdrjobs.model;

public record MinerTrialProgress(
        int totalOres,
        int deepOres,
        int rareOres,
        int ancientDebris,
        boolean stoneComplete,
        boolean deepComplete
) {}
