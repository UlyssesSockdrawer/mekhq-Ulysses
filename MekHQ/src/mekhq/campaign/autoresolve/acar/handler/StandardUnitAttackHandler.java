/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve.acar.handler;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Roll;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.action.AttackToHitData;
import mekhq.campaign.autoresolve.acar.action.StandardUnitAttack;
import mekhq.campaign.autoresolve.acar.report.AttackReporter;
import mekhq.campaign.autoresolve.component.EngagementControl;
import mekhq.campaign.autoresolve.component.Formation;

import java.util.Optional;

public class StandardUnitAttackHandler extends AbstractActionHandler {

    private final AttackReporter reporter;

    public StandardUnitAttackHandler(StandardUnitAttack action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new AttackReporter(game(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    @Override
    public void execute() {
        var attack = (StandardUnitAttack) getAction();
        var attackerOpt = game().getFormation(attack.getEntityId());
        var targetOpt = game().getFormation(attack.getTargetId());
        var attacker = attackerOpt.orElseThrow();
        var target = targetOpt.orElseThrow();

        // Using simplified damage as of Interstellar Operations (BETA) page 241
        var targetUnitOpt = ObjectUtility.getRandomItemSafe(target.getUnits());
        var targetUnit = targetUnitOpt.orElseThrow();
        resolveAttack(attacker, attack, target, targetUnit);
    }

    private void resolveAttack(Formation attacker, StandardUnitAttack attack, Formation target, SBFUnit targetUnit) {
        var attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
        var toHit = AttackToHitData.compileToHit(game(), attack);

        // Start of attack report
        reporter.reportAttackStart(attacker, attack.getUnitNumber(), target);

        if (toHit.cannotSucceed()) {
            reporter.reportCannotSucceed(toHit.getDesc());
        } else {
            reporter.reportToHitValue(toHit);
            Roll roll = Compute.rollD6(2);
            reporter.reportAttackRoll(roll, attacker);

            if (roll.getIntValue() < toHit.getValue()) {
                reporter.reportAttackMiss();
            } else {
                reporter.reportAttackHit();
                int damage = calculateDamage(attacker, attack, attackingUnit, target);

                int newArmor = Math.max(0, targetUnit.getCurrentArmor() - damage);
                reporter.reportDamageDealt(targetUnit, damage, newArmor);

                if (newArmor * 2 <= targetUnit.getCurrentArmor()) {
                    target.setHighStressEpisode();
                    reporter.reportStressEpisode();
                }

                targetUnit.setCurrentArmor(newArmor);

                if (target.isCrippled() && newArmor > 0) {
                    reporter.reportStressEpisode();
                    target.setHighStressEpisode();
                    reporter.reportUnitCrippled();
                }

                if (newArmor == 0) {
                    // Destroyed
                    reporter.reportUnitDestroyed();
                    target.setHighStressEpisode();
                    countKill(attackingUnit, targetUnit);
                } else {
                    // Check for critical hits if armor is now less than half original
                    if (newArmor * 2 < targetUnit.getArmor()) {
                        reporter.reportCriticalCheck();
                        var critRoll = Compute.rollD6(2);
                        var criticalRollResult = critRoll.getIntValue();
                        handleCrits(target, targetUnit, criticalRollResult, attackingUnit);
                    }
                }
            }
        }
    }

    private int calculateDamage(Formation attacker, StandardUnitAttack attack,
                                SBFUnit attackingUnit, Formation target) {
        int damage = 0;
        if (attack.getManeuverResult().equals(StandardUnitAttack.ManeuverResult.SUCCESS)) {
            damage += 1;
        }
        damage += attackingUnit.getCurrentDamage().getDamage(attack.getRange()).damage;

        damage = Math.max(1, processDamageByEngagementControl(attacker, target, damage));
        return damage;
    }

    private void handleCrits(Formation target, SBFUnit targetUnit, int criticalRollResult, SBFUnit attackingUnit) {
        switch (criticalRollResult) {
            case 2, 3, 4 -> reporter.reportNoCrit();
            case 5, 6, 7 -> {
                targetUnit.addTargetingCrit();
                reporter.reportTargetingCrit(targetUnit);
            }
            case 8, 9 -> {
                targetUnit.addDamageCrit();
                reporter.reportDamageCrit(targetUnit);
            }
            case 10, 11 -> {
                targetUnit.addTargetingCrit();
                targetUnit.addDamageCrit();
                reporter.reportTargetingCrit(targetUnit);
                reporter.reportDamageCrit(targetUnit);
            }
            default -> {
                countKill(attackingUnit, targetUnit);
                targetUnit.setCurrentArmor(0);
                target.setHighStressEpisode();
                reporter.reportUnitDestroyed();
            }
        }
    }


    private void countKill(SBFUnit attackingUnit, SBFUnit targetUnit) {
        var killers = attackingUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> simulationManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        var targets = targetUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> simulationManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        for (var target : targets) {
            ObjectUtility.getRandomItemSafe(killers).ifPresent(e -> e.addKill(target));
        }
    }

    private int processDamageByEngagementControl(Formation attacker, Formation target, int damage) {
        var engagementControlMemories = attacker.getMemory().getMemories("engagementControl");
        var engagement = engagementControlMemories
            .stream()
            .filter(f -> f.getOrDefault("targetFormationId", Entity.NONE).equals(target.getId()))
            .findFirst(); // there should be only one engagement control memory for this target

        if (engagement.isEmpty()) {
            return damage;
        }

        var isAttacker = (boolean) engagement.get().getOrDefault("attacker", false);

        if (!isAttacker) {
            return damage;
        }

        var wonEngagement = (boolean) engagement.get().getOrDefault("wonEngagementControl", false);
        var engagementControl = (EngagementControl) engagement.get().getOrDefault("engagementControl", EngagementControl.NONE);
        if (wonEngagement) {
            return processDamageEngagementControlVictory(engagementControl, damage);
        }
        return processDamageEngagementControlDefeat(engagementControl, damage);
    }

    private int processDamageEngagementControlDefeat(EngagementControl engagementControl, double damage) {
        if (engagementControl == EngagementControl.EVADE) {
            return (int) (damage * 0.5);
        }
        return (int) damage;
    }

    private int processDamageEngagementControlVictory(EngagementControl engagementControl, double damage) {
        return (int) switch(engagementControl) {
            case OVERRUN -> damage * 0.25;
            case FORCED_ENGAGEMENT -> damage * 0.5;
            default -> damage;
        };
    }
}
