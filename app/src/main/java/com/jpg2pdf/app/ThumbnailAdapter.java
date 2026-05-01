package com.jpg2pdf.app;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 缩略图适配器，水平展示已选图片
 */
public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {

    private final List<Uri> imageUris = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemDeleteListener deleteListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemDeleteListener {
        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setImages(List<Uri> uris) {
        imageUris.clear();
        imageUris.addAll(uris);
        notifyDataSetChanged();
    }

    public List<Uri> getImages() {
        return imageUris;
    }

    public void addImage(Uri uri) {
        imageUris.add(uri);
        notifyItemInserted(imageUris.size() - 1);
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUris.size());
        }
    }

    public void clear() {
        imageUris.clear();
        notifyDataSetChanged();
    }

    public int getCount() {
        return imageUris.size();
    }

    public Uri getUri(int position) {
        return imageUris.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_thumbnail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.ivThumbnail);
        holder.tvIndex.setText(String.valueOf(position + 1));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvIndex;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvIndex = itemView.findViewById(R.id.tvIndex);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
