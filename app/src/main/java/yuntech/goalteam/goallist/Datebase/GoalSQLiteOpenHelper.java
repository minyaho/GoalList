package yuntech.goalteam.goallist.Datebase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GoalSQLiteOpenHelper extends SQLiteOpenHelper {
    private final static String DB = "GoalDB.db";
    private final static String TB = "goals_table";
    private final int VS = 2;

    public GoalSQLiteOpenHelper(Context context,String name,SQLiteDatabase.CursorFactory factory, int version) {
        //super(context, name, factory, version);

        super(context, DB, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createStoreTable = "create table if not exists "+TB+" (" +
                "id integer primary key autoincrement, " +          //id
                "name text not null, " +                            //標題
                "notation text," +                                  //附註
                "start_time TIMESTAMP not null," +                  //開始時間
                "end_time TIMESTAMP not null," +                    //結束時間
                "finish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+ //完成時間
                "notify_time TIMESTAMP,"+                           //通知時間
                "done integer not null,"+                          //是否完成      0:未完成, 1:完成
                "notify integer not null default 0)";                //是否需要通知    0:不需要, 1:需要
        db.execSQL(createStoreTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String dropTable = "drop table "+TB;
        db.execSQL(dropTable);
    }
}
