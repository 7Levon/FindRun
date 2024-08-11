package com.example.findrun;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdpter extends RecyclerView.Adapter<UserAdpter.ViewHolder> {
    private Context mainActivity;
    private ArrayList<Users> usersArrayList;
    private DatabaseReference userRef;

    public UserAdpter(Context mainActivity, ArrayList<Users> usersArrayList) {
        this.mainActivity = mainActivity;
        this.usersArrayList = usersArrayList;
        userRef = FirebaseDatabase.getInstance().getReference().child("user");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = usersArrayList.get(position);
        holder.username.setText(user.getUserName());

        // Use Glide to load the user avatar
        Glide.with(mainActivity)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man) // default avatar in case of no image
                .into(holder.userimg);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatRoomId = currentUserId + user.getUserId();
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference().child("chats").child(chatRoomId).child("messages");

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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userimg;
        TextView username;
        TextView userstatus;
        ImageView unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.userimg);
            username = itemView.findViewById(R.id.username);
            userstatus = itemView.findViewById(R.id.userstatus);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}
