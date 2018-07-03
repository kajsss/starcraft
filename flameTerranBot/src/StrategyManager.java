import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import bwapi.Race;
import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

/// 상황을 판단하여, 정찰, 빌드, 공격, 방어 등을 수행하도록 총괄 지휘를 하는 class <br>
/// InformationManager 에 있는 정보들로부터 상황을 판단하고, <br>
/// BuildManager 의 buildQueue에 빌드 (건물 건설 / 유닛 훈련 / 테크 리서치 / 업그레이드) 명령을 입력합니다.<br>
/// 정찰, 빌드, 공격, 방어 등을 수행하는 코드가 들어가는 class
public class StrategyManager {

	private static StrategyManager instance = new StrategyManager();

	private CommandUtil commandUtil = new CommandUtil();
	private HashSet<Integer> attackToEnemyMainBaseControl = new HashSet<>();
	private boolean isFullScaleAttackStarted;
	private boolean isInitialBuildOrderFinished;

	// BasicBot 1.1 Patch Start ////////////////////////////////////////////////
	// 경기 결과 파일 Save / Load 및 로그파일 Save 예제 추가를 위한 변수 및 메소드 선언

	/// 한 게임에 대한 기록을 저장하는 자료구조
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

	/// 과거 전체 게임들의 기록을 저장하는 자료구조
	ArrayList<GameRecord> gameRecordList = new ArrayList<GameRecord>();

	// BasicBot 1.1 Patch End //////////////////////////////////////////////////

	/// static singleton 객체를 리턴합니다
	public static StrategyManager Instance() {
		return instance;
	}

	public StrategyManager() {
		isFullScaleAttackStarted = false;
		isInitialBuildOrderFinished = false;
	}

	/// 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
	public void onStart() {

		// BasicBot 1.1 Patch Start
		// ////////////////////////////////////////////////
		// 경기 결과 파일 Save / Load 및 로그파일 Save 예제 추가

		// 과거 게임 기록을 로딩합니다
		loadGameRecordList();

		// BasicBot 1.1 Patch End
		// //////////////////////////////////////////////////

		setInitialBuildOrder();
	}

	/// 경기가 종료될 때 일회적으로 전략 결과 정리 관련 로직을 실행합니다
	public void onEnd(boolean isWinner) {

		// BasicBot 1.1 Patch Start
		// ////////////////////////////////////////////////
		// 경기 결과 파일 Save / Load 및 로그파일 Save 예제 추가

		// 과거 게임 기록 + 이번 게임 기록을 저장합니다
		saveGameRecordList(isWinner);

		// BasicBot 1.1 Patch End
		// //////////////////////////////////////////////////
	}

	/// 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
	public void update() {
		if (BuildManager.Instance().buildQueue.isEmpty()) {
			isInitialBuildOrderFinished = true;
		}

		executeWorkerTraining();

		executeSupplyManagement();

		executeBasicCombatUnitTraining();

		executeCombat();

		// BasicBot 1.1 Patch Start
		// ////////////////////////////////////////////////
		// 경기 결과 파일 Save / Load 및 로그파일 Save 예제 추가

		// 이번 게임의 로그를 남깁니다
		saveGameLog();

		// BasicBot 1.1 Patch End
		// //////////////////////////////////////////////////
	}

