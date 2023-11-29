package com.abinaya.greenthumb.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.abinaya.greenthumb.R;
import com.abinaya.greenthumb.tracker.impl.actions.Group;

import java.util.List;


public class GroupTileArrayAdapter extends ArrayAdapter<Group> {

    private int viewResourceId;

    public GroupTileArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        viewResourceId = textViewResourceId;
    }

    public GroupTileArrayAdapter(Context context, int resource, List<Group> items) {
        super(context, resource, items);
        viewResourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(viewResourceId, null);
        }

        Group g = getItem(position);

        if (g != null) {
            TextView groupNameTextView = (TextView)v.findViewById(R.id.firstLine);
            TextView groupSummaryTextView = (TextView)v.findViewById(R.id.secondLine);
            TextView archivedTextView = (TextView)v.findViewById(R.id.archivedTextView);

            if (groupNameTextView != null) {
                groupNameTextView.setText(g.getGroupName());
            }

            if (groupSummaryTextView != null) {
                groupSummaryTextView.setText("");
            }
        }

        return v;
    }
}