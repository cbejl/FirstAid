package de.technikforlife.firstaid.damagesystem.enums;

public enum EnumWoundState {
    HEALTHY, WOUNDED_LIGHT, WOUNDED_HEAVY;

    public static EnumWoundState getWoundState(float maxHealth, float currentHealth) {
        if (maxHealth * 0.9F <= currentHealth)
            return EnumWoundState.HEALTHY;
        if ((maxHealth * 0.4F) <= currentHealth)
            return EnumWoundState.WOUNDED_LIGHT;
        return EnumWoundState.WOUNDED_HEAVY;
    }
}
