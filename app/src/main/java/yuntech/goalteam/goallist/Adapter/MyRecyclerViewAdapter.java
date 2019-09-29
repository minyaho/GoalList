package yuntech.goalteam.goallist.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import yuntech.goalteam.goallist.List.ListItem;
import yuntech.goalteam.goallist.R;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {
    private List<ListItem> listItems;
    private Context context;

    public MyRecyclerViewAdapter(List<ListItem> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_1,viewGroup,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        ListItem listItem = listItems.get(i);
        viewHolder.textViewId.setText(Integer.toString(listItem.getId()));
        viewHolder.textViewTitle.setText(listItem.getTitle());
        viewHolder.textViewContext.setText(listItem.getContext());
        viewHolder.checkBoxDone.setChecked(listItem.isDone());
        viewHolder.textViewRemain.setText(listItem.getRemain());
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView textViewId;
        public TextView textViewTitle;
        public TextView textViewContext;
        public CheckBox checkBoxDone;
        public TextView textViewRemain;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewId = (TextView)itemView.findViewById(R.id.tv_item_id);
            textViewTitle = (TextView)itemView.findViewById(R.id.tv_title);
            textViewContext = (TextView)itemView.findViewById(R.id.tv_context);
            checkBoxDone = (CheckBox)itemView.findViewById(R.id.ch_done);
            textViewRemain = (TextView)itemView.findViewById(R.id.tv_remain);
        }
    }
}
