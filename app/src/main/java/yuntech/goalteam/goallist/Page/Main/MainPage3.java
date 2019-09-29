package yuntech.goalteam.goallist.Page.Main;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import yuntech.goalteam.goallist.Activity.GoalItemActivity;
import yuntech.goalteam.goallist.Activity.GoalItemEditActivity;
import yuntech.goalteam.goallist.Adapter.ItemClickListener;
import yuntech.goalteam.goallist.Adapter.MyRecyclerViewAdapter;
import yuntech.goalteam.goallist.Datebase.GoalSQLiteOpenHelper;
import yuntech.goalteam.goallist.List.ListItem;
import yuntech.goalteam.goallist.R;

public class MainPage3 extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    TextView tv_page_title,tv_empty;
    ImageView iv_empty;
    ConstraintLayout layout_empty;

    private View view = null;
    private RecyclerView recyclerView = null;
    private RecyclerView.Adapter adapter = null;
    private List<ListItem> listItems = null;
    private SwipeRefreshLayout swipeRefreshLayout;

    /*系統服務註冊*/
    private Vibrator vibrator;              //註冊震動感應器

    private String TB = "goals_table";
    private GoalSQLiteOpenHelper helper;
    private SQLiteDatabase db;

    private static int REQUEST_CODE_RESETITEM = 1;              //請求代碼->請求刷新Item用
    private static int RETURN_CODE_RESETITEM = 1;               //返回代碼->得知刷新Item用
    private static String TIMEOUT = " time out";                //字串: 時間超時
    private static String STARTED_YET = " haven't started yet"; //字串: 尚未開始

    DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_page_1,container,false);

        swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        init();
        initDB();       //初始化資料庫
        initDate();
        initList();
        initTimer();
        return view;
    }

    private void init(){
        tv_page_title = (TextView)view.findViewById(R.id.tv_page_title);
        tv_empty = (TextView)view.findViewById(R.id.tv_empty);
        iv_empty = (ImageView)view.findViewById(R.id.iv_empty);
        layout_empty = (ConstraintLayout)view.findViewById(R.id.layout_empty);

        tv_page_title.setText("Finish");
        tv_empty.setText("There is empty\nNothing is finish");
        iv_empty.setImageResource(R.drawable.ic_file_ok_120);

        listItems = new ArrayList<ListItem>();

        vibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
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

    private void initDate(){

        /*設定要搜索資料庫裡的資料表的資料，我們在這裡定義這筆資料叫做紀錄*/
        Cursor cursor = db.query(TB,null,null,null,null,null,"id ASC");
        cursor.moveToFirst();

        for (int i = 0 ; i < cursor.getCount() ; i++){  //

            /*從記錄中讀取出開始時間和結束時間*/
            long startTimeL = (cursor.getLong(cursor.getColumnIndex("start_time")));
            long endTimeL = (cursor.getLong(cursor.getColumnIndex("end_time")));

            /*設置剩餘時間字串*/
            String result_remain=" Done at "+sdf.format(new Date(cursor.getLong(cursor.getColumnIndex("finish_time"))));

            /*開始進行item的建立、檢查、新增、刪除*/
            /*假如以下條例成立
                1.剩餘時間字串是顯示TIMEOUT
                2.該筆紀錄的done(是否完成的flag)是否為false
                3.該listItems裡並沒有任何一個物件旗下的id與此紀錄的id相同(請見 註1)
            * 那麼就新增這筆item到listItems裡，並且透過scheduleNotifications的方法來註冊該紀錄未來的通知(請見 註2)
            * 並且adapter不為null，則就通知adapter要更新這筆被新增的item到RecyclerView上
            * */
            if((cursor.getInt(cursor.getColumnIndex("done")) > 0) == true){

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
                    /*新增這筆item到listItems裡*/
                    ListItem listItem = new ListItem(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getString(cursor.getColumnIndex("notation")),
                            result_remain,
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
                        Toast.makeText(getActivity(),"Undone",Toast.LENGTH_SHORT).show();
                        if(!isChecked) {
                            //Toast.makeText(getContext(),pos+"is Done!",Toast.LENGTH_SHORT).show();
                            ContentValues values = new ContentValues();
                            values.put("done",false);
                            values.put("finish_time",System.currentTimeMillis());
                            db.update(TB,values,"id="+id,null);
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

    private void action_delete(int position){
        Toast.makeText(getActivity(),"Delete "+((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.tv_title)).getText().toString(),Toast.LENGTH_LONG).show();
        String id = ((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.tv_item_id)).getText().toString();
        listItems.remove(position);
        adapter.notifyItemRemoved(position);
        db.delete(TB,"id="+id,null);
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

    private void isListEmpty(){
        if(listItems.size()==0){
            layout_empty.setVisibility(View.VISIBLE);
        }else{
            layout_empty.setVisibility(View.GONE);
        }
    }

    public void onRefresh() {   //swipeRefreshLayout的刷新動作
        listItems.clear();
        initDate();
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
        System.out.println("Page 3: onRefresh");
    }

    @Override
    public void onResume() {    //fragment的復甦動作
        super.onResume();
        initDB();
        initDate();
        adapter.notifyDataSetChanged();
        System.out.println("Page 3: onResume");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {    //進行startActivity的返回檢查
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RETURN_CODE_RESETITEM) //進行RecycleView上Item的更新
        {
            Intent intent = data;
            String returnID = intent.getExtras().getString("id", null);
            int returnPos = intent.getExtras().getInt("position",0);
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
                return;
            }
            for (int i = 0 ; i < cursor.getCount() ; i++){
                String result_remain=" Done at "+sdf.format(new Date(cursor.getLong(cursor.getColumnIndex("finish_time"))));
                long startTimeL = (cursor.getLong(cursor.getColumnIndex("start_time")));
                long endTimeL = (cursor.getLong(cursor.getColumnIndex("end_time")));
                ListItem newListItem = new ListItem(             //製造新的ListItem
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("notation")),
                        result_remain,
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
                        (oldListItem.isDone()==(newListItem.isDone()))){
                    listItems.add(returnPos,newListItem);
                    listItems.remove(returnPos+1);
                    adapter.notifyItemChanged(returnPos);
                }
                //假使返回的資料未已完成，必須在RecycleView上做Item的刪除
                if((newListItem.isDone()==false)){
                    listItems.remove(returnPos);
                    adapter.notifyItemRemoved(returnPos);
                }
                cursor.moveToNext();
            }
            cursor.moveToNext();
            //Toast.makeText(getActivity(),"requestCode = "+requestCode+" resultCode = "+resultCode+" id= "+returnID+" pos = "+returnPos,Toast.LENGTH_SHORT).show();
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
