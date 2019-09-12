package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class CivilianHelpBuiltInScenario extends AtBScenario {
	private static final long serialVersionUID = 1757542171960038919L;
	private static final String CIVILIAN_FORCE_ID = "Civilians";
	
	@Override
	public boolean isSpecialMission() {
		return true;
	}

	@Override
	public int getScenarioType() {
		return CIVILIANHELP;
	}

	@Override
	public String getScenarioTypeDescription() {
		return "Special Mission: Civilian Help";
	}

	@Override
	public String getResourceKey() {
		return "civilianHelp";
	}

	@Override
	public boolean canDeploy(Unit unit, Campaign campaign) {
		return unit.getCommander().getRank().isOfficer();
	}

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
			ArrayList<Entity> enemyEntities) {
		setStart(startPos[Compute.randomInt(4)]);
		int enemyStart = getStart() + 4;

		if (enemyStart > 8) {
			enemyStart -= 8;
		}

		for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
			enemyEntities = new ArrayList<Entity>();
			for (int i = 0; i < 3; i++)
				enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
						getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
			getSpecMissionEnemies().add(enemyEntities);
		}

		addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));

		ArrayList<Entity> otherForce = new ArrayList<Entity>();
		addCivilianUnits(otherForce, 4, campaign);

		for (Entity e : otherForce) {
			getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
		}

		addBotForce(new BotForce(CIVILIAN_FORCE_ID, 1, getStart(), otherForce));
	}
	
	@Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
	    super.setObjectives(campaign, contract);
	    
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 66);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 1, true);
        ScenarioObjective keepCiviliansAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(CIVILIAN_FORCE_ID, 1, true);
        
        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepCiviliansAlive.addSuccessEffect(bonusEffect);
        keepCiviliansAlive.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"), bonusEffect.howMuch));
        
        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
        getObjectives().add(keepCiviliansAlive);
    }
}
