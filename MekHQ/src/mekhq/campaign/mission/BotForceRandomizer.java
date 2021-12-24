/*
 * Copyright (c) 2021 - The Megamek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import megamek.Version;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.TaharqaSkillGenerator;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.util.StringUtil;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.UnitGeneratorParameters;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.math3.*;

/**
 * A class that can be used to generate a random force with some parameters. Provides a simpler approach
 * to opfor generation than AtBDynamicScenarioFactory. Intended for use by StoryArc but written generally
 * enough to be repurposed.
 *
 * Unlike AtBDynamicScenarioFactory, the methods here are not static, but depend on variables in an actual
 * BotForceRandomizer than can be added to a BotForce. If present, this randomizer will be used to generate
 * forces for the BotForce.
 */
public class BotForceRandomizer implements Serializable, MekHqXmlSerializable {

    private enum BalancingMethod {
        BV,
        WEIGHT_ADJ;

        @Override
        public String toString() {
            if(this == BV) {
                return "BV";
            } else if(this == WEIGHT_ADJ) {
                return "Adjusted Weight";
            }
            return super.toString();
        }
    }

    /** faction to draw from **/
    private String factionCode;

    /** skill level **/
    private SkillLevel skill;

    /** unit quality level **/
    private int quality;

    /** unit type **/
    private int unitType;

    /** lance size - this is the smallest increment in which random units will be generated and added **/
    private int lanceSize;

    /** focal weight class - if this is missing we use the mean weight class of the players unit **/
    private double focalWeightClass;

    /** force multiplier relative to player's deployed forces **/
    private double forceMultiplier;

    /** balancing method **/
    private BalancingMethod balancingMethod;

    /** convenience campaign pointer **/
    private Campaign campaign;

    /**
     * what percent of mek and aero forces should actually be conventional?
     * (tanks and conventional aircraft respectively)
     **/
    private int percentConventional;

    public BotForceRandomizer() {
        factionCode = "MERC";
        skill = SkillLevel.REGULAR;
        unitType = UnitType.MEK;
        forceMultiplier = 1.0;
        percentConventional = 0;
        balancingMethod = BalancingMethod.WEIGHT_ADJ;
        lanceSize = 1;
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(Factions.getInstance().getFaction(factionCode).getFullName(campaign.getGameYear()));
        sb.append(" ");
        sb.append(skill.toString());
        sb.append(" ");
        String typeDesc = UnitType.getTypeDisplayableName(unitType);
        if(percentConventional > 0) {
            typeDesc = typeDesc + " and Conventional";
        }
        sb.append(typeDesc);
        sb.append(" at x");
        sb.append(forceMultiplier);
        sb.append(" multiplier (");
        sb.append(balancingMethod.toString());
        sb.append(")");
        return sb.toString();
    }

    public List<Entity> generateForce(List<Unit> playerUnits, List<Entity> botFixedEntities) {
        ArrayList<Entity> entityList = new ArrayList<>();

        double maxPoints = calculateMaxPoints(playerUnits);
        double currentPoints = calculateStartingPoints(botFixedEntities);
        if(focalWeightClass < EntityWeightClass.WEIGHT_LIGHT || focalWeightClass > EntityWeightClass.WEIGHT_ASSAULT) {
            // if no focal weight class was provided or its outside of range then use the mean of the player units
            focalWeightClass = calculateMeanWeightClass(playerUnits);
        }

        // using a gamma distribution to get actual weight class for each lance. Each gamma
        // distribution is centered on the focal weight class and has some chance of going higher
        // or lower. The scale parameter of 0.4 produces a reasonable variance.
        GammaDistribution gamma = new GammaDistribution(focalWeightClass/.4, .4);

        int uType;
        ArrayList<Entity> lanceList;
        int weightClass;
        while(currentPoints < maxPoints) {

            weightClass = getWeightClass(gamma);

            // if the unit type is mek or aero, then roll to see if I get a conventional unit instead
            uType = unitType;
            if(unitType == UnitType.MEK && percentConventional > 0
                    && Compute.randomInt(100) <= percentConventional) {
                uType = UnitType.TANK;
            } else if(unitType == UnitType.AERO && percentConventional > 0
                    && Compute.randomInt(100) <= percentConventional) {
                uType = UnitType.CONV_FIGHTER;
            }

            lanceList = generateLance(lanceSize, uType, weightClass);
            for(Entity e : lanceList) {
                entityList.add(e);
                currentPoints += calculatePoints(e);
            }
        }

        return entityList;
    }

