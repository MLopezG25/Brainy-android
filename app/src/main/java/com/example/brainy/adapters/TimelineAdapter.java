package com.example.brainy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.TimelineYear;

import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_YEAR_HEADER = 0;
    private static final int TYPE_ENTRY = 1;

    private List<TimelineYear> data;
    private OnEntryClickListener listener;
    private final List<Object> flatList = new ArrayList<>();

    public interface OnEntryClickListener {
        void onEntryClick(Entry entry);
    }

    public TimelineAdapter(List<TimelineYear> data, OnEntryClickListener listener) {
        this.data = data;
        this.listener = listener;
        buildFlatList();
    }

    public void updateData(List<TimelineYear> newData) {
        this.data = newData;
        buildFlatList();
        notifyDataSetChanged();
    }

    private void buildFlatList() {
        flatList.clear();
        for (TimelineYear year : data) {
            flatList.add(year.getYear());
            flatList.addAll(year.getEntries());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return flatList.get(position) instanceof String ? TYPE_YEAR_HEADER : TYPE_ENTRY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_YEAR_HEADER) {
            return new YearViewHolder(inflater.inflate(R.layout.item_timeline_year, parent, false));
        }
        return new EntryViewHolder(inflater.inflate(R.layout.item_timeline_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof YearViewHolder) {
            ((YearViewHolder) holder).tvYear.setText((String) flatList.get(position));
        } else if (holder instanceof EntryViewHolder) {
            Entry entry = (Entry) flatList.get(position);
            EntryViewHolder vh = (EntryViewHolder) holder;
            vh.tvTitle.setText(entry.getTitle());
            if (entry.getCategoryName() != null) {
                vh.tvCategory.setText(entry.getCategoryName());
                vh.tvCategory.setVisibility(View.VISIBLE);
            } else {
                vh.tvCategory.setVisibility(View.GONE);
            }
            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEntryClick(entry);
            });
        }
    }

    @Override
    public int getItemCount() {
        return flatList.size();
    }

    static class YearViewHolder extends RecyclerView.ViewHolder {
        TextView tvYear;
        YearViewHolder(@NonNull View itemView) {
            super(itemView);
            tvYear = itemView.findViewById(R.id.tvTimelineYear);
        }
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory;
        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTimelineEntryTitle);
            tvCategory = itemView.findViewById(R.id.tvTimelineEntryCategory);
        }
    }
}