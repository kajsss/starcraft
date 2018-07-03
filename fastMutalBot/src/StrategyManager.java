import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

import java.util.ArrayList;

// 상황을 판단하여, 정찰, 빌드, 공격, 방어 등을 수행하도록 총괄 지휘를 하는 class 
// InformationManager 에 있는 정보들로부터 상황을 판단하고, 
// BuildManager 의 buildQueue에 빌드 (건물 건설 / 유닛 훈련 / 테크 리서치 / 업그레이드) 명령을 입력합니다.
// 정찰, 빌드, 공격, 방어 등을 수행하는 코드가 들어가는 class
public class StrategyManager {

    private static StrategyManager instance = new StrategyManager();
    // 과거 전체 게임들의 기록을 저장하는 자료구조
    ArrayList<GameRecord> gameRecordList = new ArrayList<>();
    private CommandUtil commandUtil = new CommandUtil();
    private boolean isInitialBuildOrderFinished;

    private boolean isAttackingEnemy;

    public StrategyManager() {
        isAttackingEnemy = false;
        isInitialBuildOrderFinished = false;
    }

    // static singleton 객체를 리턴합니다
    public static StrategyManager Instance() {
        return instance;
    }

    // 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
    public void onStart() {
        // run initial build
        setInitialBuildOrder();
    }

    //  경기가 종료될 때 일회적으로 전략 결과 정리 관련 로직을 실행합니다
    public void onEnd(boolean isWinner) {
        saveGameRecordList(isWinner);
    }

    // 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
    public void update() {
        if (BuildManager.Instance().buildQueue.isEmpty()) {
            isInitialBuildOrderFinished = true;
        }

        executeSupplyManagement();
        executeBasicCombatUnitTraining();
        executeWorkerTraining();
        controlZergling();
        controlMutalisk();
        checkSupplyCount();
        saveGameLog();
    }

