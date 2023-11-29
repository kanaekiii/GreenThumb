package com.abinaya.greenthumb.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abinaya.greenthumb.R;
import com.abinaya.greenthumb.android.interf.IImageCache;

import java.io.File;
import java.util.ArrayList;

import static com.abinaya.greenthumb.android.AndroidUtility.decodeSampledBitmapFromResource;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class ImageViewRecyclerViewAdapter extends RecyclerView.Adapter<ImageViewRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> imagePaths;
    private LruCache<String, Bitmap> imageCache;

    class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imagePreview;
        TextView bottomCaption;

        ViewHolder(View v) {
            super(v);

            imagePreview = v.findViewById(R.id.plantPreview);
            bottomCaption = v.findViewById(R.id.bottomCaptionTextView);
        }
    }

    public ImageViewRecyclerViewAdapter(Context c, ArrayList<String> images)   {
        context = c;
        imagePaths = images;
        createImageCache();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.tile_image_list,null);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        String path = imagePaths.get(position);

        if (path == null || path.equals(""))    {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = cache.getImage(path);


                Runnable updateUi = new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.imagePreview.setImageBitmap(bitmap);
                        viewHolder.imagePreview.setAlpha(0.5f);
                        viewHolder.bottomCaption.setText("");
                    }
                };

                ((AppCompatActivity)context).runOnUiThread(updateUi);
            }
        };

        Thread loadImage = new Thread(runnable);
        loadImage.start();
    }

    @Override
    public int getItemCount() {
        return ((imagePaths!=null) ? imagePaths.size() : 0);
    }

    private void createImageCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        final int cacheSize = maxMemory / 8;

        imageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private IImageCache cache = new IImageCache() {
        @Override
        public Bitmap getImage(String path) {
            if (path == null)   {
                return null;
            }

            Bitmap bmap;
            if ((bmap=imageCache.get(path)) == null)   {
                bmap = decodeSampledBitmapFromResource(new File(path), 400, 300);

                imageCache.put(path, bmap);
            }

            return bmap;
        }
    };
}