	public void setInitialBuildOrder() {
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Barracks,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue
				.queueAsLowestPriority(InformationManager.Instance().getRefineryBuildingType());
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Engineering_Bay);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Terran_Infantry_Armor, false);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Comsat_Station, false);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Academy);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Barracks,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Medic,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Medic,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Barracks,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Medic,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Stim_Packs, false);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.U_238_Shells, false);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Barracks,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Firebat,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Firebat,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
	}

	// 일꾼 계속 추가 생산
	public void executeWorkerTraining() {

		// InitialBuildOrder 진행중에는 아무것도 하지 않습니다
		if (isInitialBuildOrderFinished == false) {
			return;
		}

		if (MyBotModule.Broodwar.self().minerals() >= 50) {
			// workerCount = 현재 일꾼 수 + 생산중인 일꾼 수
			int workerCount = MyBotModule.Broodwar.self().allUnitCount(InformationManager.Instance().getWorkerType());

			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if (unit.getType().isResourceDepot()) {
					if (unit.isTraining()) {
						workerCount += unit.getTrainingQueue().size();
					}
				}
			}

			if (workerCount < 30) {
				for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
					if (unit.getType().isResourceDepot()) {
						if (unit.isTraining() == false) {
							// 빌드큐에 일꾼 생산이 1개는 있도록 한다
							if (BuildManager.Instance().buildQueue
									.getItemCount(InformationManager.Instance().getWorkerType(), null) == 0) {
								// std.cout + "worker enqueue" + std.endl;
								BuildManager.Instance().buildQueue.queueAsLowestPriority(
										new MetaType(InformationManager.Instance().getWorkerType()), false);
							}
						}
					}
				}
			}
		}
	}

	// Supply DeadLock 예방 및 SupplyProvider 가 부족해질 상황 에 대한 선제적 대응으로서<br>
	// SupplyProvider를 추가 건설/생산한다
	public void executeSupplyManagement() {

		// BasicBot 1.1 Patch Start
		// ////////////////////////////////////////////////
		// 가이드 추가 및 콘솔 출력 명령 주석 처리

		// InitialBuildOrder 진행중 혹은 그후라도 서플라이 건물이 파괴되어 데드락이 발생할 수 있는데, 이 상황에 대한
		// 해결은 참가자께서 해주셔야 합니다.
		// 오버로드가 학살당하거나, 서플라이 건물이 집중 파괴되는 상황에 대해 무조건적으로 서플라이 빌드 추가를 실행하기 보다 먼저
		// 전략적 대책 판단이 필요할 것입니다

		// BWAPI::Broodwar->self()->supplyUsed() >
		// BWAPI::Broodwar->self()->supplyTotal() 인 상황이거나
		// BWAPI::Broodwar->self()->supplyUsed() + 빌드매니저 최상단 훈련 대상 유닛의
		// unit->getType().supplyRequired() >
		// BWAPI::Broodwar->self()->supplyTotal() 인 경우
		// 서플라이 추가를 하지 않으면 더이상 유닛 훈련이 안되기 때문에 deadlock 상황이라고 볼 수도 있습니다.
		// 저그 종족의 경우 일꾼을 건물로 Morph 시킬 수 있기 때문에 고의적으로 이런 상황을 만들기도 하고,
		// 전투에 의해 유닛이 많이 죽을 것으로 예상되는 상황에서는 고의적으로 서플라이 추가를 하지 않을수도 있기 때문에
		// 참가자께서 잘 판단하셔서 개발하시기 바랍니다.

		// InitialBuildOrder 진행중에는 아무것도 하지 않습니다
		if (isInitialBuildOrderFinished == false) {
			return;
		}

		// 1초에 한번만 실행
		if (MyBotModule.Broodwar.getFrameCount() % 24 != 0) {
			return;
		}

		// 게임에서는 서플라이 값이 200까지 있지만, BWAPI 에서는 서플라이 값이 400까지 있다
		// 저글링 1마리가 게임에서는 서플라이를 0.5 차지하지만, BWAPI 에서는 서플라이를 1 차지한다
		if (MyBotModule.Broodwar.self().supplyTotal() <= 400) {

			// 서플라이가 다 꽉찼을때 새 서플라이를 지으면 지연이 많이 일어나므로, supplyMargin (게임에서의 서플라이
			// 마진 값의 2배)만큼 부족해지면 새 서플라이를 짓도록 한다
			// 이렇게 값을 정해놓으면, 게임 초반부에는 서플라이를 너무 일찍 짓고, 게임 후반부에는 서플라이를 너무 늦게 짓게 된다
			int supplyMargin = 12;

			// currentSupplyShortage 를 계산한다
			int currentSupplyShortage = MyBotModule.Broodwar.self().supplyUsed() + supplyMargin
					- MyBotModule.Broodwar.self().supplyTotal();

			if (currentSupplyShortage > 0) {

				// 생산/건설 중인 Supply를 센다
				int onBuildingSupplyCount = 0;

				onBuildingSupplyCount += ConstructionManager.Instance().getConstructionQueueItemCount(
						InformationManager.Instance().getBasicSupplyProviderUnitType(), null)
						* InformationManager.Instance().getBasicSupplyProviderUnitType().supplyProvided();

				if (currentSupplyShortage > onBuildingSupplyCount) {
					// BuildQueue 최상단에 SupplyProvider 가 있지 않으면 enqueue 한다
					boolean isToEnqueue = true;
					if (!BuildManager.Instance().buildQueue.isEmpty()) {
						BuildOrderItem currentItem = BuildManager.Instance().buildQueue.getHighestPriorityItem();
						if (currentItem.metaType.isUnit() && currentItem.metaType.getUnitType() == InformationManager
								.Instance().getBasicSupplyProviderUnitType()) {
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

		// BasicBot 1.1 Patch End
		// ////////////////////////////////////////////////
	}

	public void executeBasicCombatUnitTraining() {

		// InitialBuildOrder 진행중에는 아무것도 하지 않습니다
		if (isInitialBuildOrderFinished == false) {
			return;
		}

		// 마린 추가 훈련
		if (MyBotModule.Broodwar.self().minerals() >= 150 && MyBotModule.Broodwar.self().supplyUsed() < 390) {
			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if (unit.getType() == UnitType.Terran_Barracks) {
					if (unit.isTraining() == false) {
						if (BuildManager.Instance().buildQueue
								.getItemCount(UnitType.Terran_Marine, null) == 0) {
							BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Marine, 
									BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
						}
					}
				}
			}
		}
		
		// 메딕이 3마리 이하일 경우 추가 훈련
		if (MyBotModule.Broodwar.self().minerals() >= 100 && 
			MyBotModule.Broodwar.self().gas() >= 30 && 
			MyBotModule.Broodwar.self().supplyUsed() < 390){
			 for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				 if (unit.getType() == UnitType.Terran_Barracks) {
					 if(unit.isTraining() == false){
						 if(MyBotModule.Broodwar.self().completedUnitCount(UnitType.Terran_Medic)<3){
							 if(BuildManager.Instance().buildQueue.getItemCount(UnitType.Terran_Medic, null)==0){
								 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Terran_Medic, 
										 BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
							 }
						 }
					 }
				 }
			}
		}
	}

	public void executeCombat() {
		// 공격 모드가 아닐 때에는 전투유닛들을 아군 진영 길목에 집결시켜서 방어
		if (isFullScaleAttackStarted == false) {
			Chokepoint firstChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
					.getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition());
			Chokepoint SecondChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
					.getSecondChokePoint(InformationManager.Instance().selfPlayer).getPoint());

			// 마린,파이어뱃,메딕의 숫자를 합하여 12명 이하일 경우 본진 언덕에 집결
			if (FlameTerranUnitTypeCount() <= 12) {
				for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
					if (FlameUnitType(unit) && unit.isIdle()) {
						commandUtil.attackMove(unit, firstChokePoint.getCenter());
					}
				}
			}

			// 마린,파이어뱃,메딕의 숫자를 합하여 12명 이상 25명 이하일 경우 앞마당 골목에 집결
			if (FlameTerranUnitTypeCount() > 12 && FlameTerranUnitTypeCount() <= 25) {
				for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
					if (FlameUnitType(unit) && unit.isIdle()) {
						commandUtil.attackMove(unit, SecondChokePoint.getCenter());
					}
				}
			}

			// 마린,파이어뱃,메딕의 숫자를 합하여 26명 이상일 경우 공격모드로 전환
			if (FlameTerranUnitTypeCount() > 25) {
				if (InformationManager.Instance().enemyPlayer != null
						&& InformationManager.Instance().enemyRace != Race.Unknown && InformationManager.Instance()
								.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer).size() > 0) {
					isFullScaleAttackStarted = true;
				}
			}

			// 수비모드에서 적이 쳐들어온 경우
			for (Unit selfUnit : MyBotModule.Broodwar.self().getUnits()) {
				// 건물은 제외
				if (selfUnit.getType().isBuilding()) {
					continue;
				}
				// 모든 일꾼은 제외
				if (selfUnit.getType().isWorker()) {
					continue;
				}
				int enemyCount = 0;
				// 시야의 적군이 2마리 이상이고 체력이 35 이상일 때 스팀팩 사용
				for (Unit enemyUnit : MyBotModule.Broodwar.enemy().getUnits()) {
					int unitDistance = selfUnit.getDistance(enemyUnit);

					if (unitDistance < selfUnit.getType().sightRange()) {
						enemyCount++;
					}

					if (enemyCount >= 2 && selfUnit.getHitPoints() >= 35) {
						selfUnit.useTech(TechType.Stim_Packs);
					}
				}
			}
		}
		// 공격 모드
		else {
			Chokepoint EnemySecondChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
					.getSecondChokePoint(InformationManager.Instance().enemyPlayer).getPoint());
			BaseLocation EnemyBaseLocation = InformationManager.Instance()
					.getMainBaseLocation(InformationManager.Instance().enemyPlayer);

			if (InformationManager.Instance().enemyPlayer != null
					&& InformationManager.Instance().enemyRace != Race.Unknown && InformationManager.Instance()
							.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer).size() > 0) {

				if (EnemySecondChokePoint != null) {
					for (Unit selfUnit : MyBotModule.Broodwar.self().getUnits()) {
						// 건물은 제외
						if (selfUnit.getType().isBuilding()) {
							continue;
						}
						// 모든 일꾼은 제외
						if (selfUnit.getType().isWorker()) {
							continue;
						}

						int enemyCount = 0;
						if (selfUnit.canAttack()) {
							// 적군의 SecondChokePoint에 모인 아군 병력의 숫자를 카운트
							Chokepoint attackToSecondChokePoint = BWTA.getNearestChokepoint(selfUnit.getPosition());
							if (attackToSecondChokePoint.getCenter().getX() == EnemySecondChokePoint.getCenter().getX() && 
								attackToSecondChokePoint.getCenter().getY() == EnemySecondChokePoint.getCenter().getY()) {
								attackToEnemyMainBaseControl.add(selfUnit.getID());
							}
							// 적군의 SecondChokePoint에 15명이상 도착할 경우 적군 본진 공격
							if (attackToEnemyMainBaseControl.size() >= 15) {
								commandUtil.attackMove(selfUnit, EnemyBaseLocation.getPosition());
							} else {
								commandUtil.attackMove(selfUnit, EnemySecondChokePoint.getCenter());
							}
							
							for (Unit enemyUnit : MyBotModule.Broodwar.enemy().getUnits()) {
								int unitDistance = selfUnit.getDistance(enemyUnit);
								// 시야에 들어온 적군의 수를 카운트
								if (unitDistance < selfUnit.getType().sightRange()) {
									enemyCount++;
								}
								// 시야의 적군이 2마리 이상이고 체력이 35 이상일 때 스팀팩 사용
								if (enemyCount >= 2 && selfUnit.getHitPoints() >= 35) {
									selfUnit.useTech(TechType.Stim_Packs);
								}
									
								// 러커가 버로우 할때 스캔 사용
								if (enemyUnit.getType() == UnitType.Zerg_Lurker){
									if(enemyUnit.isBurrowed()){
										for (Unit findScanner : MyBotModule.Broodwar.self().getUnits()) {
											if(findScanner.getType() == UnitType.Terran_Comsat_Station){
												if (unitDistance <= selfUnit.getType().groundWeapon().maxRange()) {
													if (MyBotModule.Broodwar.getFrameCount() % 60 != 0) continue;
													findScanner.useTech(TechType.Scanner_Sweep, enemyUnit.getPosition());
												}
											}
										}
									}
								}
							}
						}
					}
				}
				// 마린,파이어뱃,메딕의 숫자를 합하여 10명이하일 때 수비모드로 전환
				if (FlameTerranUnitTypeCount() <= 12) {
					isFullScaleAttackStarted = false;
				}
			}
		}
	}

	private boolean FlameUnitType(Unit unit) {
		return unit.getType() == UnitType.Terran_Marine || unit.getType() == UnitType.Terran_Medic
				|| unit.getType() == UnitType.Terran_Firebat;
	}

	private int FlameTerranUnitTypeCount() {
		return MyBotModule.Broodwar.self().completedUnitCount(UnitType.Terran_Marine)
				+ MyBotModule.Broodwar.self().completedUnitCount(UnitType.Terran_Medic)
				+ MyBotModule.Broodwar.self().completedUnitCount(UnitType.Terran_Firebat);
	}

	// BasicBot 1.1 Patch Start ////////////////////////////////////////////////
	// 경기 결과 파일 Save / Load 및 로그파일 Save 예제 추가

	/// 과거 전체 게임 기록을 로딩합니다
	void loadGameRecordList() {

		// 과거의 게임에서 bwapi-data\write 폴더에 기록했던 파일은 대회 서버가 bwapi-data\read 폴더로
		// 옮겨놓습니다
		// 따라서, 파일 로딩은 bwapi-data\read 폴더로부터 하시면 됩니다

		// TODO : 파일명은 각자 봇 명에 맞게 수정하시기 바랍니다
		String gameRecordFileName = "c:\\starcraft\\bwapi-data\\read\\NoNameBot_GameRecord.dat";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(gameRecordFileName));

			System.out.println("loadGameRecord from file: " + gameRecordFileName);

			String currentLine;
			StringTokenizer st;
			GameRecord tempGameRecord;
			while ((currentLine = br.readLine()) != null) {

				st = new StringTokenizer(currentLine, " ");
				tempGameRecord = new GameRecord();
				if (st.hasMoreTokens()) {
					tempGameRecord.mapName = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.myName = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.myRace = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.myWinCount = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.myLoseCount = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.enemyName = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.enemyRace = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.enemyRealRace = st.nextToken();
				}
				if (st.hasMoreTokens()) {
					tempGameRecord.gameFrameCount = Integer.parseInt(st.nextToken());
				}

				gameRecordList.add(tempGameRecord);
			}
		} catch (FileNotFoundException e) {
			System.out.println("loadGameRecord failed. Could not open file :" + gameRecordFileName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/// 과거 전체 게임 기록 + 이번 게임 기록을 저장합니다
	void saveGameRecordList(boolean isWinner) {

		// 이번 게임의 파일 저장은 bwapi-data\write 폴더에 하시면 됩니다.
		// bwapi-data\write 폴더에 저장된 파일은 대회 서버가 다음 경기 때 bwapi-data\read 폴더로
		// 옮겨놓습니다

		// TODO : 파일명은 각자 봇 명에 맞게 수정하시기 바랍니다
		String gameRecordFileName = "c:\\starcraft\\bwapi-data\\write\\NoNameBot_GameRecord.dat";

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

	/// 이번 게임 중간에 상시적으로 로그를 저장합니다
	void saveGameLog() {

		// 100 프레임 (5초) 마다 1번씩 로그를 기록합니다
		// 참가팀 당 용량 제한이 있고, 타임아웃도 있기 때문에 자주 하지 않는 것이 좋습니다
		// 로그는 봇 개발 시 디버깅 용도로 사용하시는 것이 좋습니다
		if (MyBotModule.Broodwar.getFrameCount() % 100 != 0) {
			return;
		}

		// TODO : 파일명은 각자 봇 명에 맞게 수정하시기 바랍니다
		String gameLogFileName = "c:\\starcraft\\bwapi-data\\write\\NoNameBot_LastGameLog.dat";

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

	public void onUnitDestroy(Unit unit) {
	}
	// BasicBot 1.1 Patch End //////////////////////////////////////////////////

}