package yuntech.goalteam.goallist.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import yuntech.goalteam.goallist.Adapter.MyFragmentAdapter;
import yuntech.goalteam.goallist.R;
import yuntech.goalteam.goallist.Page.Main.MainPage1;
import yuntech.goalteam.goallist.Page.Main.MainPage2;
import yuntech.goalteam.goallist.Page.Main.MainPage3;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    ArrayList<Fragment> mFragments;
    ArrayList<String> mTitle;
    ViewPager mainViewPager;
    TabLayout mainTabLayout;
    FloatingActionButton mainFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initViewPager();
        initTabLayout();
        initFab();

    }

    private void initToolbar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initTabLayout() {
        mainTabLayout = (TabLayout) findViewById(R.id.main_tablayout);
        //mainTabLayout.setTabMode(TabLayout.MODE_FIXED);
        //ViewCompat.setElevation(mainTabLayout,10);
        mainTabLayout.setupWithViewPager(mainViewPager);
        for (int i = 0; i < mFragments.size(); i++) {
            TabLayout.Tab itemTab = mainTabLayout.getTabAt(i);
            itemTab.setCustomView(R.layout.main_tab_item_layout);
            TextView itemTab_title = (TextView)itemTab.getCustomView().findViewById(R.id.tab_title);
            ImageView imageView_icon = (ImageView)itemTab.getCustomView().findViewById(R.id.tab_icon);
            switch (i) {
                case 0:
                    itemTab_title.setText(mainTabLayout.getTabAt(i).getText());
                    imageView_icon.setImageResource(R.drawable.selector_ic_event);
                    break;
                case 1:
                    itemTab_title.setText(mainTabLayout.getTabAt(i).getText());
                    imageView_icon.setImageResource(R.drawable.selector_ic_event_busy);
                    break;
                case 2:
                    itemTab_title.setText(mainTabLayout.getTabAt(i).getText());
                    imageView_icon.setImageResource(R.drawable.selector_ic_event_available);
                    break;
                default:
                    break;
            }
        }
        mainTabLayout.getTabAt(0).getCustomView().setSelected(true);
    }

    private void initViewPager(){

        mFragments = new ArrayList<Fragment>();
        mFragments.add(new MainPage1());
        mFragments.add(new MainPage2());
        mFragments.add(new MainPage3());

        mTitle = new ArrayList<String>();
        mTitle.add("Next");
        mTitle.add("Delay");
        mTitle.add("Finish");

        MyFragmentAdapter adapter = new MyFragmentAdapter(getSupportFragmentManager(),mFragments,mTitle);
        mainViewPager = (ViewPager)findViewById(R.id.main_viewpager);
        mainViewPager.setAdapter(adapter);


    }

    private void initFab(){
        mainFab = findViewById(R.id.main_fab);
        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                Intent intent = new Intent(MainActivity.this,GoalItemAddActivity.class);
                startActivity(intent);
            }
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                action_about();
                break;
            default:
                break;
        }
        return true;
    }

    private void action_about(){
        Intent intent = new Intent(this,AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode==KeyEvent.KEYCODE_BACK){
            ConfirmExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void ConfirmExit(){//退出確認
        AlertDialog.Builder ad=new AlertDialog.Builder(this);
        ad.setTitle("Exit");
        ad.setMessage("Do you want to leave this program?");
        ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {//退出按鈕
            public void onClick(DialogInterface dialog, int i) {
                // TODO Auto-generated method stub
                MainActivity.this.finish();//關閉activity
            }
        });
        ad.setNegativeButton("No",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                //不退出不用執行任何操作
            }
        });
        ad.show();//顯示對話框
    }

}
