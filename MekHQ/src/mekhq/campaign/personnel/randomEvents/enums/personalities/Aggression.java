/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.randomEvents.enums.personalities;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum Aggression {
    // region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description", false, false),
    AGGRESSIVE("Aggression.AGGRESSIVE.text", "Aggression.AGGRESSIVE.description", false, false),
    ASSERTIVE("Aggression.ASSERTIVE.text", "Aggression.ASSERTIVE.description", true, false),
    BELLIGERENT("Aggression.BELLIGERENT.text", "Aggression.BELLIGERENT.description", false, false),
    BLOODTHIRSTY("Aggression.BLOODTHIRSTY.text", "Aggression.BLOODTHIRSTY.description", false, true),
    BOLD("Aggression.BOLD.text", "Aggression.BOLD.description", true, false),
    BRASH("Aggression.BRASH.text", "Aggression.BRASH.description", false, false),
    CONFIDENT("Aggression.CONFIDENT.text", "Aggression.CONFIDENT.description", true, false),
    COURAGEOUS("Aggression.COURAGEOUS.text", "Aggression.COURAGEOUS.description", true, false),
    DARING("Aggression.DARING.text", "Aggression.DARING.description", true, false),
    DECISIVE("Aggression.DECISIVE.text", "Aggression.DECISIVE.description", true, false),
    DETERMINED("Aggression.DETERMINED.text", "Aggression.DETERMINED.description", true, false),
    DIPLOMATIC("Aggression.DIPLOMATIC.text", "Aggression.DIPLOMATIC.description", true, true),
    DOMINEERING("Aggression.DOMINEERING.text", "Aggression.DOMINEERING.description", false, false),
    FEARLESS("Aggression.FEARLESS.text", "Aggression.FEARLESS.description", true, false),
    HOSTILE("Aggression.HOSTILE.text", "Aggression.HOSTILE.description", false, false),
    HOT_HEADED("Aggression.HOT_HEADED.text", "Aggression.HOT_HEADED.description", false, false),
    IMPETUOUS("Aggression.IMPETUOUS.text", "Aggression.IMPETUOUS.description", false, false),
    IMPULSIVE("Aggression.IMPULSIVE.text", "Aggression.IMPULSIVE.description", false, false),
    INFLEXIBLE("Aggression.INFLEXIBLE.text", "Aggression.INFLEXIBLE.description", false, false),
    INTREPID("Aggression.INTREPID.text", "Aggression.INTREPID.description", true, false),
    MURDEROUS("Aggression.MURDEROUS.text", "Aggression.MURDEROUS.description", false, true),
    OVERBEARING("Aggression.OVERBEARING.text", "Aggression.OVERBEARING.description", false, false),
    PACIFISTIC("Aggression.PACIFISTIC.text", "Aggression.PACIFISTIC.description", true, true),
    RECKLESS("Aggression.RECKLESS.text", "Aggression.RECKLESS.description", false, false),
    RESOLUTE("Aggression.RESOLUTE.text", "Aggression.RESOLUTE.description", true, false),
    SADISTIC("Aggression.SADISTIC.text", "Aggression.SADISTIC.description", false, true),
    SAVAGE("Aggression.SAVAGE.text", "Aggression.SAVAGE.description", false, true),
    STUBBORN("Aggression.STUBBORN.text", "Aggression.STUBBORN.description", false, false),
    TENACIOUS("Aggression.TENACIOUS.text", "Aggression.TENACIOUS.description", true, false),
    VIGILANT("Aggression.VIGILANT.text", "Aggression.VIGILANT.description", true, false);

    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Aggression(final String name, final String description, boolean isPositive, boolean isMajor) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personalities",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    // endregion Constructors

    // region Getters

    public String getDescription() {
        return description;
    }

    /**
     * @return {@code true} if the personality trait is considered positive,
     *         {@code false} otherwise.
     */

    public boolean isTraitPositive() {
        return isPositive;
    }

    /**
     * @return {@code true} if the personality trait is considered a major trait,
     *         {@code false} otherwise.
     */

    public boolean isTraitMajor() {
        return isMajor;
    }
    // endregion Getters

    // region Boolean Comparison Methods

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isBloodthirsty() {
        return this == BLOODTHIRSTY;
    }

    public boolean isBold() {
        return this == BOLD;
    }

    public boolean isAggressive() {
        return this == AGGRESSIVE;
    }

    public boolean isAssertive() {
        return this == ASSERTIVE;
    }

    public boolean isBelligerent() {
        return this == BELLIGERENT;
    }

    public boolean isBrash() {
        return this == BRASH;
    }

    public boolean isConfident() {
        return this == CONFIDENT;
    }

    public boolean isCourageous() {
        return this == COURAGEOUS;
    }

    public boolean isDaring() {
        return this == DARING;
    }

    public boolean isDecisive() {
        return this == DECISIVE;
    }

    public boolean isDetermined() {
        return this == DETERMINED;
    }

    public boolean isDiplomatic() {
        return this == DIPLOMATIC;
    }

    public boolean isDomineering() {
        return this == DOMINEERING;
    }

    public boolean isFearless() {
        return this == FEARLESS;
    }

    public boolean isHostile() {
        return this == HOSTILE;
    }

    public boolean isHotHeaded() {
        return this == HOT_HEADED;
    }

    public boolean isImpetuous() {
        return this == IMPETUOUS;
    }

    public boolean isImpulsive() {
        return this == IMPULSIVE;
    }

    public boolean isInflexible() {
        return this == INFLEXIBLE;
    }

    public boolean isIntrepid() {
        return this == INTREPID;
    }

    public boolean isMurderous() {
        return this == MURDEROUS;
    }

    public boolean isOverbearing() {
        return this == OVERBEARING;
    }

    public boolean isPacifistic() {
        return this == PACIFISTIC;
    }

    public boolean isReckless() {
        return this == RECKLESS;
    }

    public boolean isResolute() {
        return this == RESOLUTE;
    }

    public boolean isSadistic() {
        return this == SADISTIC;
    }

    public boolean isSavage() {
        return this == SAVAGE;
    }

    public boolean isStubborn() {
        return this == STUBBORN;
    }

    public boolean isTenacious() {
        return this == TENACIOUS;
    }

    public boolean isVigilant() {
        return this == VIGILANT;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a given string and returns the corresponding Aggression enum.
     * Accepts either the ENUM ordinal value or its name
     *
     * @param aggression the string to be parsed
     * @return the Aggression enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid
     *                               Aggression
     */

    public static Aggression parseFromString(final String aggression) {
        return switch (aggression) {
            case "0", "None" -> NONE;
            // Minor Characteristics
            case "1", "Bold" -> BOLD;
            case "2", "Aggressive" -> AGGRESSIVE;
            case "3", "Assertive" -> ASSERTIVE;
            case "4", "Belligerent" -> BELLIGERENT;
            case "5", "Brash" -> BRASH;
            case "6", "Confident" -> CONFIDENT;
            case "7", "Courageous" -> COURAGEOUS;
            case "8", "Daring" -> DARING;
            case "9", "Decisive" -> DECISIVE;
            case "10", "Determined" -> DETERMINED;
            case "11", "Domineering" -> DOMINEERING;
            case "12", "Fearless" -> FEARLESS;
            case "13", "Hostile" -> HOSTILE;
            case "14", "Hot-Headed" -> HOT_HEADED;
            case "15", "Impetuous" -> IMPETUOUS;
            case "16", "Impulsive" -> IMPULSIVE;
            case "17", "Inflexible" -> INFLEXIBLE;
            case "18", "Intrepid" -> INTREPID;
            case "19", "Overbearing" -> OVERBEARING;
            case "20", "Reckless" -> RECKLESS;
            case "21", "Resolute" -> RESOLUTE;
            case "22", "Stubborn" -> STUBBORN;
            case "23", "Tenacious" -> TENACIOUS;
            case "24", "Vigilant" -> VIGILANT;
            // Major Characteristics
            case "25", "Bloodthirsty" -> BLOODTHIRSTY;
            case "26", "Diplomatic" -> DIPLOMATIC;
            case "27", "Murderous" -> MURDEROUS;
            case "28", "Pacifistic" -> PACIFISTIC;
            case "29", "Sadistic" -> SADISTIC;
            case "30", "Savage" -> SAVAGE;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Aggression.java/parseFromString: "
                                + aggression);
        };
    }

    /**
     * Parses an integer value into an Aggression enum.
     *
     * @param aggression the integer value representing the Aggression level
     * @return the corresponding Aggression enum value
     * @throws IllegalStateException if the integer value does not correspond to any
     *                               valid Aggression enum value
     */

    public static Aggression parseFromInt(final int aggression) {
        return switch (aggression) {
            case 0 -> NONE;
            // Minor Characteristics
            case 1 -> BOLD;
            case 2 -> AGGRESSIVE;
            case 3 -> ASSERTIVE;
            case 4 -> BELLIGERENT;
            case 5 -> BRASH;
            case 6 -> CONFIDENT;
            case 7 -> COURAGEOUS;
            case 8 -> DARING;
            case 9 -> DECISIVE;
            case 10 -> DETERMINED;
            case 11 -> DOMINEERING;
            case 12 -> FEARLESS;
            case 13 -> HOSTILE;
            case 14 -> HOT_HEADED;
            case 15 -> IMPETUOUS;
            case 16 -> IMPULSIVE;
            case 17 -> INFLEXIBLE;
            case 18 -> INTREPID;
            case 19 -> OVERBEARING;
            case 20 -> RECKLESS;
            case 21 -> RESOLUTE;
            case 22 -> STUBBORN;
            case 23 -> TENACIOUS;
            case 24 -> VIGILANT;
            // Major Characteristics
            case 25 -> BLOODTHIRSTY;
            case 26 -> DIPLOMATIC;
            case 27 -> MURDEROUS;
            case 28 -> PACIFISTIC;
            case 29 -> SADISTIC;
            case 30 -> SAVAGE;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Aggression.java/parseFromInt: "
                                + aggression);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
