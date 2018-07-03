import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;


// 게임 초반에 일꾼 유닛 중에서 정찰 유닛을 하나 지정하고, 정찰 유닛을 이동시켜 정찰을 수행하는 class
// 적군의 BaseLocation 위치를 알아내는 것까지만 개발되어있습니다
public class ScoutManager {

    private static ScoutManager instance = new ScoutManager();
    private Unit firstScoutUnit;
    private int firstScoutUnitStatus;
    private Unit secondScoutUnit;
    private BaseLocation firstScoutTargetBaseLocation = null;
    private BaseLocation secondScoutTargetBaseLocation = null;
    private CommandUtil commandUtil = new CommandUtil();

    // static singleton 객체를 리턴합니다
    public static ScoutManager Instance() {
        return instance;
    }

    public void onStart() {
        MyBotModule.Broodwar.self().getUnits().stream()
                .filter(f -> f.getType().equals(UnitType.Zerg_Overlord))
                .forEach(unit -> firstScoutUnit = unit);
        firstScoutUnitStatus = ScoutStatus.MovingToAnotherBaseLocation.ordinal();

        // assign a scout to go scout it
        firstScoutTargetBaseLocation = getClosestBaseLocation();
        commandUtil.move(firstScoutUnit, firstScoutTargetBaseLocation.getPosition());
    }

    // 정찰 유닛을 지정하고, 정찰 상태를 업데이트하고, 정찰 유닛을 이동시킵니다
    public void update() {
        // 1초에 4번만 실행합니다
        if (MyBotModule.Broodwar.getFrameCount() % 6 != 0) {
            return;
        }

        // scoutUnit 을 지정하고, scoutUnit 의 이동을 컨트롤함.
        moveScoutUnit();
    }

    // 정찰 유닛을 필요하면 새로 지정합니다
    public void assignScoutIfNeeded(Unit unit) {
        BaseLocation enemyBaseLocation = InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.enemy());
        if (enemyBaseLocation == null && secondScoutUnit == null) {
            secondScoutUnit = unit;
        }
    }

    // 정찰 유닛을 이동시킵니다
    private void moveScoutUnit() {
        BaseLocation enemyBaseLocation = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);
        if (enemyBaseLocation == null) {
            // assign a scout to go scout it
            if (secondScoutUnit != null) {
                if (secondScoutTargetBaseLocation == null) {
                    secondScoutTargetBaseLocation = getClosestBaseLocation();
                } else {
                    if (MyBotModule.Broodwar.isExplored(secondScoutTargetBaseLocation.getTilePosition())) {
                        secondScoutTargetBaseLocation = getClosestBaseLocation();
                    } else {
                        commandUtil.move(secondScoutUnit, secondScoutTargetBaseLocation.getPosition());
                    }
                }
            }

            if (MyBotModule.Broodwar.isExplored(firstScoutTargetBaseLocation.getTilePosition())) {
                // assign a scout to go scout it
                firstScoutTargetBaseLocation = getClosestBaseLocation();
                commandUtil.move(firstScoutUnit, firstScoutTargetBaseLocation.getPosition());
            }
        } else {
            boolean isHydraliskDen = false;
            for (Unit unit : MyBotModule.Broodwar.enemy().getUnits()) {
                // morphing hydralisk_den
                if (unit.getBuildType().equals(UnitType.Zerg_Hydralisk_Den)) {
                    isHydraliskDen = true;
                    break;
                }

                // complete hydralisk_den
                if (unit.getType().equals(UnitType.Zerg_Hydralisk_Den)) {
                    isHydraliskDen = true;
                    break;
                }
            }

            if (MyBotModule.Broodwar.enemy().getRace().equals(Race.Terran) || isHydraliskDen) {
                moveScoutUnitToMyBaseLocation();
            }

            if (firstScoutUnit.isUnderAttack()) {
                moveScoutUnitToMyBaseLocation();
            }

            if (secondScoutUnit != null && secondScoutUnit.isUnderAttack()) {
                BaseLocation myMainBaseLocation = InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.self());
                commandUtil.move(secondScoutUnit, myMainBaseLocation.getPosition());
            }
        }
    }

    private void moveScoutUnitToMyBaseLocation() {
        BaseLocation myFirstExpansionLocation = InformationManager.Instance().getFirstExpansionLocation(MyBotModule.Broodwar.self());
        commandUtil.move(firstScoutUnit, myFirstExpansionLocation.getPoint());
        if (secondScoutUnit != null) {
            BaseLocation myMainBaseLocation = InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.self());
            commandUtil.move(secondScoutUnit, myMainBaseLocation.getPosition());
        }
    }

    private BaseLocation getClosestBaseLocation() {
        double closestDistance = 1000000000;
        double tempDistance;
        BaseLocation closestBaseLocation = null;
        // 아군 MainBaseLocation 으로부터 가장 가까운 미정찰 BaseLocation 을 새로운 정찰 대상 currentScoutTargetBaseLocation 으로 잡아서 이동
        for (BaseLocation startLocation : BWTA.getStartLocations()) {
            if (MyBotModule.Broodwar.isExplored(startLocation.getTilePosition())) {
                continue;
            }

            if (startLocation == firstScoutTargetBaseLocation) {
                continue;
            }

            if (startLocation == secondScoutTargetBaseLocation) {
                continue;
            }

            tempDistance = InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.self()).getGroundDistance(startLocation) + 0.5;
            if (tempDistance > 0 && tempDistance < closestDistance) {
                closestBaseLocation = startLocation;
                closestDistance = tempDistance;
            }
        }

        return closestBaseLocation;
    }

    // 정찰 유닛을 리턴합니다
    public Unit getScoutUnit() {
        return firstScoutUnit;
    }

    // 정찰 상태를 리턴합니다
    public int getScoutStatus() {
        return firstScoutUnitStatus;
    }

    public enum ScoutStatus {
        NoScout,                        // 정찰 유닛을 미지정한 상태
        MovingToAnotherBaseLocation,    // 적군의 BaseLocation 이 미발견된 상태에서 정찰 유닛을 이동시키고 있는 상태
        MoveAroundEnemyBaseLocation    // 적군의 BaseLocation 이 발견된 상태에서 정찰 유닛을 이동시키고 있는 상태
    }
}