package com.sibilante.houseseats.client;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.sibilante.houseseats.client.model.Show;

import java.util.List;

public class RecentShowsAdapter extends RecyclerView.Adapter<RecentShowsAdapter.ShowViewHolder> {
    private List<Show> showsDataset;
    private LayoutInflater mInflater;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecentShowsAdapter(Context context, List<Show> showsDataset) {
        this.mInflater = LayoutInflater.from(context);
        this.showsDataset = showsDataset;
    }

    @NonNull
    @Override
    public ShowViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, viewGroup, false);
        return new ShowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowViewHolder showViewHolder, int position) {
        Show show = showsDataset.get(position);
        showViewHolder.textView.setText(show.getName());
    }

    @Override
    public int getItemCount() {
        return showsDataset.size();
    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ShowViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView textView;

        public ShowViewHolder(View view) {
            super(view);
            textView = itemView.findViewById(R.id.tvShow);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //TODO https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
        }
    }

}
