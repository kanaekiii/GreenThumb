package com.abinaya.greenthumb.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.abinaya.greenthumb.R;
import com.abinaya.greenthumb.android.interf.IAction;
import com.abinaya.greenthumb.android.interf.IImageCache;
import com.abinaya.greenthumb.tracker.impl.actions.Plant;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlantTileRecyclerViewAdapter extends
        RecyclerView.Adapter<PlantTileRecyclerViewAdapter.ViewHolder> {

    private final IAction<Plant> longClickAction;
    private final IAction<Plant> clickAction;

    private Context context;
    private List<Plant> list;
    private IImageCache imageCache;

    private Calendar loadTime;

    class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView plantPreview;
        public TextView plantNameTextView;
        public TextView plantSummaryTextView;
        public TextView archivedTextView;

        public ViewHolder(View v) {
            super(v);

            plantPreview = (ImageView)v.findViewById(R.id.plantPreview);
            plantNameTextView = (TextView)v.findViewById(R.id.firstLine);
            plantSummaryTextView = (TextView)v.findViewById(R.id.secondLine);
            archivedTextView = (TextView)v.findViewById(R.id.archivedTextView);
        }
    }

    public PlantTileRecyclerViewAdapter(Context context, List<Plant> items,
                                        IAction<Plant> clickAction, IAction<Plant> longClickAction,
                                        IImageCache imageCache) {
        this.context = context;
        this.list = items;
        this.clickAction = clickAction;
        this.longClickAction = longClickAction;
        this.imageCache = imageCache;
        this.loadTime = Calendar.getInstance(Locale.getDefault());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                      int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.tile_plant_list,
                null);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder,
                                 int position) {

        final Plant p = list.get(position);

        if (p != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (p.getThumbnail() != null && p.getThumbnail() != "") {
                        Bitmap bitmap = imageCache.getImage(p.getThumbnail());

                        Runnable updateUi = new Runnable() {
                            @Override
                            public void run() {
                                viewHolder.plantPreview.setImageBitmap(bitmap);
                                viewHolder.plantPreview.setAlpha(0.5f);
                                Log.d("IPV", "End of loadThumb thread");
                            }
                        };

                        ((AppCompatActivity)context).runOnUiThread(updateUi);
                    }
                }
            };

            Thread loadImage = new Thread(runnable);
            loadImage.start();

            viewHolder.plantPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickAction.exec(p);
                }
            });

            viewHolder.plantPreview.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickAction.exec(p);
                    return true;
                }
            });

            if (viewHolder.plantNameTextView != null) {
                viewHolder.plantNameTextView.setText(p.getPlantName());
            }

            if (viewHolder.plantSummaryTextView != null) {
                String flowerWeek = "";

                viewHolder.plantSummaryTextView.setText("Started " + p.getDaysFromStart() +
                        " days ago, Grow Wk. " + p.getWeeksFromStart(loadTime));
            }

            if (p.isArchived()) {
                viewHolder.archivedTextView.setVisibility(View.VISIBLE);
            }
            else    {
                viewHolder.archivedTextView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return ((list!=null) ? list.size() : 0);
    }
}