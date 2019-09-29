package yuntech.goalteam.goallist.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import yuntech.goalteam.goallist.Activity.GoalItemActivity;
import yuntech.goalteam.goallist.Datebase.GoalSQLiteOpenHelper;
import yuntech.goalteam.goallist.R;

public class NotificationJobService extends JobService {

    int id;
    private String title;
    private String text;
    private String output_title;
    private String output_text;
    private boolean notify;
    private boolean done;
    private long start_time;
    private long end_time;
    private long notify_time;

    //Notification.Builder builder;
    NotificationCompat.Builder builder;
    Notification notification;
    NotificationManager notificationManager;
    NotificationChannel channel;

    private String TB = "goals_table";
    private GoalSQLiteOpenHelper helper;
    private SQLiteDatabase db;

    private static int REQUEST_CODE_RESETITEM = 1;
    private static String TIMEOUT = " time out";
    private static String STARTED_YET = " haven't started yet";

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle bundle = params.getExtras();
        id = params.getJobId();
        initDB();
        createNotificationChannel();
        makeNotification();
        closeDB();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
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

    private void makeNotification(){
        //Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        //vibrator.vibrate(200);
        Cursor cursor = db.query(TB,null,"id="+id,null,null,null,null);
        cursor.moveToFirst();
        for (int i = 0 ; i < cursor.getCount() ; i++){
            title       = cursor.getString(cursor.getColumnIndex("name"));
            text        = cursor.getString(cursor.getColumnIndex("notation"));
            done        =(cursor.getInt(cursor.getColumnIndex("done")) > 0);
            notify      =(cursor.getInt(cursor.getColumnIndex("notify")) > 0);
            start_time  = cursor.getLong(cursor.getColumnIndex("start_time"));
            end_time    = cursor.getLong(cursor.getColumnIndex("end_time"));
            notify_time = cursor.getLong(cursor.getColumnIndex("notify_time"));
            cursor.moveToNext();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(this,"nc_1");
        }else{
            builder = new NotificationCompat.Builder(this);
        }

        if((notify==true)&&(done==false)&&(cursor.getCount()!=0)){
            if(notify_time!=0){
                output_title = "Notification ahead of " + title;
                output_text = "It will be ended after"+ resultRemain(end_time-notify_time,end_time).toString()+", touch me for more information";
            }
            if(notify_time==0){
                output_title = "Notice of the deadline for " + title;
                output_text = "This goal ended, touch me for more information";
            }
            Intent intent = new Intent(this, GoalItemActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id",String.valueOf(id));
            intent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,REQUEST_CODE_RESETITEM,intent,PendingIntent.FLAG_CANCEL_CURRENT);

            builder.setContentTitle(output_title)
                    .setContentText(output_text);
            builder.setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_list_black)
                    .setColor(Color.GREEN)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId("0");
                notificationManager.notify(id,builder.build());
            }else{
                builder.setPriority(Notification.PRIORITY_MAX);
                notification = builder.build();
                NotificationManager manager = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                manager.notify(id, notification);
            }


        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            channel = new NotificationChannel("0", "Notify Goal", importance);
            channel.setDescription("Notify your goal");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private StringBuilder resultRemain(long startTimeL,long endTimeL){ //產出時間字串
        StringBuilder builder = new StringBuilder();
        builder.append(TIMEOUT);
        long day = (endTimeL-startTimeL)/86400000;
        long hour = ((endTimeL-startTimeL)%86400000)/3600000;
        long minute = ((endTimeL-startTimeL)%3600000)/60000;
        long second = ((endTimeL-startTimeL)%60000)/1000;

        if ((System.currentTimeMillis()-startTimeL)<0){
            builder.delete(0,builder.length()-1);
            builder.append(STARTED_YET);
        }
        else if(day>0){
            builder.delete(0,builder.length());
            builder.append(" "+ day + " day");
            if(hour>0) {
                builder.append(" " + hour + " hr");
            }
            if(minute>0){
                builder.append(" " + hour + " hr");
            }
        }
        else if (hour>0){
            builder.delete(0,builder.length());
            builder.append(" "+ hour + " hr");
            if(minute>0)
                builder.append(" "+minute + " min");
        }
        else if (minute>0){
            builder.delete(0,builder.length());
            builder.append(" "+minute + " min");
        }
        else if (second>0){
            builder.delete(0,builder.length());
            builder.append(" "+second + " sec");
        }
        return builder;
    }
}