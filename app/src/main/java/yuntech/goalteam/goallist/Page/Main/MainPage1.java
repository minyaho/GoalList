package yuntech.goalteam.goallist.Page.Main;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yuntech.goalteam.goallist.Activity.GoalItemActivity;
import yuntech.goalteam.goallist.Activity.GoalItemEditActivity;
import yuntech.goalteam.goallist.Adapter.ItemClickListener;
import yuntech.goalteam.goallist.Adapter.MyRecyclerViewAdapter;
import yuntech.goalteam.goallist.Datebase.GoalSQLiteOpenHelper;
import yuntech.goalteam.goallist.List.ListItem;
import yuntech.goalteam.goallist.Notification.NotificationJobService;
import yuntech.goalteam.goallist.R;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class MainPage1 extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /*xml上的元件*/
    TextView tv_page_title,tv_empty,tv_remain;
    ImageView iv_empty;
    ConstraintLayout layout_empty;
    private View view = null;
    private RecyclerView recyclerView = null;
    private RecyclerView.Adapter adapter = null;
    private List<ListItem> listItems = null;
    private SwipeRefreshLayout swipeRefreshLayout;

    /*系統服務註冊*/
    private Vibrator vibrator;              //註冊震動感應器
    private JobScheduler jobScheduler;      //用來註冊和消除通知用，android必須要在5板以上

    private String TB = "goals_table";      //使用的資料庫
    private GoalSQLiteOpenHelper helper;    //協助SQL的更新、刪除、創立、查詢
    private SQLiteDatabase db;

    private static int REQUEST_CODE_RESETITEM = 1;              //請求代碼->請求刷新Item用
    private static int RETURN_CODE_RESETITEM = 1;               //返回代碼->得知刷新Item用
    private static String TIMEOUT = " time out";                //字串: 時間超時
    private static String STARTED_YET = " haven't started yet"; //字串: 尚未開始

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_page_1,container,false);

        init();         //設定初始化xml上的元件
        initDB();       //初始化資料庫
        initDate();     //初始化資料，本程式的重點部分
        initList();     //初始化RecyclerView (重點只能被初始化一次!)
        initTimer();    //初始化更新頁面的計時器
        return view;
    }

    private void init(){    //設定初始化xml上的元件
        tv_page_title = (TextView)view.findViewById(R.id.tv_page_title);
        tv_empty = (TextView)view.findViewById(R.id.tv_empty);
        iv_empty = (ImageView)view.findViewById(R.id.iv_empty);
        layout_empty = (ConstraintLayout)view.findViewById(R.id.layout_empty);
        tv_remain = (TextView)view.findViewById(R.id.tv_remain);;

        tv_page_title.setText("Next");
        tv_empty.setText("There is empty\nQuick new a Goal");
        iv_empty.setImageResource(R.drawable.ic_file_new_120);

        swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        listItems = new ArrayList<ListItem>();

        vibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
        jobScheduler = (JobScheduler)getActivity().getSystemService(JOB_SCHEDULER_SERVICE);
    }

    private void initDB(){      //初始化資料庫
        helper = new GoalSQLiteOpenHelper(getActivity(),null,null,1);
        db = helper.getWritableDatabase();
    }

    private void closeDB(){
        if(db.isOpen()){
            db.close();
        }
    }

    private void initDate(){    //初始化資料，本程式的重點部分

        /*設定要搜索資料庫裡的資料表的資料，我們在這裡定義這筆資料叫做紀錄*/
        Cursor cursor = db.query(TB,null,null,null,null,null,"id ASC");
        cursor.moveToFirst();

        for (int i = 0 ; i < cursor.getCount() ; i++){  //

            /*從記錄中讀取出開始時間和結束時間*/
            long startTimeL = (cursor.getLong(cursor.getColumnIndex("start_time")));
            long endTimeL = (cursor.getLong(cursor.getColumnIndex("end_time")));

            /*設置剩餘時間字串*/
            long timeout = endTimeL-System.currentTimeMillis();

            /*開始進行item的建立、檢查、新增、刪除*/
            /*假如以下條例成立，那麼就刪除該item
                1.剩餘時間字串是顯示TIMEOUT
                2.cursor所找到的紀錄數量不為0
                3.RecyclerView的adapter不為null (注意: 已經過initList()過後才會有adapter)
            * 從listItems移除，並且告知adapter進行更新
            * */
            if((timeout<=0)&&(cursor.getCount()!=0)&&(adapter!=null)){
                int TIME_OUT_ID = cursor.getInt(cursor.getColumnIndex("id"));
                for(int j=0;j<listItems.size();j++){
                    if(listItems.get(j).getId()==TIME_OUT_ID){
                        listItems.remove(j);
                        adapter.notifyItemRemoved(j);
                    }
                }
            }

            /*假如以下條例成立
                1.剩餘時間字串不是顯示TIMEOUT
                2.該筆紀錄的done(是否完成的flag)是否為false
                3.該listItems裡並沒有任何一個物件旗下的id與此紀錄的id相同(請見 註1)
            * 那麼就新增這筆item到listItems裡，並且透過scheduleNotifications的方法來註冊該紀錄未來的通知(請見 註2)
            * 並且adapter不為null，則就通知adapter要更新這筆被新增的item到RecyclerView上
            * */
            if((timeout>0)&&((cursor.getInt(cursor.getColumnIndex("done")) > 0) == false)){
                /*註1:
                * add_flag:用來表示應該新增這筆listItem到listItems
                *
                * 假如下面條件成立
                *   1.listItems的任一個物件旗下的id與此紀錄的id相同
                * 那麼add_flag會被設為false
                * */
                boolean add_flag = true;
                for (int j = 0; j < listItems.size(); j++)
                {
                    if(listItems.get(j).getId()==cursor.getInt(cursor.getColumnIndex("id"))){
                        add_flag = false;
                        break;
                    }
                }
                if (add_flag==true) {

                    /*註2:
                    * setNotificationsTime: 用來計算要註冊通知的時間
                    *                       方法結束時間-通知設定的時間-目前系統的時間
                    *
                    * 假如以下條件成立
                    *   1.該紀錄的notify(通知旗幟)為true
                    *   2.setNotificationsTime的值大於等於0
                    * 那麼就透過scheduleNotifications(紀錄的id,用來計算要註冊通知的時間)來註冊該通知
                    * */
                    long setNotificationsTime = cursor.getLong(cursor.getColumnIndex("end_time"))-System.currentTimeMillis()-cursor.getLong(cursor.getColumnIndex("notify_time"));
                    if(((cursor.getInt(cursor.getColumnIndex("notify")) > 0)==true)&&(setNotificationsTime>=0))//設置通知
                    {
                        scheduleNotifications(cursor.getInt(cursor.getColumnIndex("id")),setNotificationsTime);
                    }
                    /*新增這筆item到listItems裡*/
                    ListItem listItem = new ListItem(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getString(cursor.getColumnIndex("notation")),
                            resultRemain(System.currentTimeMillis(),endTimeL).toString(),
                            (cursor.getInt(cursor.getColumnIndex("done")) > 0),
                            (cursor.getInt(cursor.getColumnIndex("notify")) > 0),
                            (cursor.getLong(cursor.getColumnIndex("start_time"))),
                            (cursor.getLong(cursor.getColumnIndex("end_time"))),
                            (cursor.getLong(cursor.getColumnIndex("notify_time")))
                    );
                    listItems.add(0, listItem);

                    /*通知adapter進行更新*/
                    if(adapter!=null){
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                    }
                }
            }

            /*搜索下一筆*/
            cursor.moveToNext();
        }

        /*判斷listItems是不是為空
        *
        * 以下的定義與上者相同
        *   1.上方的程序並沒有辦法讓listItems內有object
        *   2.此頁面可能沒有任何的Goal可以顯示
        *
        * 因此透過isListEmpty()來協助驗證
        * */
        isListEmpty();
        cursor.close();
    }

    private void initList(){    //初始化RecyclerView

        /*初始化xml上的RecyclerView*/
        recyclerView = (RecyclerView)view.findViewById(R.id.rec_view);
        recyclerView.setHasFixedSize(true);     //固定每頁大小
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));  //設定線性排版

        /*RecyclerViewAdapter adapter的初始化透過我們自定義的MyRecyclerViewAdapter*/
        adapter = new MyRecyclerViewAdapter(listItems,getActivity());
        recyclerView.setAdapter(adapter);

        /*初始化RecyclerView上的每一個item的click事件，透過自行定義的ItemClickListener*/
        recyclerView.addOnItemTouchListener(new ItemClickListener(recyclerView, new ItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(final View view, int position) {
                final int pos = position;
                final String id = ((TextView) recyclerView.findViewHolderForAdapterPosition(pos).itemView.findViewById(R.id.tv_item_id)).getText().toString();
                //Toast.makeText(getActivity(), "touch click short:" + position, Toast.LENGTH_SHORT).show();
                CheckBox done = ((CheckBox) recyclerView.findViewHolderForAdapterPosition(pos).itemView.findViewById(R.id.ch_done));
                LinearLayout mainLayout = ((LinearLayout) recyclerView.findViewHolderForAdapterPosition(pos).itemView.findViewById(R.id.layout_main_list));

                mainLayout.setOnClickListener(new View.OnClickListener() {  //設立item進入Goal Detail的動作
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), GoalItemActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("id",id);
                        bundle.putInt("position",pos);
                        intent.putExtras(bundle);
                        startActivityForResult(intent,REQUEST_CODE_RESETITEM);
                    }
                });
                done.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {  //設立item勾選完成的動作
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Toast.makeText(getActivity(),"Done",Toast.LENGTH_SHORT).show();
                        if(isChecked) {
                            //Toast.makeText(getContext(),pos+"is Done!",Toast.LENGTH_SHORT).show();
                            ContentValues values = new ContentValues();
                            values.put("done",true);
                            values.put("notify",false);
                            values.put("finish_time",System.currentTimeMillis());
                            db.update(TB,values,"id="+id,null);
                            jobScheduler.cancel(Integer.parseInt(id));
                            adapter.notifyItemRemoved(pos);
                            listItems.remove(pos);
                            isListEmpty();
                        }
                    }
                });
            }

            @Override
            public void onItemLongClick(View view,final int position) { //設定長時間按壓的事件動作
                //Toast.makeText(getActivity(), "touch click long:" + position, Toast.LENGTH_SHORT).show();
                vibrator.vibrate(50);
                PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.list_popup, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.action_delete:
                                action_delete(position);
                                break;
                            case R.id.action_edit:
                                action_edit(position);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
                vibrator.vibrate(50);
            }
        }));
    }

    private void isListEmpty(){     //判斷listItems是不是為空
        /*
        * 如果為空
        *        顯示此頁面為空的消息
        *
        * 如果非空
        *        隱藏此頁面為空的消息
        *
        * layout_empty:是用來存放為空的消息的layout
        * */
        if(listItems.size()==0){
            layout_empty.setVisibility(View.VISIBLE);   //顯示
        }else{
            layout_empty.setVisibility(View.GONE);      //隱藏
        }
    }

    private void action_delete(int position){
        Toast.makeText(getActivity(),"Delete "+((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.tv_title)).getText().toString(),Toast.LENGTH_LONG).show();
        String id = ((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.tv_item_id)).getText().toString();
        jobScheduler.cancel(listItems.get(position).getId());
        listItems.remove(position);
        adapter.notifyItemRemoved(position);
        db.delete(TB,"id="+id,null);
        jobScheduler.cancel(Integer.parseInt(id));
        isListEmpty();
    }

    private void action_edit(int position){
        String id = ((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.tv_item_id)).getText().toString();
        Intent intent = new Intent(getActivity(), GoalItemEditActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("id",id);
        bundle.putInt("position",position);
        intent.putExtras(bundle);
        startActivityForResult(intent,REQUEST_CODE_RESETITEM);
    }

    public void onRefresh() {   //swipeRefreshLayout的刷新動作
        listItems.clear();
        initDate();
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
        System.out.println("Page 1: onRefresh");
    }

    @Override
    public void onResume() {    //fragment的復甦動作
        super.onResume();
        initDB();
        initDate();
        adapter.notifyDataSetChanged();
        System.out.println("Page 1: onResume");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {    //進行startActivity的返回檢查
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RETURN_CODE_RESETITEM) //進行RecycleView上Item的更新
        {
            Intent intent = data;
            String returnID = intent.getExtras().getString("id", null);
            int returnPos = intent.getExtras().getInt("position",0);
            Log.d("tag", String.valueOf(returnPos));
            //System.out.println("returnID:" + returnID + " returnPos: "+returnPos);
            if((returnID==null)||(returnPos==-1)){  //進行錯誤判斷
                Toast.makeText(getActivity(),"Return Error",Toast.LENGTH_SHORT).show();
                return;
            }
            Cursor cursor = db.query(TB,null,"id="+returnID,null,null,null,null);
            cursor.moveToFirst();
            if((cursor.getCount()==0)&&(returnID!=null)){   //進行Item被Delete的判斷
                listItems.remove(returnPos);
                adapter.notifyItemRemoved(returnPos);
                jobScheduler.cancel(Integer.parseInt(returnID));
                return;
            }
            for (int i = 0 ; i < cursor.getCount() ; i++){
                System.out.println("cursor.getCount(): "+cursor.getCount());
                long startTimeL = (cursor.getLong(cursor.getColumnIndex("start_time")));
                long endTimeL = (cursor.getLong(cursor.getColumnIndex("end_time")));
                ListItem newListItem = new ListItem(             //製造新的ListItem
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("notation")),
                        resultRemain(startTimeL,endTimeL).toString(),
                        (cursor.getInt(cursor.getColumnIndex("done")) > 0),
                        (cursor.getInt(cursor.getColumnIndex("notify")) > 0),
                        (cursor.getLong(cursor.getColumnIndex("start_time"))),
                        (cursor.getLong(cursor.getColumnIndex("end_time"))),
                        (cursor.getLong(cursor.getColumnIndex("notify_time"))));

                ListItem oldListItem = listItems.get(returnPos);    //取出就的ListItem

                //新舊ListItem做比較
                //假使返回的資料有更新，必須在RecycleView上做Item的更新
                if((oldListItem.getTitle()!=newListItem.getTitle())||
                        (oldListItem.getContext()!=newListItem.getContext())||
                        ((oldListItem.getRemain()!=newListItem.getRemain())&&(newListItem.getRemain()!=TIMEOUT))){
                    listItems.add(returnPos,newListItem);
                    listItems.remove(returnPos+1);
                    adapter.notifyItemChanged(returnPos);
                }
                //假使返回的資料已完成或是超時，必須在RecycleView上做Item的刪除
                if((newListItem.isDone()==true)||(newListItem.getRemain()==TIMEOUT)){
                    listItems.remove(returnPos);
                    adapter.notifyItemRemoved(returnPos);
                }
                if(newListItem.isNotify()==false){
                    jobScheduler.cancel(newListItem.getId());
                }
                cursor.moveToNext();
            }
            cursor.close();
            //Toast.makeText(getActivity(),"requestCode = "+requestCode+" resultCode = "+resultCode+" id= "+returnID+" pos = "+returnPos,Toast.LENGTH_SHORT).show();
        }
    }

    private StringBuilder resultRemain(long startTimeL,long endTimeL){ //產出時間字串只要給開始與結束時間既可以返回剩餘時間字串

        /*
        *   只要(結束時間-開始時間) < 0 ，會返回TIMEOUT字串
        *   其餘(結束時間-目前時間) > 0 ，將會返回:
        *       如果有天數返回字串內會有天數
        *       如果有小時返回字串內會有小時
        *       如果有分鐘返回字串內會有分鐘
        *       注意: 唯獨時間剩下秒鐘，才會返回秒鐘
        *            (意思是處此之外都沒有秒)
        * */

        StringBuilder builder = new StringBuilder(TIMEOUT);
        long day = (endTimeL-System.currentTimeMillis())/86400000;
        long hour = ((endTimeL-System.currentTimeMillis())%86400000)/3600000;
        long minute = ((endTimeL-System.currentTimeMillis())%3600000)/60000;
        long second = ((endTimeL-System.currentTimeMillis())%60000)/1000;

        if ((System.currentTimeMillis()-startTimeL)<0){
            builder.delete(0,builder.length());
            builder.append(STARTED_YET);
        }
        else if(day>0){
            builder.delete(0,builder.length());
            builder.append(" "+ day + " day");
            if(hour>0) {
                builder.append(" " + hour + " hr");
            }
            if(minute>0){
                builder.append(" " + hour + " min");
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

    private void scheduleNotifications(int JobID, Long time) {   //註冊Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                JobInfo jobInfo = new JobInfo.Builder(JobID, new ComponentName(getActivity().getPackageName(), NotificationJobService.class.getName()))
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

    private void initTimer(){   //初始化計時器，透過線呈每秒更新
        Thread time_date_info_thread = new Thread(timedate_runnable);
        time_date_info_thread.start();
    }

    /*計時器要做的動作*/
    Handler timedate_handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    initDate();
                    break;
                default:
                    break;
            }
        }
    };

    Runnable timedate_runnable=new Runnable() {
        @Override
        public void run() {
            do{
                try {
                    Message msg = new Message();
                    msg.what = 1;
                    timedate_handler.sendMessage(msg);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while (true);
        }
    };

    @Override
    public void onDestroy() {
        //closeDB();
        super.onDestroy();
    }
}