    private void setInitialBuildOrder() {
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        // 9 dron spawningpoll
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Spawning_Pool, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        // build gas
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Extractor);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        // overload
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Overlord);

        // zergling
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Lair);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Spire, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Hatchery, BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Overlord, BuildOrderItem.SeedPositionStrategy.MainBaseBackYard);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Overlord, BuildOrderItem.SeedPositionStrategy.MainBaseBackYard);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Extractor, BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(), BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(), BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(), BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);
        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.SeedPositionSpecified);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Overlord, BuildOrderItem.SeedPositionStrategy.MainBaseBackYard);

        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
        BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType());
    }

    // overload 정찰 중 죽을 경우
    private void checkSupplyCount() {
        if (MyBotModule.Broodwar.self().supplyTotal() > MyBotModule.Broodwar.self().supplyUsed()) {
            return;
        }

        int onBuildingSupplyCount = (int) MyBotModule.Broodwar.self().getUnits().stream()
                .filter(f1 -> f1.getType().equals(UnitType.Zerg_Egg))
                .filter(f2 -> f2.getBuildType().equals(UnitType.Zerg_Overlord))
                .count();

        onBuildingSupplyCount += (int) MyBotModule.Broodwar.self().getUnits().stream()
                .filter(f1 -> f1.getType().equals(UnitType.Zerg_Overlord))
                .filter(f2 -> f2.isConstructing())
                .count();

        if (onBuildingSupplyCount >= 1) {
            return;
        }

        boolean addQueue = true;
        if (!BuildManager.Instance().buildQueue.isEmpty()) {
            for (BuildOrderItem item : BuildManager.Instance().buildQueue.getQueue()) {
                if (item.metaType.getUnitType().isBuilding()) {
                    addQueue = false;
                    break;
                }
                if (item.metaType.getUnitType().equals(UnitType.Zerg_Overlord)) {
                    addQueue = false;
                    break;
                }
            }
        }

        if (addQueue) {
            BuildManager.Instance().buildQueue.queueAsHighestPriority(new MetaType(UnitType.Zerg_Overlord), true);
        }
    }

    private void controlZergling() {
        if (isAttackingEnemy) {
            BaseLocation enemyBaseLocation = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);
            MyBotModule.Broodwar.self().getUnits().stream()
                    .filter(f1 -> f1.getType().equals(UnitType.Zerg_Zergling))
                    .forEach(unit -> commandUtil.attackMove(unit, enemyBaseLocation.getPoint()));
        } else {
            Chokepoint enemySecondChokePoint = InformationManager.Instance().getSecondChokePoint(MyBotModule.Broodwar.enemy());
            if (MyBotModule.Broodwar.self().completedUnitCount(UnitType.Zerg_Zergling) >= 6 && enemySecondChokePoint != null) {
                MyBotModule.Broodwar.self().getUnits().stream()
                        .filter(f1 -> f1.getType().equals(UnitType.Zerg_Zergling))
                        .forEach(unit -> commandUtil.attackMove(unit, enemySecondChokePoint.getPoint()));
            } else {
                Chokepoint secondChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance().getSecondChokePoint(InformationManager.Instance().selfPlayer).getCenter());
                MyBotModule.Broodwar.self().getUnits().stream()
                        .filter(f1 -> f1.getType().equals(UnitType.Zerg_Zergling))
                        .forEach(unit -> commandUtil.attackMove(unit, secondChokePoint.getCenter()));
            }
        }
    }

    private void controlMutalisk() {
        if (MyBotModule.Broodwar.self().completedUnitCount(UnitType.Zerg_Mutalisk) < 3) {
            return;
        }

        BaseLocation enemyBaseLocation = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);
        // 처음 공격 개시
        if (!isAttackingEnemy) {
            isAttackingEnemy = true;
            MyBotModule.Broodwar.self().getUnits().stream()
                    .filter(f1 -> f1.getType().equals(UnitType.Zerg_Mutalisk))
                    .forEach(unit -> commandUtil.move(unit, enemyBaseLocation.getPosition()));
        } else {
            if (enemyBaseLocation.getMinerals().isEmpty()) {
                MyBotModule.Broodwar.self().getUnits().stream()
                        .filter(f1 -> f1.getType().equals(UnitType.Zerg_Mutalisk))
                        .forEach(unit -> commandUtil.attackMove(unit, enemyBaseLocation.getPosition()));
            } else if (findAttackTarget() == null) {
                MyBotModule.Broodwar.self().getUnits().stream()
                        .filter(f1 -> f1.getType().equals(UnitType.Zerg_Mutalisk))
                        .forEach(unit -> commandUtil.attackMove(unit, enemyBaseLocation.getPosition()));
            } else {
                MyBotModule.Broodwar.self().getUnits().stream()
                        .filter(f1 -> f1.getType().equals(UnitType.Zerg_Mutalisk))
                        .forEach(unit -> commandUtil.attackUnit(unit, findAttackTarget()));
            }
        }
    }

    private Unit findAttackTarget() {
        Unit target = null;
        for (Unit unit : MyBotModule.Broodwar.enemy().getUnits()) {
            if (unit.canAttack()) {
                target = unit;
                break;
            }
        }
        return target;
    }

    // 일꾼 계속 추가 생산
    public void executeWorkerTraining() {

        // InitialBuildOrder 진행중에는 아무것도 하지 않습니다
        if (!isInitialBuildOrderFinished ) {
            return;
        }

        // workerCount = 현재 일꾼 수 + 생산중인 일꾼 수
        int workerCount = MyBotModule.Broodwar.self().allUnitCount(InformationManager.Instance().getWorkerType());
        for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
            if (unit.getType().equals(UnitType.Zerg_Egg) && unit.isMorphing() && unit.getBuildType().equals(UnitType.Zerg_Drone)) {
                    workerCount++;
            }
        }

        if (workerCount < 30) {
            for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
                if (unit.getType().isResourceDepot()) {
                    if (!unit.isTraining() || unit.getLarva().size() > 0) {
                        // 빌드큐에 일꾼 생산이 1개는 있도록 한다
                        if (BuildManager.Instance().buildQueue.getItemCount(InformationManager.Instance().getWorkerType(), null) == 0) {
                            BuildManager.Instance().buildQueue.queueAsLowestPriority(new MetaType(InformationManager.Instance().getWorkerType()), false);
                        }
                    }
                }
            }
        }
    }

    // Supply DeadLock 예방 및 SupplyProvider 가 부족해질 상황 에 대한 선제적 대응으로서
    // SupplyProvider를 추가 건설/생산한다
    private void executeSupplyManagement() {
        // BasicBot 1.1 Patch Start ////////////////////////////////////////////////
        // 가이드 추가 및 콘솔 출력 명령 주석 처리

        // InitialBuildOrder 진행중 혹은 그후라도 서플라이 건물이 파괴되어 데드락이 발생할 수 있는데, 이 상황에 대한 해결은 참가자께서 해주셔야 합니다.
        // 오버로드가 학살당하거나, 서플라이 건물이 집중 파괴되는 상황에 대해  무조건적으로 서플라이 빌드 추가를 실행하기 보다 먼저 전략적 대책 판단이 필요할 것입니다

        // BWAPI::Broodwar->self()->supplyUsed() > BWAPI::Broodwar->self()->supplyTotal()  인 상황이거나
        // BWAPI::Broodwar->self()->supplyUsed() + 빌드매니저 최상단 훈련 대상 유닛의 unit->getType().supplyRequired() > BWAPI::Broodwar->self()->supplyTotal() 인 경우
        // 서플라이 추가를 하지 않으면 더이상 유닛 훈련이 안되기 때문에 deadlock 상황이라고 볼 수도 있습니다.
        // 저그 종족의 경우 일꾼을 건물로 Morph 시킬 수 있기 때문에 고의적으로 이런 상황을 만들기도 하고,
        // 전투에 의해 유닛이 많이 죽을 것으로 예상되는 상황에서는 고의적으로 서플라이 추가를 하지 않을수도 있기 때문에
        // 참가자께서 잘 판단하셔서 개발하시기 바랍니다.

        // InitialBuildOrder 진행중에는 아무것도 하지 않습니다
        if (!isInitialBuildOrderFinished) {
            return;
        }

        // 1초에 한번만 실행
        if (MyBotModule.Broodwar.getFrameCount() % 24 != 0) {
            return;
        }

        // 게임에서는 서플라이 값이 200까지 있지만, BWAPI 에서는 서플라이 값이 400까지 있다
        if (MyBotModule.Broodwar.self().supplyTotal() <= 400) {

            // 서플라이가 다 꽉찼을때 새 서플라이를 지으면 지연이 많이 일어나므로, supplyMargin (게임에서의 서플라이 마진 값의 2배)만큼 부족해지면 새 서플라이를 짓도록 한다
            // 이렇게 값을 정해놓으면, 게임 초반부에는 서플라이를 너무 일찍 짓고, 게임 후반부에는 서플라이를 너무 늦게 짓게 된다
            int supplyMargin = 12;

            // currentSupplyShortage 를 계산한다
            int currentSupplyShortage = MyBotModule.Broodwar.self().supplyUsed() + supplyMargin - MyBotModule.Broodwar.self().supplyTotal();
            if (currentSupplyShortage > 0) {
                // 생산/건설 중인 Supply를 센다
                int onBuildingSupplyCount = 0;
                // 저그 종족인 경우, 생산중인 Zerg_Overlord (Zerg_Egg) 를 센다. Hatchery 등 건물은 세지 않는다
                for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
                    if (unit.getType() == UnitType.Zerg_Egg && unit.getBuildType() == UnitType.Zerg_Overlord) {
                        onBuildingSupplyCount += UnitType.Zerg_Overlord.supplyProvided();
                    }
                    // 갓태어난 Overlord 는 아직 SupplyTotal 에 반영안되어서, 추가 카운트를 해줘야함
                    if (unit.getType() == UnitType.Zerg_Overlord && unit.isConstructing()) {
                        onBuildingSupplyCount += UnitType.Zerg_Overlord.supplyProvided();
                    }
                }

                if (currentSupplyShortage > onBuildingSupplyCount) {
                    // BuildQueue 최상단에 SupplyProvider 가 있지 않으면 enqueue 한다
                    boolean isToEnqueue = true;
                    if (!BuildManager.Instance().buildQueue.isEmpty()) {
                        BuildOrderItem currentItem = BuildManager.Instance().buildQueue.getHighestPriorityItem();
                        if (currentItem.metaType.isUnit()
                                && currentItem.metaType.getUnitType() == InformationManager.Instance().getBasicSupplyProviderUnitType()) {
                            isToEnqueue = false;
                        }
                    }
                    if (isToEnqueue) {
                        BuildManager.Instance().buildQueue.queueAsHighestPriority(
                                new MetaType(InformationManager.Instance().getBasicSupplyProviderUnitType()), true);
                    }
                }
            }
        }

        // BasicBot 1.1 Patch End ////////////////////////////////////////////////
    }

    private void executeBasicCombatUnitTraining() {
        // InitialBuildOrder 진행중에는 아무것도 하지 않습니다
        if (!isInitialBuildOrderFinished) {
            return;
        }

        // 기본 build order 완료 후 mutalisk 계속 생산
        if (BuildManager.Instance().buildQueue.getItemCount(UnitType.Zerg_Mutalisk, null) == 0) {
            BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, false);
            BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, false);
            BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Mutalisk, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, false);
            BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, false);
            BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, false);
        }
    }

    // BasicBot 1.1 Patch Start ////////////////////////////////////////////////
    // 과거 전체 게임 기록 + 이번 게임 기록을 저장합니다
    private void saveGameRecordList(boolean isWinner) {

        // 이번 게임의 파일 저장은 bwapi-data\write 폴더에 하시면 됩니다.
        // bwapi-data\write 폴더에 저장된 파일은 대회 서버가 다음 경기 때 bwapi-data\read 폴더로 옮겨놓습니다

        String gameRecordFileName = "C:\\Starcraft\\bwapi-data\\write\\" + Config.LogFilename;

        System.out.println("saveGameRecord to file: " + gameRecordFileName);

        String mapName = MyBotModule.Broodwar.mapFileName();
        mapName = mapName.replace(' ', '_');
        String enemyName = MyBotModule.Broodwar.enemy().getName();
        enemyName = enemyName.replace(' ', '_');
        String myName = MyBotModule.Broodwar.self().getName();
        myName = myName.replace(' ', '_');

        /// 이번 게임에 대한 기록
        GameRecord thisGameRecord = new GameRecord();
        thisGameRecord.mapName = mapName;
        thisGameRecord.myName = myName;
        thisGameRecord.myRace = MyBotModule.Broodwar.self().getRace().toString();
        thisGameRecord.enemyName = enemyName;
        thisGameRecord.enemyRace = MyBotModule.Broodwar.enemy().getRace().toString();
        thisGameRecord.enemyRealRace = InformationManager.Instance().enemyRace.toString();
        thisGameRecord.gameFrameCount = MyBotModule.Broodwar.getFrameCount();
        if (isWinner) {
            thisGameRecord.myWinCount = 1;
            thisGameRecord.myLoseCount = 0;
        } else {
            thisGameRecord.myWinCount = 0;
            thisGameRecord.myLoseCount = 1;
        }

        // 이번 게임 기록을 전체 게임 기록에 추가
        gameRecordList.add(thisGameRecord);

        // 전체 게임 기록 write
        StringBuilder ss = new StringBuilder();
        for (GameRecord gameRecord : gameRecordList) {
            ss.append(gameRecord.mapName + " ");
            ss.append(gameRecord.myName + " ");
            ss.append(gameRecord.myRace + " ");
            ss.append(gameRecord.myWinCount + " ");
            ss.append(gameRecord.myLoseCount + " ");
            ss.append(gameRecord.enemyName + " ");
            ss.append(gameRecord.enemyRace + " ");
            ss.append(gameRecord.enemyRealRace + " ");
            ss.append(gameRecord.gameFrameCount + "\n");
        }

        Common.overwriteToFile(gameRecordFileName, ss.toString());
    }

    // 이번 게임 중간에 상시적으로 로그를 저장합니다
    private void saveGameLog() {
        // 100 프레임 (5초) 마다 1번씩 로그를 기록합니다
        // 참가팀 당 용량 제한이 있고, 타임아웃도 있기 때문에 자주 하지 않는 것이 좋습니다
        // 로그는 봇 개발 시 디버깅 용도로 사용하시는 것이 좋습니다
        if (MyBotModule.Broodwar.getFrameCount() % 100 != 0) {
            return;
        }

        String gameLogFileName = "C:\\Starcraft\\bwapi-data\\write\\" + Config.LogFilename;

        String mapName = MyBotModule.Broodwar.mapFileName();
        mapName = mapName.replace(' ', '_');
        String enemyName = MyBotModule.Broodwar.enemy().getName();
        enemyName = enemyName.replace(' ', '_');
        String myName = MyBotModule.Broodwar.self().getName();
        myName = myName.replace(' ', '_');

        StringBuilder ss = new StringBuilder();
        ss.append(mapName + " ");
        ss.append(myName + " ");
        ss.append(MyBotModule.Broodwar.self().getRace().toString() + " ");
        ss.append(enemyName + " ");
        ss.append(InformationManager.Instance().enemyRace.toString() + " ");
        ss.append(MyBotModule.Broodwar.getFrameCount() + " ");
        ss.append(MyBotModule.Broodwar.self().supplyUsed() + " ");
        ss.append(MyBotModule.Broodwar.self().supplyTotal() + "\n");

        Common.appendTextToFile(gameLogFileName, ss.toString());
    }

    // 한 게임에 대한 기록을 저장하는 자료구조
    private class GameRecord {
        String mapName;
        String enemyName;
        String enemyRace;
        String enemyRealRace;
        String myName;
        String myRace;
        int gameFrameCount = 0;
        int myWinCount = 0;
        int myLoseCount = 0;
    }
    // BasicBot 1.1 Patch End //////////////////////////////////////////////////
}