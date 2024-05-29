package com.example.findrun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatwindo extends AppCompatActivity {
    String receiverImg, receiverUid, receiverName, receiverStatus, senderUID;
    CircleImageView profile;
    TextView receiverNameView, userStatusView;
    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;
    public static String senderImg;
    public static String receiverIImg;
    CardView sendBtn;
    EditText textMsg;
    RelativeLayout rootLayout;

    String senderRoom, receiverRoom;
    RecyclerView messageAdapter;
    ArrayList<msgModelclass> messagesArrayList;
    messagesAdpter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindo);
        getSupportActionBar().hide();

        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        receiverName = getIntent().getStringExtra("nameeee");
        receiverImg = getIntent().getStringExtra("reciverImg");
        receiverUid = getIntent().getStringExtra("uid");
        receiverStatus = getIntent().getStringExtra("status");
        senderUID = firebaseAuth.getUid();

        senderRoom = senderUID + receiverUid;
        receiverRoom = receiverUid + senderUID;

        sendBtn = findViewById(R.id.sendbtnn);
        textMsg = findViewById(R.id.textmsg);
        receiverNameView = findViewById(R.id.recivername);
        userStatusView = findViewById(R.id.userstatus);
        profile = findViewById(R.id.profileimgg);
        messageAdapter = findViewById(R.id.msgadpter);
        messagesArrayList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageAdapter.setLayoutManager(linearLayoutManager);
        messagesAdapter = new messagesAdpter(chatwindo.this, messagesArrayList);
        messageAdapter.setAdapter(messagesAdapter);

        Picasso.get().load(receiverImg).into(profile);
        receiverNameView.setText(receiverName);
        userStatusView.setText(receiverStatus);

        DatabaseReference reference = database.getReference().child("user").child(senderUID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg = snapshot.child("profilepic").getValue(String.class);
                receiverIImg = receiverImg;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DatabaseReference chatReference = database.getReference().child("chats").child(senderRoom).child("messages");
        chatReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModelclass message = dataSnapshot.getValue(msgModelclass.class);
                    messagesArrayList.add(message);
                }
                messagesAdapter.notifyDataSetChanged();
                messageAdapter.scrollToPosition(messagesArrayList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textMsg.getText().toString();
                if (message.isEmpty()) {
                    Toast.makeText(chatwindo.this, "Enter The Message First", Toast.LENGTH_SHORT).show();
                    return;
                }
                textMsg.setText("");
                Date date = new Date();
                msgModelclass messageObj = new msgModelclass(message, senderUID, date.getTime());
                messageObj.setRead(false);

                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .push().setValue(messageObj).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .child("messages")
                                        .push().setValue(messageObj).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    messageAdapter.smoothScrollToPosition(messagesArrayList.size() - 1);
                                                }
                                            }
                                        });
                            }
                        });
            }
        });

        messageAdapter.scrollToPosition(messagesArrayList.size() - 1);
        rootLayout = findViewById(R.id.root_layout);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight < screenHeight * 0.15) {
                    messageAdapter.scrollToPosition(messagesArrayList.size() - 1);
                }
            }
        });
    }
}
