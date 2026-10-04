package com.hcs.rpgcore.runtime;

import java.util.UUID;

public final class PlayerRuntimeData {

    private final UUID uuid;

    private double currentMana;
    private double maximumMana;

    public PlayerRuntimeData(
            UUID uuid,
            double currentMana,
            double maximumMana
    ) {
        this.uuid = uuid;

        this.maximumMana =
                Math.max(
                        0.0,
                        maximumMana
                );

        this.currentMana =
                clamp(
                        currentMana,
                        0.0,
                        this.maximumMana
                );
    }

    public UUID getUuid() {

        return uuid;
    }

    public synchronized double getCurrentMana() {

        return currentMana;
    }

    public synchronized double getMaximumMana() {

        return maximumMana;
    }

    public synchronized void setCurrentMana(
            double currentMana
    ) {

        this.currentMana =
                clamp(
                        currentMana,
                        0.0,
                        maximumMana
                );
    }

    public synchronized void setMaximumMana(
            double maximumMana,
            boolean preserveRatio
    ) {

        double newMaximum =
                Math.max(
                        0.0,
                        maximumMana
                );

        double oldMaximum =
                this.maximumMana;

        double oldCurrent =
                this.currentMana;

        this.maximumMana =
                newMaximum;

        if (newMaximum <= 0.0) {

            this.currentMana = 0.0;

            return;
        }

        if (preserveRatio
                && oldMaximum > 0.0) {

            double ratio =
                    oldCurrent
                            / oldMaximum;

            this.currentMana =
                    clamp(
                            newMaximum * ratio,
                            0.0,
                            newMaximum
                    );

            return;
        }

        this.currentMana =
                Math.min(
                        oldCurrent,
                        newMaximum
                );
    }

    public synchronized boolean consumeMana(
            double amount
    ) {

        if (amount <= 0.0) {

            return true;
        }

        if (currentMana < amount) {

            return false;
        }

        currentMana -= amount;

        return true;
    }

    public synchronized void restoreMana(
            double amount
    ) {

        if (amount <= 0.0) {

            return;
        }

        currentMana =
                Math.min(
                        maximumMana,
                        currentMana + amount
                );
    }

    public synchronized void refillMana() {

        currentMana =
                maximumMana;
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