    public ArrayList<Entity> generateLance(int size, int uType, int weightClass) {
        ArrayList<Entity> lanceList = new ArrayList<>();

        //TODO: what about adding BA?

        for(int i = 0; i < size; i++) {
            Entity e = getEntity(uType, weightClass);
            if(null != e) {
                lanceList.add(e);
            }
        }

        return lanceList;
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param uType    The UnitTableData constant for the type of unit to generate.
     * @param weightClass The weight class of the unit to generate
     * @return A new Entity with crew.
     */
    public Entity getEntity(int uType, int weightClass) {
        MechSummary ms;

        //allow some variation in actual weight class
        int weightRoll = Compute.randomInt(6);
        if(weightRoll == 1 && weightClass > EntityWeightClass.WEIGHT_LIGHT) {
            weightClass -= 1;
        } else if(weightRoll == 6 && weightClass < EntityWeightClass.WEIGHT_ASSAULT) {
            weightClass += 1;
        }

        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setUnitType(uType);
        params.setWeightClass(weightClass);
        params.setYear(campaign.getGameYear());

        if(uType == UnitType.TANK) {
            //allow VTOLs too
            params.getMovementModes().addAll(IUnitGenerator.MIXED_TANK_VTOL);
        }

        ms = campaign.getUnitGenerator().generate(params);

        return createEntityWithCrew(ms);
    }

    /**
     * @param ms Which entity to generate
     * @return A crewed entity
     */
    public @Nullable Entity createEntityWithCrew(MechSummary ms) {
        Entity en;
        try {
            en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception ex) {
            LogManager.getLogger().error("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName(), ex);
            return null;
        }
        Faction faction = Factions.getInstance().getFaction(factionCode);

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());
        Gender gender = RandomGenderGenerator.generate();
        String[] crewNameArray = rng.generateGivenNameSurnameSplit(gender, faction.isClan(), faction.getShortName());
        String crewName = crewNameArray[0];
        crewName += !StringUtil.isNullOrEmpty(crewNameArray[1]) ?  " " + crewNameArray[1] : "";

        Map<Integer, Map<String, String>> extraData = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put(Crew.MAP_GIVEN_NAME, crewNameArray[0]);
        innerMap.put(Crew.MAP_SURNAME, crewNameArray[1]);

        final AbstractSkillGenerator skillGenerator = new TaharqaSkillGenerator();
        skillGenerator.setLevel(skill);
        if (faction.isClan()) {
            skillGenerator.setType(SkillGeneratorType.CLAN);
        }
        int[] skills = skillGenerator.generateRandomSkills(en);

        if (faction.isClan() && (Compute.d6(2) > (6 - skill.ordinal() + skills[0] + skills[1]))) {
            Phenotype phenotype = Phenotype.NONE;
            switch (en.getUnitType()) {
                case UnitType.MEK:
                    phenotype = Phenotype.MECHWARRIOR;
                    break;
                case UnitType.TANK:
                case UnitType.VTOL:
                    // The Vehicle Phenotype is unique to Clan Hell's Horses
                    if (faction.getShortName().equals("CHH")) {
                        phenotype = Phenotype.VEHICLE;
                    }
                    break;
                case UnitType.BATTLE_ARMOR:
                    phenotype = Phenotype.ELEMENTAL;
                    break;
                case UnitType.AERO:
                case UnitType.CONV_FIGHTER:
                    phenotype = Phenotype.AEROSPACE;
                    break;
                case UnitType.PROTOMEK:
                    phenotype = Phenotype.PROTOMECH;
                    break;
                case UnitType.SMALL_CRAFT:
                case UnitType.DROPSHIP:
                case UnitType.JUMPSHIP:
                case UnitType.WARSHIP:
                    // The Naval Phenotype is unique to Clan Snow Raven and the Raven Alliance
                    if (faction.getShortName().equals("CSR") || faction.getShortName().equals("RA")) {
                        phenotype = Phenotype.NAVAL;
                    }
                    break;
            }

            if (phenotype != Phenotype.NONE) {
                String bloodname = Bloodname.randomBloodname(faction.getShortName(), phenotype,
                        campaign.getGameYear()).getName();
                crewName += " " + bloodname;
                innerMap.put(Crew.MAP_BLOODNAME, bloodname);
                innerMap.put(Crew.MAP_PHENOTYPE, phenotype.name());
            }
        }

        extraData.put(0, innerMap);

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName, Compute.getFullCrewSize(en),
                skills[0], skills[1], gender, extraData));

