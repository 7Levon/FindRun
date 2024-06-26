package com.example.findrun;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserAdpter extends RecyclerView.Adapter<UserAdpter.viewholder> {
    Context mainActivity;
    ArrayList<Users> usersArrayList;
    DatabaseReference userRef;

    public UserAdpter(Context mainActivity, ArrayList<Users> usersArrayList) {
        this.mainActivity = mainActivity;
        this.usersArrayList = usersArrayList;
        userRef = FirebaseDatabase.getInstance().getReference().child("user");
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.user_item, parent, false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, @SuppressLint("RecyclerView") int position) {
        Users user = usersArrayList.get(position);
        holder.username.setText(user.getUserName());
        Picasso.get().load(user.getProfilepic()).into(holder.userimg);

        // Get the current user's ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Construct the chat reference path
        String chatRoomId = currentUserId + user.getUserId();
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference().child("chats").child(chatRoomId).child("messages");

        // Add a ValueEventListener to update the unread indicator in real-time
        chatReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasUnreadMessages = false;
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    msgModelclass message = messageSnapshot.getValue(msgModelclass.class);
                    if (message != null && !message.isRead() && message.getSenderid().equals(user.getUserId())) {
                        hasUnreadMessages = true;
                        break;
                    }
                }
                holder.unreadIndicator.setVisibility(hasUnreadMessages ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mainActivity, chatwindo.class);
                intent.putExtra("nameeee", user.getUserName());
                intent.putExtra("reciverImg", user.getProfilepic());
                intent.putExtra("uid", user.getUserId());
                intent.putExtra("status", user.getStatus());
                mainActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersArrayList.size();
    }

    public class viewholder extends RecyclerView.ViewHolder {
        CircleImageView userimg;
        TextView username;
        TextView userstatus;
        ImageView unreadIndicator;

        public viewholder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.userimg);
            username = itemView.findViewById(R.id.username);
            userstatus = itemView.findViewById(R.id.userstatus);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}

