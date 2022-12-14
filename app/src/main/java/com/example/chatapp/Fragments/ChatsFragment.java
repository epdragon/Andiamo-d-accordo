package com.example.chatapp.Fragments;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.chatapp.Adapter.ChatAdapter;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.Chatlist;
import com.example.chatapp.Model.User;
import com.example.chatapp.Notifications.Token;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class ChatsFragment extends Fragment {
    public static final String TAG = "ChatsFragment";
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<User> mUsers;

    private FirebaseUser firebaseUser;
    private DatabaseReference reference;

    private Stack<Chatlist> usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        usersList = new Stack<>();


        if (firebaseUser != null) {
            Query reference1 = FirebaseDatabase.getInstance().getReference("Chatlist").child(firebaseUser.getUid()).orderByChild("time");
            // Get every user from chatlist reference
            reference1.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    usersList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Chatlist chatlist = snapshot.getValue(Chatlist.class);
                        usersList.push(chatlist);
                    }
                    Collections.reverse(usersList);
                    chatList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { throw databaseError.toException(); }
            });
        }
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String mtoken = task.getResult();
                    updateToken(mtoken);
                    Log.d("TAG", mtoken); //Check the generated token.
                } else {
                    Log.d("TAG", task.getException().getMessage()); //Never ignore potential errors!
                }
            }
        });

        registerForContextMenu(recyclerView);

        return view;
    }

    private void updateToken(String token) {
        if (firebaseUser != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
            Token token1 = new Token(token);
            reference.child(firebaseUser.getUid()).setValue(token1);
        }
    }
   // if (chatlist.getId().equals(mUsers.getId()) && chatList.hasMessages == 1)

    // Show users with whom you have talked
    private void chatList() {
        mUsers = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");

            reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();
                // For every user check their id and if it matches with chatlist id add them to the list
                for (Chatlist chatlist : usersList) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        if (chatlist.getId().equals(user.getId())) {
                            mUsers.add(user);
                        }
                    }
                }
                // Show them with the recycler view
                Log.d(TAG, "String mUsers: " + mUsers);
                chatAdapter = new ChatAdapter(getContext(), mUsers);
                recyclerView.setAdapter(chatAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { throw databaseError.toException(); }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int position = -1;
        try {
            position = ((ChatAdapter) recyclerView.getAdapter()).getPosition();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
            return super.onContextItemSelected(item);
        }
        final User user = mUsers.get(position);
        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        switch (item.getItemId()) {
            case R.id.del_friend:
                FirebaseDatabase.getInstance().getReference("Friends").child(fuser.getUid()).child(user.getId()).removeValue();
                FirebaseDatabase.getInstance().getReference("Friends").child(user.getId()).child(fuser.getUid()).removeValue();
            case R.id.deleting:
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot deleteSnapshot : dataSnapshot.getChildren()) {
                            Chat chat = deleteSnapshot.getValue(Chat.class);
                            assert chat != null;
                            if (chat.getReceiver() != null && chat.getSender() != null) {
                                if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(user.getId()) ||
                                        chat.getReceiver().equals(user.getId()) && chat.getSender().equals(fuser.getUid())) {
                                    if (chat.getDeletedfrom().equals("none")) {
                                        HashMap<String, Object> hashMap = new HashMap<>();
                                        hashMap.put("deletedfrom", fuser.getUid());
                                        deleteSnapshot.getRef().updateChildren(hashMap);
                                    } else if (chat.getDeletedfrom().equals(user.getId())) {
                                        deleteSnapshot.getRef().removeValue();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        throw databaseError.toException();
                    }
                });
            case R.id.archiving:
                FirebaseDatabase.getInstance().getReference("Chatlist").child(fuser.getUid()).child(user.getId()).removeValue();
                break;

        }
        return super.onContextItemSelected(item);
    }
}