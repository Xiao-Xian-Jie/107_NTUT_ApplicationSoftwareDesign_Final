package com.example.wesdx.a107_ntut_applicationsoftwaredesign_final;

import android.util.Log;

import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.API;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.LineStation;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RailDailyTimetable;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RailStation;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.StationOfLine;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.StopTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

//列車重複

public class Router {
    public static long TRANSFER_TIME = 10 * 60 * 1000;
    public static List<TrainPath> getTranserPath(String transportation, String date, String takeTimeString, List<RailStation> railStationList, RailStation originStation, RailStation destinationStation) {
        final SimpleDateFormat simpleDateFormat_HHmm = new SimpleDateFormat("HH:mm");
        List<TrainPath> trainPathList = new ArrayList<>();

        if(transportation.equals(API.TRA)) {
            try {
                Date takeTime = simpleDateFormat_HHmm.parse(takeTimeString);
                List<List<RailStation>> railStationList_List = MyRailStation.getRailStationList(railStationList, originStation, destinationStation);

                List<RailDailyTimetable> railDailyTimetableList_all = API.getDailyTimetable(transportation, API.TRAIN_DATE, date);

                for(int i = 0; i < railStationList_List.size(); i++) {
                    for(int j = i+1; j < railStationList_List.size(); j++) {
                        boolean same = true;
                        if(railStationList_List.get(i).size() == railStationList_List.get(j).size()) {
                            for(int k = 0; k < railStationList_List.get(i).size(); k++) {
                                if(!railStationList_List.get(i).get(k).StationID.equals(railStationList_List.get(j).get(k).StationID)) {
                                    same = false;
                                    break;
                                }
                            }
                            if(same) {
                                railStationList_List.remove(j);
                            }
                        }
                    }
                }

                for(List<RailStation> railStationList_current:railStationList_List) {
                    List<RailDailyTimetable> railDailyTimetableList = RailDailyTimetable.filterByPath(railDailyTimetableList_all, railStationList_current, true, 2);

                    if(railDailyTimetableList == null) {
                        continue;
                    }

                    for(RailDailyTimetable railDailyTimetable_mid:railDailyTimetableList) {
                        TrainPath trainPath = new TrainPath();
                        trainPath.trainPathPartList = new ArrayList<>();

                        RailStation firstRailStation, lastRailStation;
                        StopTime firstStopTime, lastStopTime;
                        Date firstTime, lastTime, firstTimeThreshold, lastTimeThreshold;

                        firstStopTime = railDailyTimetable_mid.findStopTime(railStationList_current);
                        lastStopTime = railDailyTimetable_mid.findLastStopTime(railStationList_current);
                        if((firstStopTime == null)||(lastStopTime == null)) {
                            continue;
                        }

                        firstTime = simpleDateFormat_HHmm.parse(firstStopTime.DepartureTime);
                        if(!railDailyTimetable_mid.beforeOverNightStation(firstStopTime.StationID)) {
                            firstTime.setDate(firstTime.getDate() + 1);
                        }
                        lastTime = simpleDateFormat_HHmm.parse(lastStopTime.DepartureTime);
                        if(!railDailyTimetable_mid.beforeOverNightStation(lastStopTime.StationID)) {
                            lastTime.setDate(lastTime.getDate() + 1);
                        }

                        if(firstTime.after(lastTime)) {
                            continue;
                        }

                        if(firstTime.before(takeTime)) {
                            continue;
                        }

                        firstTimeThreshold = new Date(firstTime.getTime() - TRANSFER_TIME);
                        lastTimeThreshold = new Date(lastTime.getTime() + TRANSFER_TIME);
                        firstRailStation = RailStation.find(railStationList_current, firstStopTime.StationID);
                        lastRailStation = RailStation.find(railStationList_current, lastStopTime.StationID);
                        if((firstRailStation == null)||(lastRailStation == null)) {
                            continue;
                        }

                        if(!firstStopTime.StationID.equals(originStation.StationID)) {
                            RailDailyTimetable railDailyTimetable_best = null;
                            Date arrivalFirstTime_best = null;

                            for(RailDailyTimetable railDailyTimetable_first:railDailyTimetableList) {
                                StopTime originStopTime, arrivalFirstStopTime;
                                Date originTime, arrivalFirstTime;

                                originStopTime = railDailyTimetable_first.getStopTimeOfStopTimes(originStation.StationID);
                                arrivalFirstStopTime = railDailyTimetable_first.getStopTimeOfStopTimes(firstStopTime.StationID);
                                if((originStopTime == null)||(arrivalFirstStopTime == null)) {
                                    continue;
                                }
                                originTime = simpleDateFormat_HHmm.parse(originStopTime.DepartureTime);
                                if(!railDailyTimetable_first.beforeOverNightStation(originStopTime.StationID)) {
                                    originTime.setDate(originTime.getDate() + 1);
                                }
                                arrivalFirstTime = simpleDateFormat_HHmm.parse(arrivalFirstStopTime.DepartureTime);
                                if(!railDailyTimetable_first.beforeOverNightStation(arrivalFirstStopTime.StationID)) {
                                    arrivalFirstTime.setDate(arrivalFirstTime.getDate() + 1);
                                }

                                if(originTime.after(arrivalFirstTime)) {
                                    continue;
                                }

                                if(originTime.before(takeTime)) {
                                    continue;
                                }

                                if(arrivalFirstTime.after(firstTimeThreshold)) {
                                    continue;
                                }

                                if(railDailyTimetable_best == null) {
                                    railDailyTimetable_best = railDailyTimetable_first;
                                    arrivalFirstTime_best = arrivalFirstTime;
                                } else {
                                    if(arrivalFirstTime.after(arrivalFirstTime_best)) {
                                        railDailyTimetable_best = railDailyTimetable_first;
                                        arrivalFirstTime_best = arrivalFirstTime;
                                    }
                                }
                            }

                            if(railDailyTimetable_best != null) {
                                TrainPath.TrainPathPart trainPathPart_first = new TrainPath.TrainPathPart();
                                trainPathPart_first.originStation = originStation;
                                trainPathPart_first.destinationStation = firstRailStation;
                                trainPathPart_first.railDailyTimetable = railDailyTimetable_best;
                                trainPath.trainPathPartList.add(trainPathPart_first);
                            } else {
                                continue;
                            }
                        }

                        TrainPath.TrainPathPart trainPathPart_mid = new TrainPath.TrainPathPart();
                        trainPathPart_mid.originStation = firstRailStation;
                        trainPathPart_mid.destinationStation = lastRailStation;
                        trainPathPart_mid.railDailyTimetable = railDailyTimetable_mid;
                        trainPath.trainPathPartList.add(trainPathPart_mid);

                        if(!lastRailStation.StationID.equals(destinationStation.StationID)) {
                            RailDailyTimetable railDailyTimetable_best = null;
                            Date destinationTime_best = null;

                            for(RailDailyTimetable railDailyTimetable_last:railDailyTimetableList) {
                                StopTime departureLastStopTime, destinationStopTime;
                                Date departureLastTime, destinationTime;

                                departureLastStopTime = railDailyTimetable_last.getStopTimeOfStopTimes(lastRailStation.StationID);
                                destinationStopTime = railDailyTimetable_last.getStopTimeOfStopTimes(destinationStation.StationID);
                                if((departureLastStopTime == null)||(destinationStopTime == null)) {
                                    continue;
                                }
                                departureLastTime = simpleDateFormat_HHmm.parse(departureLastStopTime.DepartureTime);
                                if(!railDailyTimetable_last.beforeOverNightStation(departureLastStopTime.StationID)) {
                                    departureLastTime.setDate(departureLastTime.getDate() + 1);
                                }
                                destinationTime = simpleDateFormat_HHmm.parse(destinationStopTime.DepartureTime);
                                if(!railDailyTimetable_last.beforeOverNightStation(destinationStopTime.StationID)) {
                                    destinationTime.setDate(destinationTime.getDate() + 1);
                                }

                                if(departureLastTime.after(destinationTime)) {
                                    continue;
                                }

                                if(departureLastTime.before(lastTimeThreshold)) {
                                    continue;
                                }

                                if(railDailyTimetable_best == null) {
                                    railDailyTimetable_best = railDailyTimetable_last;
                                    destinationTime_best = destinationTime;
                                } else {
                                    if(destinationTime.before(destinationTime_best)) {
                                        railDailyTimetable_best = railDailyTimetable_last;
                                        destinationTime_best = destinationTime;
                                    }
                                }
                            }

                            if(railDailyTimetable_best != null) {
                                TrainPath.TrainPathPart trainPathPart_last = new TrainPath.TrainPathPart();
                                trainPathPart_last.originStation = lastRailStation;
                                trainPathPart_last.destinationStation = destinationStation;
                                trainPathPart_last.railDailyTimetable = railDailyTimetable_best;
                                trainPath.trainPathPartList.add(trainPathPart_last);
                            } else {
                                continue;
                            }
                        }
                        trainPathList.add(trainPath);
                    }
                }

                Log.d("trainPathList", "/////////////////////////////////////");
                Log.d("trainPathList", "/////////////////////////////////////");
                Log.d("trainPathList", "/////////////////////////////////////");
                Log.d("trainPathList", "/////////////////////////////////////");
                Log.d("trainPathList", "/////////////////////////////////////");
                Log.d("trainPathList", "/////////////////////////////////////");
                for(TrainPath trainPath:trainPathList) {
                    String info = "";
                    for(TrainPath.TrainPathPart trainPathPart:trainPath.trainPathPartList) {
                        info += trainPathPart.originStation.StationName.Zh_tw;
                        info += "(" + trainPathPart.railDailyTimetable.getStopTimeOfStopTimes(trainPathPart.originStation.StationID).DepartureTime + ")";
                        info += trainPathPart.destinationStation.StationName.Zh_tw;
                        info += "(" + trainPathPart.railDailyTimetable.getStopTimeOfStopTimes(trainPathPart.destinationStation.StationID).ArrivalTime + ")";
                        info += "(No: " + trainPathPart.railDailyTimetable.DailyTrainInfo.TrainNo + ")";
                        info += " → ";
                    }
                    Log.d("trainPathList", info);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }

        } else if(transportation.equals(API.THSR)) {
            List<RailStation> railStationList_current = RailStation.getStationList(railStationList, originStation, destinationStation);
            List<RailDailyTimetable> railDailyTimetableList = API.getDailyTimetable(transportation, API.TRAIN_DATE, date);
            railDailyTimetableList = RailDailyTimetable.filterByOD(railDailyTimetableList, originStation, destinationStation, true);

            for(RailDailyTimetable railDailyTimetable:railDailyTimetableList) {
                TrainPath.TrainPathPart trainPathPart = new TrainPath.TrainPathPart();
                TrainPath trainPath = new TrainPath();
                trainPath.trainPathPartList = new ArrayList<>();
                trainPathPart.originStation = originStation;
                trainPathPart.destinationStation = destinationStation;
                trainPathPart.railDailyTimetable = railDailyTimetable;
                trainPath.trainPathPartList.add(trainPathPart);
                trainPathList.add(trainPath);
            }
        }

        trainPathList = TrainPath.filter(trainPathList);

        Collections.sort(trainPathList, new Comparator<TrainPath>(){
            public int compare(TrainPath obj1, TrainPath obj2) {
                try {
                    Date obj1ArrivalTime = simpleDateFormat_HHmm.parse(obj1.getLastItem().railDailyTimetable.getStopTimeOfStopTimes(obj1.getLastItem().destinationStation).ArrivalTime);
                    Date obj2ArrivalTime = simpleDateFormat_HHmm.parse(obj2.getLastItem().railDailyTimetable.getStopTimeOfStopTimes(obj2.getLastItem().destinationStation).ArrivalTime);
                    if(!obj1.getLastItem().railDailyTimetable.beforeOverNightStation(obj1.getLastItem().destinationStation.StationID)) {
                        obj1ArrivalTime.setDate(obj1ArrivalTime.getDate() + 1);
                    }
                    if(!obj2.getLastItem().railDailyTimetable.beforeOverNightStation(obj2.getLastItem().destinationStation.StationID)) {
                        obj2ArrivalTime.setDate(obj2ArrivalTime.getDate() + 1);
                    }
                    if (obj1ArrivalTime.after(obj2ArrivalTime)) {
                        return 1;
                    }
                    else if (obj1ArrivalTime.before(obj2ArrivalTime)) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });


        if(trainPathList.size() == 0) return null;
        for(int i = 10; i < (trainPathList != null ? trainPathList.size() : 0); i++) {
            trainPathList.remove(i);
            i--;
        }

        List<RailStation> railStationList_temp = null;
        List<RailStation> railStationList_temp2 = null;
        for(StationOfLine stationOfLine:MyRailStation.stationOfLineList) {
            for(LineStation lineStation:stationOfLine.Stations) {
                if(RailStation.find(railStationList, lineStation.StationID) != null) {
                    if(railStationList_temp == null) railStationList_temp = new ArrayList<>();
                    railStationList_temp.add(RailStation.find(railStationList, lineStation.StationID));
                }
            }
        }
        for(int i = 0; i < railStationList_temp.size(); i++) {
            for(int j = i + 1; j < railStationList_temp.size(); j++) {
                if(railStationList_temp.get(i).StationID.equals(railStationList_temp.get(j).StationID)) {
                    railStationList_temp.remove(j);
                    j--;
                }
            }
        }
        for(RailStation railStation_temp1:railStationList) {
            boolean find = false;
            for(RailStation railStation_tmep2:railStationList_temp) {
                if(railStation_temp1.StationID.equals(railStation_tmep2.StationID)) {
                    find = true;
                    break;
                }
            }
            if(!find) {
                if(railStationList_temp2 == null) railStationList_temp2 = new ArrayList<>();
                railStationList_temp2.add(railStation_temp1);
            }
        }


        return trainPathList;
    }

    public static List<RailDailyTimetable> get(String transportation, String date, String takeTimeString, List<RailStation> railStationList, RailStation originStation, RailStation destinationStation) {
        List<RailDailyTimetable> railDailyTimetableList_new = new ArrayList<>();

        List<RailDailyTimetable> railDailyTimetableList = API.getDailyTimetable(transportation, API.TRAIN_DATE, date);
        railDailyTimetableList = RailDailyTimetable.filter(railDailyTimetableList, originStation, destinationStation);
        railDailyTimetableList = RailDailyTimetable.filter(railDailyTimetableList, originStation, destinationStation, takeTimeString, "24:00");
        //RailDailyTimetable.sort(railDailyTimetableList, originStation);

        for(int i = 30; i < (railDailyTimetableList != null ? railDailyTimetableList.size() : 0); i++) {
            railDailyTimetableList.remove(i);
            i--;
        }

        railDailyTimetableList_new = railDailyTimetableList;


        List<TrainPath> trainPathList = getTranserPath(transportation, date, takeTimeString, railStationList, originStation, destinationStation);


        return railDailyTimetableList_new;
    }
}
