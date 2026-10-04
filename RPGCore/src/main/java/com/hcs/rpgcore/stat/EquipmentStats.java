package com.hcs.rpgcore.stat;

public record EquipmentStats(
        double attack,
        double defense,
        double maxMana,
        double damageReductionFlat
) {

    public static EquipmentStats empty() {

        return new EquipmentStats(
                0.0,
                0.0,
                0.0,
                0.0
        );
    }


    public EquipmentStats add(
            EquipmentStats other
    ) {

        if (other == null) {
            return this;
        }

        return new EquipmentStats(
                attack + other.attack(),
                defense + other.defense(),
                maxMana + other.maxMana(),
                damageReductionFlat
                        + other.damageReductionFlat()
        );
    }
}