        en.setExternalIdAsString(UUID.randomUUID().toString());
        return en;
    }

    private int getWeightClass(GammaDistribution gamma) {
        double weightClass = gamma.sample();
        weightClass = (weightClass > EntityWeightClass.WEIGHT_ASSAULT) ? EntityWeightClass.WEIGHT_ASSAULT : weightClass;
        weightClass = (weightClass < EntityWeightClass.WEIGHT_LIGHT) ? EntityWeightClass.WEIGHT_LIGHT : weightClass;
        return (int) Math.round(weightClass);
    }

    private int calculateMaxPoints(List<Unit> playerUnits) {
        int maxPoints = 0;
        for(Unit u : playerUnits) {
            maxPoints += calculatePoints(u.getEntity());
        }

        maxPoints = (int) Math.ceil(maxPoints * forceMultiplier);
        return maxPoints;
    }

    private int calculateStartingPoints(List<Entity> botEntities) {
        int startPoints = 0;
        for(Entity e : botEntities) {
            startPoints += calculatePoints(e);
        }

        return startPoints;
    }

    private double calculatePoints(Entity e) {
        if(balancingMethod == BalancingMethod.BV) {
            return e.calculateBattleValue();
        } else if(balancingMethod == BalancingMethod.WEIGHT_ADJ) {
            return getSimplePointScore(e);
        }
        return e.getWeight();
    }

    private static double getSimplePointScore(Entity e) {
        double points = e.getWeight();

        double multiplier;
        switch(e.getUnitType()) {
            case UnitType.MEK:
            case UnitType.AERO:
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.WARSHIP:
            case UnitType.PROTOMEK:
                multiplier = 1.0;
                break;
            case UnitType.TANK:
            case UnitType.VTOL:
            case UnitType.NAVAL:
                multiplier = 0.6;
                break;
            case UnitType.CONV_FIGHTER:
                multiplier = 0.4;
                break;
            case UnitType.BATTLE_ARMOR:
                points = 10;
                multiplier = 1;
                break;
            case UnitType.INFANTRY:
                points = 0.5;
                multiplier = 1;
                break;
            case UnitType.GUN_EMPLACEMENT:
                multiplier = 0.2;
                break;
            default:
                multiplier = 0;
        }

        return points * multiplier;

    }

    private double calculateMeanWeightClass(List<Unit> playerUnits) {
        int sumWeightClass = 0;
        int nUnits = 0;
        for(Unit u : playerUnits) {
            sumWeightClass += u.getEntity().getWeightClass();
            nUnits += 1;
        }

        if(nUnits == 0 | sumWeightClass == 0) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }

        return sumWeightClass / ((double) nUnits);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<botForceRandomizer>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<factionCode>"
                +factionCode
                +"</factionCode>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<quality>"
                +quality
                +"</quality>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<skill>"
                +skill.name()
                +"</skill>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<unitType>"
                +unitType
                +"</unitType>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<lanceSize>"
                +lanceSize
                +"</lanceSize>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<focalWeightClass>"
                +focalWeightClass
                +"</focalWeightClass>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<forceMultiplier>"
                +forceMultiplier
                +"</forceMultiplier>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<balancingMethod>"
                +balancingMethod.name()
                +"</balancingMethod>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<percentConventional>"
                +percentConventional
                +"</percentConventional>");
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</botForceRandomizer>");
    }

    public static BotForceRandomizer generateInstanceFromXML(Node wn, Campaign c, Version version) {
        BotForceRandomizer retVal = new BotForceRandomizer();

        retVal.campaign = c;
        try {
            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("factionCode")) {
                    retVal.factionCode = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
                    retVal.quality = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("unitType")) {
                    retVal.unitType = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    retVal.skill = SkillLevel.valueOf(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("lanceSize")) {
                    retVal.lanceSize = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("focalWeightClass")) {
                    retVal.focalWeightClass = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceMultiplier")) {
                    retVal.forceMultiplier = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("percentConventional")) {
                    retVal.percentConventional = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("balancingMethod")) {
                    retVal.balancingMethod = BalancingMethod.valueOf(wn2.getTextContent().trim());
                }
            }
        }  catch (Exception ex) {
            LogManager.getLogger().error(ex);
        }

        return retVal;
    }

}
