package com.example.youtube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.youtube.Adapter.PlaylistAdapter;
import com.example.youtube.Models.PlaylistModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class PublishContentActivity extends AppCompatActivity {

    EditText input_video_title , input_video_description , input_video_tag;
    LinearLayout progressLyt;
    ProgressBar progressBar;
    TextView progress_text;

    VideoView videoView;
    String type;
    Uri videoUri;

    MediaController mediaController;
    TextView txt_upload;

    TextView txt_choose_playlist;
    Dialog dialog;

    FirebaseUser user;
    DatabaseReference reference;
    StorageReference storageReference;

    String selectedPlaylist;
    int videosCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_content);

        txt_upload = findViewById(R.id.upload_bt);
        videoView = findViewById(R.id.videoView);

        input_video_title = findViewById(R.id.input_video_title);
        input_video_description = findViewById(R.id.input_video_description);
        input_video_tag = findViewById(R.id.input_video_tag);
        progressLyt = findViewById(R.id.progressLyt);
        progress_text = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progressBar);


        mediaController = new MediaController(PublishContentActivity.this);

        txt_choose_playlist = findViewById(R.id.chosse_playlist);

        Intent intent = getIntent();

        if (intent != null) {
            videoUri = intent.getData();
            videoView.setVideoURI(videoUri);
            videoView.setMediaController(mediaController);
            videoView.start();
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference().child("Content");
        storageReference = FirebaseStorage.getInstance().getReference().child("Content");



        txt_choose_playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlaylistDialogue();
            }
        });

        txt_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = input_video_title.getText().toString();
                String description = input_video_description.getText().toString();
                String tags = input_video_tag.getText().toString();

                if (title.isEmpty() || description.isEmpty() || tags.isEmpty()) {
                    Toast.makeText(PublishContentActivity.this, "Fill All Fields ...", Toast.LENGTH_SHORT).show();
                } else if (txt_choose_playlist.getText().toString().equals("Choose Playlist")) {
                    Toast.makeText(PublishContentActivity.this, "Please Select Playlist", Toast.LENGTH_SHORT).show();
                } else {
                    uploadVideoToStorage(title,description,tags);
                }
            }
        });

    }

    private void uploadVideoToStorage(String title, String description, String tags) {
        progressLyt.setVisibility(View.VISIBLE);
        final StorageReference sRef = storageReference.child(user.getUid()).child(System.currentTimeMillis()+","+getFileExtention(videoUri));
        sRef.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                sRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String videoUrl = uri.toString();

                        saveDataToFirebase(title,tags,description,videoUrl);
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = 100.0*snapshot.getBytesTransferred()/snapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                progress_text.setText("uploading " +(int)progress+"%");
            }
        });

    }

    private void saveDataToFirebase(String title, String tags, String description, String videoUrl) {
        String currentDate = DateFormat.getDateInstance().format(new Date());
        String id = reference.push().getKey();

        HashMap<String,Object> map = new HashMap<>();
        map.put("id" , id);
        map.put("video_title" , title);
        map.put("video_description" , description);
        map.put("video_tag" , tags);
        map.put("playlist" , selectedPlaylist);
        map.put("video_url" , videoUrl);
        map.put("publisher" , user.getUid());
        map.put("type" , "video");
        map.put("views" , 0);
        map.put("date" , currentDate);

        reference.child(id).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    progressLyt.setVisibility(View.GONE);
                    Toast.makeText(PublishContentActivity.this, "Video Uploaded Successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PublishContentActivity.this,MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();

                    updateVideoCount();
                }
                else {
                    progressLyt.setVisibility(View.GONE);
                    Toast.makeText(PublishContentActivity.this, "Failed to Upload" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateVideoCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Playlists");

        int update = videosCount + 1;
        HashMap<String , Object> map = new HashMap<>();
        map.put("videos" , update);

        reference.child(user.getUid()).child(selectedPlaylist).updateChildren(map);

    }

    private String getFileExtention(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void showPlaylistDialogue() {
        dialog = new Dialog(PublishContentActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.play_list_dialog);
        dialog.setCancelable(true);
        dialog.show();


        EditText input_playlist_name = dialog.findViewById(R.id.input_playlist_name);
        TextView txt_add = dialog.findViewById(R.id.txt_add);

        ArrayList<PlaylistModel> list = new ArrayList<>();
        PlaylistAdapter adapter;
        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        adapter = new PlaylistAdapter(PublishContentActivity.this, list, new PlaylistAdapter.OnitemClickListener() {
            @Override
            public void onitemClick(PlaylistModel model) {

                dialog.dismiss();
                selectedPlaylist = model.getPlaylist_name();
                videosCount = model.getVideos();
                txt_choose_playlist.setText("Playlist : " + model.getPlaylist_name());

            }
        });

        recyclerView.setAdapter(adapter);

        checkUserAlreadyHavePlaylist(recyclerView);

        showAllPlaylists(adapter , list);

        txt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = input_playlist_name.getText().toString();
                if(value.isEmpty()) {
                    Toast.makeText(PublishContentActivity.this, "Enter Playlist name", Toast.LENGTH_SHORT).show();
                }else {
                    createNewPlaylist(value);
                }
            }
        });

    }

    private void showAllPlaylists(PlaylistAdapter adapter, ArrayList<PlaylistModel> list) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Playlists");

        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    list.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        PlaylistModel model = dataSnapshot.getValue(PlaylistModel.class);
                        list.add(model);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PublishContentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void createNewPlaylist(String value) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Playlists");

        HashMap<String,Object> map = new HashMap<>();
        map.put("playlist_name" , value);
        map.put("videos" , 0);
        map.put("uid" , user.getUid());

        reference.child(user.getUid()).child(value).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(PublishContentActivity.this, "New Playlist Created", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(PublishContentActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkUserAlreadyHavePlaylist(RecyclerView recyclerView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Playlists");
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    Toast.makeText(PublishContentActivity.this, "User have Playlist", Toast.LENGTH_SHORT).show();
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PublishContentActivity.this,error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }
}