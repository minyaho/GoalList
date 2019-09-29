package yuntech.goalteam.goallist.Activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import yuntech.goalteam.goallist.Datebase.GoalSQLiteOpenHelper;
import yuntech.goalteam.goallist.R;

public class GoalItemAddActivity extends AppCompatActivity {

    EditText edt_name,edt_notation;
    TextView tv_beginDate,tv_beginTime,tv_finishDate,tv_finishTime,tv_timeRangeContext;
    Spinner spinner_notify;
    CheckBox ch_done;
    Toolbar toolbar;
    Calendar sDateTime;
    Calendar eDateTime;
    long notify_time = 0;
    Boolean notify_flag = false;
    private String TB = "goals_table";
    private GoalSQLiteOpenHelper helper;
    private SQLiteDatabase db;


    private static String SETTING_ERROR = "setting error";
    private static String TIMEOUT = " time out";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_item_add);

        init();
        initTitle();
        initToolbar();
        initDB();
        setTimeDate(R.id.ga_tv_beginDate,R.id.ga_tv_beginTime,sDateTime);
        setTimeDate(R.id.ga_tv_finishDate,R.id.ga_tv_finishTime,eDateTime);
        initNotifySpinner();
    }

    private void init(){
        edt_name = (EditText)findViewById(R.id.ga_edT_name);
        edt_notation = (EditText)findViewById(R.id.ga_edT_notation);
        tv_beginDate = (TextView)findViewById(R.id.ga_tv_beginDate);
        tv_beginTime = (TextView)findViewById(R.id.ga_tv_beginTime);
        tv_finishDate = (TextView)findViewById(R.id.ga_tv_finishDate);
        tv_finishTime = (TextView)findViewById(R.id.ga_tv_finishTime);
        tv_timeRangeContext = (TextView)findViewById(R.id.ga_tv_timeRangeContext);
        spinner_notify = (Spinner)findViewById(R.id.ga_spinner_notify);
        ch_done = (CheckBox)findViewById(R.id.ga_cb_done);

        sDateTime = Calendar.getInstance();
        eDateTime = Calendar.getInstance();
    }

    private void initTitle(){
        //TextView tv_main,tv_date;
        //tv_main = (TextView)findViewById(R.id.ga_tv_main);
        //tv_main.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        //tv_date = (TextView)findViewById(R.id.ga_tv_date);
        //tv_date.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
    }

    private void initToolbar(){
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("New Goal");

        //设置回退键
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initDB(){
        helper = new GoalSQLiteOpenHelper(this,null,null,1);
        db = helper.getWritableDatabase();
    }

    private void closeDB(){
        if(db.isOpen()){
            db.close();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                menu_home();
                break;
            case R.id.action_saving:
                menu_save();
                break;
            case R.id.action_delete:
                menu_home();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.addwish_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void menu_home(){
        Toast.makeText(GoalItemAddActivity.this,"Cancel new",Toast.LENGTH_SHORT).show();
        finish();
    }

    private void menu_save(){
        String name = edt_name.getText().toString();
        String notation = edt_notation.getText().toString();
        String beginDateTime = tv_beginDate.getText().toString()+" "+tv_beginTime.getText().toString();
        String finishDateTime = tv_finishDate.getText().toString()+" "+tv_finishTime.getText().toString();
        boolean isFinish = ch_done.isChecked();
        Date bDateTime = new Date();
        Date fDateTime = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd aa hh:mm", Locale.ENGLISH);
        if(name.isEmpty()){
            Toast.makeText(GoalItemAddActivity.this,"name is empty",Toast.LENGTH_SHORT).show();
            return;
        }
        else if(tv_timeRangeContext.getText().toString()==SETTING_ERROR){
            Toast.makeText(GoalItemAddActivity.this,"You are setting error!",Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            try {
                bDateTime = sdf.parse(beginDateTime);
                fDateTime = sdf.parse(finishDateTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if((fDateTime.getTime()-bDateTime.getTime())<=0){
                Toast.makeText(GoalItemAddActivity.this,"Time Out Error! Please reset the time and date!",Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put("name",name);
            values.put("notation",notation);
            values.put("start_time",bDateTime.getTime());
            values.put("end_time",fDateTime.getTime());
            if(isFinish==true)
            {
                values.put("finish_time",System.currentTimeMillis());
            }
            else{
                values.put("finish_time",0);
            }
            values.put("notify_time",notify_time);
            values.put("done",isFinish);
            values.put("notify",notify_flag);
            db.insert(TB,null,values);

            Toast.makeText(this,"Save success",Toast.LENGTH_SHORT).show();
            //Toast.makeText(GoalItemAddActivity.this,name+'\n'+notation+'\n'+sdf.format(bDateTime.getTime())+'\n'+sdf.format(fDateTime.getTime())+'\n'+isFinish,Toast.LENGTH_LONG).show();
            //Toast.makeText(GoalItemAddActivity.this,name+'\n'+notation+'\n'+bDateTime.getTime()+'\n'+fDateTime.getTime()+'\n'+isFinish,Toast.LENGTH_LONG).show();
            //System.out.println("bDateTime "+bDateTime.getTime());
            //lSystem.out.println("bDateTime "+fDateTime.getTime());
            finish();
        }
    }

    private void setTimeDate(int date,int time,final Calendar calendar){
        final TextView tv_Date,tv_Time;
        tv_Date = (TextView) findViewById(date);
        //Calendar calendar= Calendar.getInstance();
        tv_Date.setText(calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.DAY_OF_MONTH));
        tv_Date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(GoalItemAddActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(year,month,dayOfMonth);
                        tv_Date.setText(year+"/"+String.valueOf(month+1)+"/"+dayOfMonth);
                        tv_timeRangeContext.setText(calculationTimeRange());
                    }
                },calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });


        tv_Time = (TextView)findViewById(time);
        String bAM_PM ;
        String bHOUR;
        String bMINUTE;
        if(calendar.get(Calendar.HOUR_OF_DAY) < 12) {
            bAM_PM = "AM";
        } else {
            bAM_PM = "PM";
        }
        if(calendar.get(Calendar.HOUR) <10)
            bHOUR = "0"+Integer.toString(calendar.get(Calendar.HOUR));
        else
            bHOUR = Integer.toString(calendar.get(Calendar.HOUR));
        if(calendar.get(Calendar.MINUTE) <10)
            bMINUTE = "0"+Integer.toString(calendar.get(Calendar.MINUTE));
        else
            bMINUTE = Integer.toString(calendar.get(Calendar.MINUTE));
        tv_Time.setText(bAM_PM + " " + bHOUR+":"+bMINUTE);
        //tv_beginTime.setText(calendar.get(Calendar.AM_PM)+ calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE));
        tv_Time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(GoalItemAddActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    String AM_PM ;
                    String HOUR;
                    String MINUTE;
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH),hourOfDay,minute);
                        if(hourOfDay < 12) {
                            AM_PM = "AM";
                        } else {
                            AM_PM = "PM";
                            hourOfDay-=12;
                        }
                        if(hourOfDay<10)
                            HOUR = "0"+Integer.toString(hourOfDay);
                        else
                            HOUR = Integer.toString(hourOfDay);
                        if(minute<10)
                            MINUTE = "0"+Integer.toString(minute);
                        else
                            MINUTE = Integer.toString(minute);
                        tv_Time.setText(AM_PM + " " + HOUR+":"+MINUTE);
                        tv_timeRangeContext.setText(calculationTimeRange());
                    }
                },calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false).show();
            }
        });
    }

    private String calculationTimeRange(){
        String returnStr="";
        String startDateTime = tv_beginDate.getText().toString()+" "+tv_beginTime.getText().toString();
        String endDateTime = tv_finishDate.getText().toString()+" "+tv_finishTime.getText().toString();
        DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd aa hh:mm", Locale.ENGLISH);
        Date sdDateTime = new Date();
        Date edDateTime = new Date();
        try {
            sdDateTime = sdf.parse(startDateTime);
            edDateTime = sdf.parse(endDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        returnStr = resultRemain(sdDateTime.getTime(),edDateTime.getTime());
        return returnStr;
    }

    private String resultRemain(long startTimeL,long endTimeL){ //產出時間字串
        String result_remain=SETTING_ERROR;
        long day = (endTimeL-startTimeL)/86400000;
        long hour = ((endTimeL-startTimeL)%86400000)/3600000;
        long minute = ((endTimeL-startTimeL)%3600000)/60000;
        long second = ((endTimeL-startTimeL)%60000)/1000;
        if(endTimeL-System.currentTimeMillis()<0){
            result_remain=TIMEOUT;
        }
        else if(day>0){
            result_remain = " "+ day + " day";
            if(hour>0)
                result_remain += " "+ hour + " hr";
            if(minute>0)
                result_remain += " "+minute + " min";
        }
        else if (hour>0){
            result_remain = " "+ hour + " hr";
            if(minute>0)
                result_remain += " "+minute + " min";
        }
        else if (minute>0){
            result_remain = " "+minute + " min";
        }
        else if (second>0){
            result_remain = " "+second + " sec";
        }
        return result_remain;
    }

    private void initNotifySpinner(){
        ArrayList<String> NotifyItems = new ArrayList<>();
        for(int i=1;i<=60;i+=10){
            NotifyItems.add(String.valueOf(i)+" min");
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.notify_time,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_notify.setAdapter(adapter);

        spinner_notify.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = getResources().getStringArray(R.array.notify_time_value)[position];
                notify_time = Long.parseLong(value);
                if(notify_time!=-1){
                    notify_flag=true;
                    Toast.makeText(GoalItemAddActivity.this,"notify "+ getResources().getStringArray(R.array.notify_time)[position],Toast.LENGTH_SHORT).show();
                }
                else{
                    notify_flag=false;
                    //Toast.makeText(GoalItemAddActivity.this,"notify close",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void finish() {
        closeDB();
        super.finish();
    }
}
