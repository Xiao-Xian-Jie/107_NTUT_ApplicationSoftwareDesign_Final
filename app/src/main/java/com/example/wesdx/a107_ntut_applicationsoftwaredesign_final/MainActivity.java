package com.example.wesdx.a107_ntut_applicationsoftwaredesign_final;

//https://github.com/ptxmotc/Sample-code
//https://ptx.transportdata.tw/PTX/Topic/fbeac0a2-fc53-4ffa-8961-597b2d3e6bdd

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.API;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RailStation;
import com.example.wesdx.a107_ntut_applicationsoftwaredesign_final.PTXAPI.RegionalRailStation;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyPrefsFile";

    private TextView dateTextView, timeTextView;
    private TextView originStationTextView_buffer, destinationStationTextView_buffer;
    private RailStation originStation;
    private RailStation destinationStation;
    private List<RailStation> railStationList;
    private List<RegionalRailStation> regionalRailStationList;
    private CheckBox isDirectArrivalCheckBox, useTHSR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        final TextView originStationTextView = findViewById(R.id.stationNameTextView);
        final TextView destinationStationTextView = findViewById(R.id.destinationStationTextView);
        originStationTextView_buffer = originStationTextView;
        destinationStationTextView_buffer = destinationStationTextView;
        Button searchButton = findViewById(R.id.searchButton);
        Button changeStationButton = findViewById(R.id.changeStationButton);
        isDirectArrivalCheckBox = findViewById(R.id.isDirectArrivalCheckBox);
        useTHSR = findViewById(R.id.useTHSR);

        dateTextView.setText(API.dateFormat.format(Calendar.getInstance().getTime()));
        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        Calendar calendar1 = Calendar.getInstance();
                        calendar1.set(year, month, day, 0, 0);
                        dateTextView.setText(API.dateFormat.format(calendar1.getTime()));
                    }
                }, year, month, day).show();
            }
        });

        timeTextView.setText(API.timeFormat.format(Calendar.getInstance().getTime()));
        timeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        Calendar calendar1 = Calendar.getInstance();
                        calendar1.set(0, 0, 0, hour, minute);
                        timeTextView.setText(API.timeFormat.format(calendar1.getTime()));
                    }

                }, hour, minute, true).show();
            }
        });

        originStationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectStation(originStationTextView);
            }
        });

        destinationStationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectStation(destinationStationTextView);
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTask();
            }
        });

        changeStationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RailStation railStation = originStation;
                originStation = destinationStation;
                destinationStation = railStation;
                originStationTextView_buffer.setText(originStation.StationName.Zh_tw);
                destinationStationTextView_buffer.setText(destinationStation.StationName.Zh_tw);
            }
        });

        isDirectArrivalCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useTHSR.setEnabled(!isDirectArrivalCheckBox.isChecked());
                if(isDirectArrivalCheckBox.isChecked()) {
                    useTHSR.setChecked(false);
                }
            }
        });

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.custom_title_bar);

        setInitialData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("originStationID", originStation.StationID);
        editor.putString("destinationStationID", destinationStation.StationID);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                final String[] settingList = {"台鐵-台鐵轉乘時間", "台鐵-高鐵轉乘時間"};

                alertDialog.setTitle("選擇設定");
                alertDialog.setItems(settingList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {

                        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                        final EditText input = new EditText(MainActivity.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        input.setLayoutParams(lp);
                        input.setInputType(InputType.TYPE_CLASS_NUMBER);

                        alertDialog.setTitle("輸入時間(分鐘)");
                        alertDialog.setView(input);
                        alertDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which2) {
                                String number = input.getText().toString();
                                if(number.equals("")) {
                                    Toast.makeText(MainActivity.this, "請輸入數字", Toast.LENGTH_SHORT).show();
                                } else {
                                    int time = Integer.parseInt(input.getText().toString());
                                    if(time < 0) {
                                        Toast.makeText(MainActivity.this, "請輸入大於等於0的數字", Toast.LENGTH_SHORT).show();
                                    } else {
                                        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                                        SharedPreferences.Editor editor = settings.edit();
                                        editor.putInt((which == 0 ? "TRAToTRATransferTime" : "TRAToTHSRTransferTime"), time);
                                        editor.apply();
                                        if(which == 0) {
                                            Router.TRAToTRATransferTime = time * 60 * 1000;
                                        } else {
                                            Router.TRAToTHSRTransferTime = time * 60 * 1000;
                                        }
                                        Toast.makeText(MainActivity.this, "已將" + (which == 0 ? "台鐵-台鐵轉乘時間" : "台鐵-高鐵轉乘時間") + "設為：" + Integer.toString(time), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        alertDialog.show();
                    }
                });
                alertDialog.show();
                return(true);
            case R.id.action_exit:
                finish();
                return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

    @SuppressLint("StaticFieldLeak")
    private void setInitialData() {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    List<RailStation> railStationList_TRA = API.getStation(API.TRA);
                    List<RailStation> railStationList_THSR = API.getStation(API.THSR);
                    RailStation.removeUnreservationStation(railStationList_TRA);

                    Router.initCache(railStationList_TRA, railStationList_THSR);

                    railStationList = new ArrayList<>();
                    railStationList.addAll(railStationList_TRA);
                    railStationList.addAll(railStationList_THSR);
                    regionalRailStationList = RegionalRailStation.convert(railStationList);

                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    String originStationID = settings.getString("originStationID", "");
                    String destinationStationID = settings.getString("destinationStationID", "");
                    int TRAToTRATransferTime = settings.getInt("TRAToTRATransferTime", 5);
                    int TRAToTHSRTransferTime = settings.getInt("TRAToTHSRTransferTime", 10);
                    Router.TRAToTRATransferTime = TRAToTRATransferTime * 60 * 1000;
                    Router.TRAToTHSRTransferTime = TRAToTHSRTransferTime * 60 * 1000;
                    for (RailStation railStation:railStationList) {
                        if (railStation.StationID.equals(originStationID)) {
                            originStationTextView_buffer.setText(railStation.StationName.Zh_tw);
                            originStation = railStation;
                        }
                        if (railStation.StationID.equals(destinationStationID)) {
                            destinationStationTextView_buffer.setText(railStation.StationName.Zh_tw);
                            destinationStation = railStation;
                        }
                    }
                } catch (SignatureException | IOException | ParseException | Router.RouterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog.setMessage("更新資料");
                dialog.setCancelable(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.dismiss();
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void searchTask() {
        if((originStation == null) || (destinationStation == null)) {
            Toast.makeText(MainActivity.this, "請選擇車站", Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            private List<TrainPath> trainPathList;
            private String errorMessage;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String transportation;
                    if(useTHSR.isChecked()) {
                        transportation = API.TRA_AND_THSR;
                    } else {
                        transportation = (originStation.OperatorID.equals(destinationStation.OperatorID) ? originStation.OperatorID : API.TRA_AND_THSR);
                    }
                    /*
                    FileWriter fw = new FileWriter(getFilesDir() +"/text.txt");
                    PrintWriter pw = new PrintWriter(fw);
                    try {
                        trainPathList = Router.getTrainPath(transportation, dateTextView.getText().toString(), API.timeFormat.parse(timeTextView.getText().toString()), null, null, railStationList, originStation, destinationStation, isDirectArrivalCheckBox.isChecked());
                        for(int i = 0; i < railStationList.size(); i++) {  //46
                            for(int j = 0; j < railStationList.size(); j++) {
                                if(i == j) continue;
                                trainPathList = Router.getTrainPath(transportation, dateTextView.getText().toString(), null, null, null, railStationList, railStationList.get(i), railStationList.get(j), false);


                                pw.print(Integer.toString(i) + " " + railStationList.get(i).StationName.Zh_tw + "→" + Integer.toString(j) + " " + railStationList.get(j).StationName.Zh_tw + " : " + Integer.toString(trainPathList != null ? trainPathList.size() : 0) + '\n');

                                if((trainPathList != null ? trainPathList.size() : 0) == 0) {
                                    Log.d("DEBUG1", Integer.toString(i) + " " + railStationList.get(i).StationName.Zh_tw + "→" + Integer.toString(j) + " " + railStationList.get(j).StationName.Zh_tw + " : " + Integer.toString(trainPathList != null ? trainPathList.size() : 0));
                                } else {
                                    Log.d("DEBUG2", Integer.toString(i) + " " + railStationList.get(i).StationName.Zh_tw + "→" + Integer.toString(j) + " " + railStationList.get(j).StationName.Zh_tw + " : " + Integer.toString(trainPathList != null ? trainPathList.size() : 0));

                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
*/

                    trainPathList = Router.getTrainPath(transportation, dateTextView.getText().toString(), API.timeFormat.parse(timeTextView.getText().toString()), null, null, railStationList, originStation, destinationStation, isDirectArrivalCheckBox.isChecked());

                } catch (ParseException | Router.RouterException | SignatureException | IOException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog.setMessage("取得班次");
                dialog.setCancelable(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.dismiss();
                if(errorMessage != null) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    if ((trainPathList != null ? trainPathList.size() : 0) == 0) {
                        Toast.makeText(MainActivity.this, "查無班次", Toast.LENGTH_SHORT).show();
                    } else {
                        int limitSize = 10;
                        if(trainPathList.size() > limitSize) trainPathList = trainPathList.subList(0, limitSize);

                        Intent intent = new Intent(MainActivity.this, ShowResult.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("trainPathListGson", (new Gson()).toJson(trainPathList));
                        bundle.putString("originStationGson", (new Gson()).toJson(originStation));
                        bundle.putString("destinationStationGson", (new Gson()).toJson(destinationStation));
                        bundle.putInt("limitSize", limitSize);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                }
            }
        }.execute();
    }

    private void selectStation(final TextView textView) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

        String[] regionList = new String[regionalRailStationList.size()];

        for(int i = 0; i < regionalRailStationList.size(); i++) {
            regionList[i] = regionalRailStationList.get(i).regionName;
        }

        alertDialog.setTitle("選擇區域");
        alertDialog.setItems(regionList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                List<String> buffer = new ArrayList<>();

                for(int i = 0; i < regionalRailStationList.get(which).railStationList.size(); i++) {
                    if(regionalRailStationList.get(which).railStationList.get(i).StationName.Zh_tw.equals("古莊")) continue;
                    buffer.add(regionalRailStationList.get(which).railStationList.get(i).StationName.Zh_tw);
                }

                final String[] stationList = new String[buffer.size()];
                for(int i = 0; i < buffer.size(); i++) {
                    stationList[i] = buffer.get(i);
                }

                alertDialog.setTitle("選擇區域");
                alertDialog.setItems(stationList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which2) {
                        textView.setText(stationList[which2]);
                        if(textView == originStationTextView_buffer) {
                            originStation = regionalRailStationList.get(which).railStationList.get(which2);
                        } else if(textView == destinationStationTextView_buffer){
                            destinationStation = regionalRailStationList.get(which).railStationList.get(which2);
                        }
                        if((originStation != null)&&(destinationStation != null)){
                            if (originStation.OperatorID.equals(API.TRA) && destinationStation.OperatorID.equals(API.TRA)) {
                                isDirectArrivalCheckBox.setEnabled(true);
                                isDirectArrivalCheckBox.setChecked(false);
                                useTHSR.setVisibility(View.VISIBLE);

                            } else if (originStation.OperatorID.equals(API.THSR) && destinationStation.OperatorID.equals(API.THSR)) {
                                isDirectArrivalCheckBox.setEnabled(false);
                                isDirectArrivalCheckBox.setChecked(true);
                                useTHSR.setVisibility(View.INVISIBLE);
                                useTHSR.setChecked(false);
                            } else {
                                isDirectArrivalCheckBox.setEnabled(false);
                                isDirectArrivalCheckBox.setChecked(false);
                                useTHSR.setVisibility(View.INVISIBLE);
                                useTHSR.setChecked(false);
                            }
                        }
                    }
                });
                alertDialog.show();
            }
        });
        alertDialog.show();
    }
}

