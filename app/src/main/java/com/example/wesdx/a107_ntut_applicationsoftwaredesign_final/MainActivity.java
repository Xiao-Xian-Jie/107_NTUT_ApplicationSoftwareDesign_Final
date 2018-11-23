package com.example.wesdx.a107_ntut_applicationsoftwaredesign_final;

//https://github.com/ptxmotc/Sample-code
//https://ptx.transportdata.tw/PTX/Topic/fbeac0a2-fc53-4ffa-8961-597b2d3e6bdd

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView, textView2, textView3, textView4, textView5;
    private RadioGroup radioGroup;
    private CheckBox checkBox;
    private CheckBox checkBox2;
    private Button search;

    private List<RailStation> railStationList;
    private List<RailGeneralTimetable> railGeneralTimetableList;
    private List<RailGeneralTrainInfo> railGeneralTrainInfoList;
    private List<RailODDailyTimetable> railODDailyTimetableList;
    private List<RegionalRailStation> regionalRailStationList;

    private RailStation originStation;
    private RailStation destinationStation;
    private int TRAOrTHSR = 0;
    private int price = 0;
    private int arriveTimeFirst = 0;
    private  String timeNumber;
    private  String dateNumber;
    private Calendar calendar = Calendar.getInstance();;
    private int year = calendar.get(Calendar.YEAR);
    private int month = calendar.get(Calendar.MONTH);
    private int day = calendar.get(Calendar.DAY_OF_MONTH);
    private int hour = calendar.get(Calendar.HOUR_OF_DAY);
    private int minute = calendar.get(Calendar.MINUTE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
        search = (Button)findViewById(R.id.search);

        railStationList = TRAAPI.getRailStation();
        RailStation.removeUnreservationStation(railStationList);
        railGeneralTimetableList = TRAAPI.getRailGeneralTimetable();
        railGeneralTrainInfoList = TRAAPI.getRailGeneralTrainInfo();
        railODDailyTimetableList = TRAAPI.getRailODDailyTimetable("1002", "1004", "2018-11-25");
        regionalRailStationList = RegionalRailStation.convert(railStationList);
        //List<RailODFare> railODFares = TRAAPI.getRailODFare(originStation.StationID, destinationStation.StationID);

        railODDailyTimetableList = RailODDailyTimetable.filter(railODDailyTimetableList, "07:00", "01:00");

        textView.setText(String.valueOf(year) + "-" + String.valueOf(month+1) + "-" + String.valueOf(day));
        textView2.setText(hour + ":" + minute);
        textView3.setText(railStationList.get(2).StationName.Zh_tw);
        textView4.setText(railStationList.get(3).StationName.Zh_tw);
        textView5.setText(railStationList.get(4).StationName.Zh_tw);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButton:
                        TRAOrTHSR = 1;
                        break;
                    case R.id.radioButton2:
                        TRAOrTHSR = 2;
                        break;
                    case R.id.radioButton3:
                        TRAOrTHSR = 3;
                        break;
                    default:
                        TRAOrTHSR = 0;
                        break;
                }
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                price = (isChecked)? 1: 0;
            }
        });

        checkBox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                arriveTimeFirst = (isChecked)? 1: 0;
            }
        });

        String[] stationName = new String[railStationList.size()];
        for (int i = 0; i < stationName.length; i++) {
            stationName[i] = railStationList.get(i).ReservationCode + railStationList.get(i).StationName.Zh_tw;
        }

        final Spinner start_station = (Spinner)findViewById(R.id.start_station);
        final Spinner arrive_station = (Spinner)findViewById(R.id.arrive_station);

        myAdapter transAdapter = new myAdapter(stationName,R.layout.rail_station_spinner_item);
        start_station.setAdapter(transAdapter);
        arrive_station.setAdapter(transAdapter);

        start_station.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                originStation = railStationList.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                originStation = null;
            }
        });

        arrive_station.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                destinationStation = railStationList.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destinationStation = null;
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShowResult.class);
                Bundle bundle = new Bundle();
                bundle.putString("mystring", "mystring123");
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        dateNumber = String.valueOf(year) + "-" + String.valueOf(month+1) + "-" + String.valueOf(day);
                        textView.setText(dateNumber);
                    }

                }, year, month, day).show();
            }
        });

        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        timeNumber = hour + ":" + minute;
                        textView2.setText(timeNumber);
                    }

                }, hour, minute, true).show();
            }
        });
    }

    public  class myAdapter extends BaseAdapter{
        private String[] data;
        private int view;

        public myAdapter(String[] data, int view){
            this.data = data;
            this.view = view;
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public String getItem(int position) {
            return data[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(view, parent, false);
            TextView name = (TextView) convertView.findViewById(R.id.name);
            name.setText(data[position]);
            return convertView;
        }
    }

}

