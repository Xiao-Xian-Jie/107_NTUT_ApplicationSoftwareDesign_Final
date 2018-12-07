package com.example.wesdx.a107_ntut_applicationsoftwaredesign_final;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.API;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RailDailyTimetable;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RailStation;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.StationOfLine;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.StopTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Router {
    public static class RouterException extends Exception {
        public static String INPUT_OBJECT_IS_NULL = "Input object is null";
        public static String ORIGINSTATION_EQUALS_DESTINATIONSTATION = "Origin station equals destination station";
        public RouterException(String message)
        {
            super(message);
        }
    }

    public static long TRANSFER_TIME = 10 * 60 * 1000;
    public static List<StationOfLine> stationOfLineList;

    public static String TRARailDailyTimetableListCacheDate;
    public static List<RailDailyTimetable> TRARailDailyTimetableListCache;
    public static String THSRRailDailyTimetableListCacheDate;
    public static List<RailDailyTimetable> THSRRailDailyTimetableListCache;

    public static List<RailDailyTimetable> getFromCache(String transportation, String date) {
        if(transportation.equals(API.TRA) && date.equals(TRARailDailyTimetableListCacheDate)) {
            return new ArrayList<>(TRARailDailyTimetableListCache);
        } else if(transportation.equals(API.THSR) && date.equals(THSRRailDailyTimetableListCacheDate)) {
            return new ArrayList<>(THSRRailDailyTimetableListCache);
        }
        return null;
    }

    public static void seveToCache(String transportation, String date, List<RailDailyTimetable> railDailyTimetableList) {
        if(transportation.equals(API.TRA)) {
            TRARailDailyTimetableListCacheDate = date;
            TRARailDailyTimetableListCache = railDailyTimetableList;
        } else if(transportation.equals(API.THSR)) {
            THSRRailDailyTimetableListCacheDate = date;
            THSRRailDailyTimetableListCache = railDailyTimetableList;
        }
    }

    public static List<TrainPath> getTrainPath(String transportation, String date, Date originDepartureTime, Date destinationArrivalTime, List<RailDailyTimetable> railDailyTimetableList_input, List<RailStation> railStationList, RailStation originStation, RailStation destinationStation, boolean isDirectArrival) throws ParseException, RouterException {
        List<TrainPath> trainPathList = new ArrayList<>();

        if(stationOfLineList == null) {
            stationOfLineList = API.getStationOfLine(API.TRA);
            StationOfLine.fixMissing15StationProblem(stationOfLineList);
            if(stationOfLineList == null) return null;
        }
        if((!isDirectArrival)&&(railStationList == null)) throw new RouterException(RouterException.INPUT_OBJECT_IS_NULL);
        if((transportation == null) || (date == null) || (originStation == null) || (destinationStation == null)) throw new RouterException(RouterException.INPUT_OBJECT_IS_NULL);
        if(originStation.StationID.equals(destinationStation.StationID)) throw new RouterException(RouterException.ORIGINSTATION_EQUALS_DESTINATIONSTATION);

        if(isDirectArrival) {
            List<RailDailyTimetable> railDailyTimetableList_temp;

            if(railDailyTimetableList_input == null) {
                if ((railDailyTimetableList_temp = getFromCache(transportation, date)) == null) {
                    if ((railDailyTimetableList_temp = API.getDailyTimetable(transportation, API.TRAIN_DATE, date)) == null)
                        return null;
                    seveToCache(transportation, date, railDailyTimetableList_temp);
                }
            } else {
                railDailyTimetableList_temp = new ArrayList<>(railDailyTimetableList_input);
            }

            if((railDailyTimetableList_temp = RailDailyTimetable.filterByOD(railDailyTimetableList_temp, originStation, destinationStation, originDepartureTime, destinationArrivalTime, true)) == null) return null;

            for(RailDailyTimetable railDailyTimetable:railDailyTimetableList_temp) {
                TrainPath.TrainPathPart trainPathPart = new TrainPath.TrainPathPart(originStation, destinationStation, railDailyTimetable);
                TrainPath trainPath = new TrainPath(trainPathPart);
                trainPathList.add(trainPath);
            }
        } else {
            if (transportation.equals(API.TRA_AND_THSR)) {
                List<RailStation> railStationList_THSR_ALL = API.getStation(API.THSR);//匯入高鐵所有站
                List<RailStation> THSR_have_TRA = new ArrayList<>();
                List<RailStation> TRA_have_THSR = new ArrayList<>();

                for (RailStation railStation_THSR:railStationList_THSR_ALL){
                    if((RailStation.transferStation(railStationList, railStation_THSR)) != null){
                        THSR_have_TRA.add(railStation_THSR);
                    }
                }
                for (RailStation railStation_TRA:THSR_have_TRA){
                    TRA_have_THSR.add(RailStation.transferStation(railStationList, railStation_TRA));
                }

                List<RailDailyTimetable> railDailyTimetableList_TRA_ALL;//當天台鐵的所有班次
                if(date.equals(TRARailDailyTimetableListCacheDate)) {
                    if(TRARailDailyTimetableListCache == null) {
                        TRARailDailyTimetableListCacheDate = date;
                        TRARailDailyTimetableListCache = API.getDailyTimetable(API.TRA, API.TRAIN_DATE, date);
                    }
                    railDailyTimetableList_TRA_ALL = TRARailDailyTimetableListCache;
                } else {
                    railDailyTimetableList_TRA_ALL = API.getDailyTimetable(API.TRA, API.TRAIN_DATE, date);
                    TRARailDailyTimetableListCacheDate = date;
                    TRARailDailyTimetableListCache = railDailyTimetableList_TRA_ALL;
                }

                List<RailDailyTimetable> railDailyTimetableList_THSR_ALL;//當天高鐵的所有班次
                if(date.equals(THSRRailDailyTimetableListCacheDate)) {
                    if(THSRRailDailyTimetableListCache == null) {
                        THSRRailDailyTimetableListCacheDate = date;
                        THSRRailDailyTimetableListCache = API.getDailyTimetable(API.THSR, API.TRAIN_DATE, date);
                    }
                    railDailyTimetableList_THSR_ALL = THSRRailDailyTimetableListCache;
                } else {
                    railDailyTimetableList_THSR_ALL = API.getDailyTimetable(API.THSR, API.TRAIN_DATE, date);
                    THSRRailDailyTimetableListCacheDate = date;
                    THSRRailDailyTimetableListCache = railDailyTimetableList_THSR_ALL;
                }

                RailStation originStation_THSR = (originStation.OperatorID.equals("TRA"))?RailStation.transferStation(railStationList, originStation):originStation;//把輸入車站一律轉換成高鐵，若無法轉換則為null
                RailStation destinationStation_THSR = (destinationStation.OperatorID.equals("TRA"))?RailStation.transferStation(railStationList, destinationStation):destinationStation;
                RailStation originStation_TRA = (originStation.OperatorID.equals("THSR"))?RailStation.transferStation(railStationList, originStation):originStation;//把輸入車站一律轉換為台鐵，若輸入為沒有台鐵的高鐵站則為null
                RailStation destinationStation_TRA = (destinationStation.OperatorID.equals("THSR"))?RailStation.transferStation(railStationList, destinationStation):destinationStation;

                if(( (originStation_TRA.OperatorID.equals("THSR")) && ((destinationStation_TRA.OperatorID.equals("THSR")) || (destinationStation_THSR != null ))) ||
                        ((originStation_THSR != null ) && ((destinationStation_THSR != null) || (destinationStation_TRA.OperatorID.equals("THSR"))) )) {//如果起站跟終站都是高鐵的話不轉乘
                    trainPathList = getTrainPath(API.THSR, date, originDepartureTime, null, null, railStationList_THSR_ALL, originStation_THSR, destinationStation_THSR, true);
                } else if((originStation_TRA.OperatorID.equals("THSR"))||(originStation_THSR != null)){//如果起站是高鐵終站是臺鐵的話一段轉乘
                    if(originStation_THSR != null){//如果起站是高鐵而且同時有臺鐵的話
                        List<List<RailStation>> railStationList_List = MyRailStation.getRailStationList(railStationList, originStation_TRA, destinationStation_TRA);//台鐵的起站到終站的所有班次裡的所有站

                        for (List<RailStation> railStationList_current : railStationList_List) {//把台鐵高鐵當天每個班次裡會經過2站以上的班次篩選出來
                            List<RailDailyTimetable> railDailyTimetableList_TRA = RailDailyTimetable.filterByPath(railDailyTimetableList_TRA_ALL, railStationList_current, true, 2);
                            List<RailStation> railStationList_THSR = RailStation.filterTHSR(railStationList_current, railStationList);//在臺鐵當下班次裡把高鐵有經過的站列出來
                            List<RailDailyTimetable> railDailyTimetableList_THSR = RailDailyTimetable.filterByPath(railDailyTimetableList_THSR_ALL, railStationList_THSR, true, 2);//在該路徑下含有台鐵的高鐵最遠可以走的班次表

                            for(int i = 0; i<railDailyTimetableList_THSR.size(); i++){
                                if((railDailyTimetableList_THSR.get(i).getStopTimeOfStopTimes(originStation_THSR) == null)||(railDailyTimetableList_THSR.get(i).getStopTimeOfStopTimes(originStation_THSR).getDepartureTimeDate().before(originDepartureTime))){
                                    railDailyTimetableList_THSR.remove(i);
                                    i--;
                                }
                            }

                            for(RailDailyTimetable railDailyTimetableList_THSR_temp:railDailyTimetableList_THSR){
                                StopTime THSR_LastStopTime = railDailyTimetableList_THSR_temp.findLastStopTime(railStationList_THSR);
                                RailStation lastStation_THSR = RailStation.find(railStationList_THSR, THSR_LastStopTime.StationID);
                                Date THSR_ArrivalTime = API.timeFormat.parse(THSR_LastStopTime.ArrivalTime);
                                Date TRA_DepartureTime = new Date(THSR_ArrivalTime.getTime() + TRANSFER_TIME);

                                List<TrainPath> TRA_trainPath;

                                if((TRA_trainPath = getTrainPath(API.TRA, date, TRA_DepartureTime, null, null, railStationList_current, RailStation.transferStation(railStationList, lastStation_THSR), destinationStation, false)) == null) continue;

                                TrainPath best = null;

                                for(TrainPath TRA_trainPath_temp:TRA_trainPath){
                                    if(best == null) best = TRA_trainPath_temp;
                                    else {
                                        Date tempTime = TRA_trainPath_temp.getDestinationArrivalTimeDate();
                                        Date bestTime = best.getLastItem().railDailyTimetable.getStopTimeOfStopTimes(best.getLastItem().destinationStation).getDepartureTimeDate();

                                        if (TRA_trainPath_temp.getLastItem().railDailyTimetable.afterOverNightStation(TRA_trainPath_temp.getLastItem().destinationStation.StationID)) {
                                            tempTime.setDate(tempTime.getDate() + 1);
                                        }
                                        if (best.getLastItem().railDailyTimetable.afterOverNightStation(best.getLastItem().destinationStation.StationID)) {
                                            bestTime.setDate(bestTime.getDate() + 1);
                                        }
                                        if(tempTime.before(bestTime)){
                                            best = TRA_trainPath_temp;
                                        }
                                    }
                                }

                                if(best == null) continue;

                                TrainPath trainPath = new TrainPath();
                                trainPath.trainPathPartList = new ArrayList<>();
                                TrainPath.TrainPathPart trainPathPart = new TrainPath.TrainPathPart();
                                trainPathPart.originStation = originStation_THSR;
                                trainPathPart.destinationStation = lastStation_THSR;
                                trainPathPart.railDailyTimetable = railDailyTimetableList_THSR_temp;
                                trainPath.trainPathPartList.add(trainPathPart);
                                trainPath.trainPathPartList.addAll(best.trainPathPartList);
                                trainPathList.add(trainPath);
                            }
                        }
                    } else {//如果起站是高鐵但沒有臺鐵的話

                    }
                } else if(destinationStation_TRA.OperatorID.equals("THSR")||(destinationStation_THSR != null)){//如果起站是臺鐵終站是高鐵的話一段轉乘
                    if(destinationStation_THSR != null){//如果終站是高鐵而且同時有臺鐵的話
                        List<List<RailStation>> railStationList_List = MyRailStation.getRailStationList(railStationList, originStation_TRA, destinationStation_TRA);//有方向性的所有路徑

                        for (List<RailStation> railStationList_current : railStationList_List) {//把台鐵高鐵當天班次裡會經過2站以上的班次篩選出來
                            List<RailDailyTimetable> railDailyTimetableList_TRA = RailDailyTimetable.filterByPath(railDailyTimetableList_TRA_ALL, railStationList_current, true, 2);
                            List<RailStation> railStationList_THSR = RailStation.filterTHSR(railStationList_current, railStationList);//在臺鐵當下班次裡把高鐵有經過的站列出來
                            List<RailDailyTimetable> railDailyTimetableList_THSR = RailDailyTimetable.filterByPath(railDailyTimetableList_THSR_ALL, railStationList_THSR, true, 2);
                        }
                    } else {//如果終站是高鐵但沒有臺鐵的話

                    }
                } else {//如果起站跟終站都是臺鐵的話二段轉乘
                    //遞迴賢杰功能
                    List<List<RailStation>> railStationList_List = MyRailStation.getRailStationList(railStationList, originStation_TRA, destinationStation_TRA);//台鐵的起站到終站的所有班次裡的所有站

                    for (List<RailStation> railStationList_current : railStationList_List) {//把台鐵高鐵當天班次裡會經過2站以上的班次篩選出來
                        List<RailDailyTimetable> railDailyTimetableList_TRA = RailDailyTimetable.filterByPath(railDailyTimetableList_TRA_ALL, railStationList_current, true, 2);
                        List<RailStation> railStationList_THSR = RailStation.filterTHSR(railStationList_current, railStationList);//在臺鐵當下班次裡把高鐵有經過的站列出來
                        List<RailDailyTimetable> railDailyTimetableList_THSR = RailDailyTimetable.filterByPath(railDailyTimetableList_THSR_ALL, railStationList_THSR, true, 2);
                    }
                }
            } else if (transportation.equals(API.TRA)) {
                List<List<RailStation>> railStationList_List;
                List<RailDailyTimetable> railDailyTimetableList_all;

                if((railDailyTimetableList_all = getFromCache(transportation, date)) == null) {
                    if((railDailyTimetableList_all = API.getDailyTimetable(transportation, API.TRAIN_DATE, date)) == null) return null;
                    seveToCache(transportation, date, railDailyTimetableList_all);
                }

                if((railStationList_List = MyRailStation.getRailStationList(railStationList, originStation, destinationStation)) == null) return null;

                if((railStationList_List = RailStation.removeRepeatedRailStationList(railStationList_List)) == null) return null;
                if((railStationList_List = RailStation.filter(railStationList_List, 2)) == null) return null;

                for (List<RailStation> railStationList_current : railStationList_List) {

                    List<TrainPath> trainPathList_mid_all = TrainPath.filter(railDailyTimetableList_all, railStationList_current, originDepartureTime, destinationArrivalTime, true, 2);
                    List<RailDailyTimetable> railDailyTimetableList_mid_all = TrainPath.convert(trainPathList_mid_all);

                    for(TrainPath trainPath_mid:trainPathList_mid_all) {
                        TrainPath trainPath_first = null;
                        TrainPath trainPath_last = null;
                        RailStation firstRailStation = trainPath_mid.getOriginRailStation();
                        RailStation lastRailStation = trainPath_mid.getDestinationRailStation();
                        Date firstTime = trainPath_mid.getOriginDepartureTimeDate();
                        Date lastTime = trainPath_mid.getDestinationArrivalTimeDate();

                        TrainPath trainPath_temp = new TrainPath();
                        trainPath_temp.trainPathPartList = new ArrayList<>();

                        if (!trainPath_mid.getOriginRailStation().StationID.equals(originStation.StationID)) {
                            Date firstTimeThreshold = new Date(firstTime.getTime() - TRANSFER_TIME);
                            List<TrainPath> trainPathList_first;
                            if((trainPathList_first = getTrainPath(API.TRA, date, originDepartureTime, firstTimeThreshold, railDailyTimetableList_mid_all, null, originStation, firstRailStation, true)) == null) continue;

                            if((trainPath_first = TrainPath.getBest(trainPathList_first, true, false)) == null) continue;
                        }

                        if (!lastRailStation.StationID.equals(destinationStation.StationID)) {
                            Date lastTimeThreshold = new Date(lastTime.getTime() + TRANSFER_TIME);
                            List<TrainPath> trainPathList_last;
                            if((trainPathList_last = getTrainPath(API.TRA, date, lastTimeThreshold, destinationArrivalTime, railDailyTimetableList_mid_all, null, lastRailStation, destinationStation, true)) == null) continue;

                            if((trainPath_last = TrainPath.getBest(trainPathList_last, false, true)) == null) continue;
                        }

                        if(trainPath_first != null) trainPath_temp.trainPathPartList.addAll(trainPath_first.trainPathPartList);

                        trainPath_temp.trainPathPartList.addAll(trainPath_mid.trainPathPartList);

                        if(trainPath_last != null) trainPath_temp.trainPathPartList.addAll(trainPath_last.trainPathPartList);

                        trainPathList.add(trainPath_temp);
                    }
                }
            } else if(transportation.equals(API.THSR)) {
                trainPathList =  getTrainPath(API.THSR, date, originDepartureTime, destinationArrivalTime, null, null, originStation, destinationStation, true);
            }
        }

        trainPathList = TrainPath.filter(trainPathList);

        if((trainPathList != null ? trainPathList.size() : 0) == 0) return null;

        TrainPath.sort(trainPathList);

        return trainPathList;
    }
}
