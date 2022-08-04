package com.example.youtube.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.youtube.Models.ContentModel;
import com.example.youtube.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {

    Context context;
    ArrayList<ContentModel> list;
    DatabaseReference reference;

    public ContentAdapter(Context context, ArrayList<ContentModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_video,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ContentModel model = list.get(position);
        if (model != null) {
            Glide.with(context).asBitmap().load(model.getVideo_url()).into(holder.thumbnail);
            holder.video_title.setText(model.getVideo_title());
            holder.views.setText(model.getViews()+" Views");
            holder.date.setText(model.getDate());

            setData(model.getPublisher(), holder.channel_logo, holder.channel_name);
        }

    }

    private void setData(String publisher , CircleImageView logo, TextView channel_name) {
        reference = FirebaseDatabase.getInstance().getReference().child("Channels");
        reference.child(publisher).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String cName = snapshot.child("channel_name").getValue().toString();
                    String cLogo = snapshot.child("channel_logo").getValue().toString();
                    channel_name.setText(cName);
                    Picasso.get().load(cLogo).placeholder(R.drawable.ic_baseline_account_circle_24).into(logo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        TextView video_title,channel_name, views,date;
        CircleImageView channel_logo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            thumbnail = itemView.findViewById(R.id.thumbnail);
            video_title = itemView.findViewById(R.id.video_title);
            channel_name = itemView.findViewById(R.id.channel_name);
            views = itemView.findViewById(R.id.views_count);
            channel_logo = itemView.findViewById(R.id.channel_logo);
            date = itemView.findViewById(R.id.date);


        }
    }
}
