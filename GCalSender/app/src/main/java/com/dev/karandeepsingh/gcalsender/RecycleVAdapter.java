package com.dev.karandeepsingh.gcalsender;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Karan Deep Singh on 1/23/2016.
 */
public class RecycleVAdapter extends RecyclerView.Adapter<RecycleVAdapter.EventViewHolder> {

    List<EventMetaData> eventMetaDataList;
    Map<Integer, EventMetaData> selectedEvents;

    RecycleVAdapter(List<EventMetaData> list) {
        selectedEvents = new HashMap<>();
        this.eventMetaDataList = new ArrayList<>();
        setMetadataArray(list);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void setMetadataArray(List<EventMetaData> list) {
        this.eventMetaDataList.clear();
        this.eventMetaDataList.addAll(list);
        notifyDataSetChanged();
    }

    public Map<Integer, EventMetaData> getSelectedEvents() {
        return selectedEvents;
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_item, viewGroup, false);
        EventViewHolder evh = new EventViewHolder(v);
        return evh;
    }

    @Override
    public void onBindViewHolder(EventViewHolder eventViewHolder, int i) {
        String[] viewsText = Utils.processDate(eventMetaDataList.get(i).eventInfo);
        eventViewHolder.eventName.setText(viewsText[0]);
        eventViewHolder.eventDate.setText(viewsText[1]);
        eventViewHolder.checkBox.setTag(i);
        eventViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Integer selectedIndex = (Integer) compoundButton.getTag();
                EventMetaData eventData = eventMetaDataList.get(selectedIndex);
                if (b) {
                    selectedEvents.put(selectedIndex, eventData);
                } else {
                    selectedEvents.remove(selectedIndex);
                }
            }
        });
        EventMetaData event = selectedEvents.get(i);
        if (event == null) {
            eventViewHolder.checkBox.setChecked(false);
        } else {
            eventViewHolder.checkBox.setChecked(true);
        }
    }

    @Override
    public int getItemCount() {
        return eventMetaDataList.size();
    }


    public static class EventViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView eventName;
        TextView eventDate;
        CheckBox checkBox;


        EventViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv);
            eventName = (TextView) itemView.findViewById(R.id.eventName);
            eventDate = (TextView) itemView.findViewById(R.id.eventDate);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        }
    }
}
