package yuntech.goalteam.goallist.Activity;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import yuntech.goalteam.goallist.Datebase.GoalSQLiteOpenHelper;
import yuntech.goalteam.goallist.List.ListItem;
import yuntech.goalteam.goallist.Notification.NotificationJobService;
import yuntech.goalteam.goallist.R;

public class GoalItemActivity extends AppCompatActivity {

    TextView tv_name,tv_notation;
    TextView tv_startDateTime,tv_endDateTime,tv_finishDateTime,tv_notifyDateTime;
    CheckBox ch_done,ch_notify;
    Toolbar toolbar;

    private String id;
    private int position;
    private long notify_time;
    private long end_time;
    private String toolbarTitle;

    DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd aa hh:mm", Locale.ENGLISH);

    private String TB = "goals_table";
    private GoalSQLiteOpenHelper helper;
    private SQLiteDatabase db;
    private JobScheduler jobScheduler;


    private static int RETURN_CODE_RESETITEM = 1;
    private static int REQUEST_CODE_RESETITEM = 1;
    private static String TIMEOUT = " time out";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_item);

        init();
        initDB();
        initDate();
        initTitle();
        initToolbar();
    }

    private void init(){
        Intent intent = getIntent();
        id = intent.getExtras().getString("id", null);
        position = intent.getExtras().getInt("position",-1);
        if((id==null)&&(position==-1)){
            Toast.makeText(this,"ID and Position Error",Toast.LENGTH_SHORT).show();
            finish();
        }

        tv_name = (TextView) findViewById(R.id.w_tv_name);
        tv_notation = (TextView)findViewById(R.id.w_tv_notation);
        tv_startDateTime = (TextView)findViewById(R.id.w_tv_startDateTime);
        tv_endDateTime = (TextView)findViewById(R.id.w_tv_endDateTime);
        tv_finishDateTime = (TextView)findViewById(R.id.w_tv_finishDateTime);
        tv_notifyDateTime = (TextView)findViewById(R.id.w_tv_notifyDateTime);
        ch_done = (CheckBox)findViewById(R.id.w_cb_done);
        ch_notify = (CheckBox)findViewById(R.id.w_cb_notify);
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

    private void initDate(){
        Cursor cursor = db.query(TB,null,"id="+id,null,null,null,null);
        cursor.moveToFirst();
        for (int i = 0 ; i < cursor.getCount() ; i++){
            if(cursor.getCount()!=1){
                Toast.makeText(this,"Count Error",Toast.LENGTH_SHORT).show();
                finish();
            }
            tv_name.setText(cursor.getString(cursor.getColumnIndex("name")));
            toolbarTitle = tv_name.getText().toString();
            tv_notation.setText(cursor.getString(cursor.getColumnIndex("notation")));
            tv_startDateTime.setText(sdf.format(new Date(cursor.getLong(cursor.getColumnIndex("start_time")))));
            tv_endDateTime.setText(sdf.format(new Date(cursor.getLong(cursor.getColumnIndex("end_time")))));

            if((cursor.getInt(cursor.getColumnIndex("done")) > 0)){
                tv_finishDateTime.setText(sdf.format(new Date(cursor.getLong(cursor.getColumnIndex("finish_time")))));
            }else{
                tv_finishDateTime.setText("Not finished yet");
            }
            if((cursor.getInt(cursor.getColumnIndex("notify")) > 0)){

                notify_time =cursor.getLong(cursor.getColumnIndex("end_time"))-cursor.getLong(cursor.getColumnIndex("notify_time"));
                tv_notifyDateTime.setText(sdf.format(new Date(notify_time)));
            }else{
                tv_notifyDateTime.setText("No notify");
            }
            ch_done.setChecked(cursor.getInt(cursor.getColumnIndex("done")) > 0);
            ch_notify.setChecked(!(cursor.getInt(cursor.getColumnIndex("notify")) > 0));
            cursor.moveToNext();
        }
        cursor.close();

        ch_done.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(ch_done.isChecked()){
                    ContentValues values = new ContentValues();
                    values.put("done",true);
                    tv_finishDateTime.setText(sdf.format(System.currentTimeMillis()));
                    values.put("finish_time",System.currentTimeMillis());
                    db.update(TB,values,"id="+id,null);
                    Toast.makeText(GoalItemActivity.this,"Done",Toast.LENGTH_SHORT).show();
                }
                else{
                    ContentValues values = new ContentValues();
                    tv_finishDateTime.setText("Not finished yet");
                    values.put("done",false);
                    db.update(TB,values,"id="+id,null);
                    Toast.makeText(GoalItemActivity.this,"Not Done",Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(ch_notify.isChecked()){ch_notify.setVisibility(View.GONE);}
        else{ch_notify.setVisibility(View.VISIBLE);}

        ch_notify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(ch_notify.isChecked()){
                    ContentValues values = new ContentValues();
                    values.put("notify",false);
                    tv_notifyDateTime.setText("No notify");
                    db.update(TB,values,"id="+id,null);
                    //Toast.makeText(GoalItemActivity.this,"Done",Toast.LENGTH_SHORT).show();
                }
                else{
                    ContentValues values = new ContentValues();
                    tv_notifyDateTime.setText(sdf.format(new Date(notify_time)));
                    values.put("notify",true);
                    db.update(TB,values,"id="+id,null);
                    //Toast.makeText(GoalItemActivity.this,"Not Done",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initTitle(){
        //TextView tv_main,tv_date;
        //tv_main = (TextView)findViewById(R.id.w_tv_main);
        //tv_main.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        //tv_date = (TextView)findViewById(R.id.w_tv_date);
        //tv_date.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
    }

    private void initToolbar(){
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Goal Detail");

        //设置回退键
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                menu_home();
                break;
            case R.id.action_delete:
                menu_delete();
                break;
            case R.id.action_edit:
                menu_edit();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.goal_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void menu_home(){
        finish();
    }

    private void menu_delete(){
        if(position==-1){
            Toast.makeText(this,"You can not delete here, please use Goal List to edit!",Toast.LENGTH_SHORT).show();
        }else {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle("Delete");
            ad.setMessage("Do you want to delete this goal?");
            ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {//退出按鈕
                public void onClick(DialogInterface dialog, int i) {
                    // TODO Auto-generated method stub
                    db.delete(TB, "id=" + id, null);
                    finish();
                    finish();//關閉activity
                }
            });
            ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int i) {
                    //不退出不用執行任何操作
                }
            });
            ad.show();//顯示對話框
        }
    }

    private void menu_edit() {
        if (position == -1) {
            Toast.makeText(this,"You can not edit here, please use Goal List to edit!",Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, GoalItemEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", id);
            bundle.putInt("position", position);
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_CODE_RESETITEM);
        }
    }

    private String calculationTimeRange(){
        String returnStr="";
        String startDateTime = tv_startDateTime.getText().toString();
        String endDateTime = tv_endDateTime.getText().toString();
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
        String result_remain=TIMEOUT;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {    //進行startActivity的返回檢查
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RETURN_CODE_RESETITEM) //進行RecycleView上Item的更新
        {
            Intent intent = data;
            String returnID = intent.getExtras().getString("id", null);
            int returnPos = intent.getExtras().getInt("position",-1);
            //System.out.println("returnID:" + returnID + " returnPos: "+returnPos);
            if((returnID==null)&&(returnPos==-1)){  //進行錯誤判斷
                Toast.makeText(GoalItemActivity.this,"Return Error",Toast.LENGTH_SHORT).show();
                return;
            }
            Cursor cursor = db.query(TB,null,"id="+returnID,null,null,null,null);
            cursor.moveToFirst();
            if((cursor.getCount()==0)&&(returnID!=null)){   //進行Item被Delete的判斷
                finish();
                return;
            }
            initDate();
        }
    }

    @Override
    public void finish() {
        //Toast.makeText(this,"GoodBy",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("id",id);
        bundle.putInt("position",position);
        intent.putExtras(bundle);
        setResult(RETURN_CODE_RESETITEM,intent);
        closeDB();
        super.finish();
    }

    private void scheduleNotifications(int JobID,Long time) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                JobInfo jobInfo = new JobInfo.Builder(JobID, new ComponentName(getPackageName(), NotificationJobService.class.getName()))
                        .setPersisted(true) //系統重啟後保留job
                        .setMinimumLatency(time)//最小延时 5秒
                        .setOverrideDeadline(time)
                        .build();
                jobScheduler.schedule(jobInfo);
            } catch (Exception ex) {
                Log.e("JobScheduler error","error");
            }
        }
    }
}
