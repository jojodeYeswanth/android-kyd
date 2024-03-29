package com.example.map;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {
    private View RequestsFragmentView;
    private RecyclerView myRequestsList;
    private DatabaseReference ChatRequestsRef, UsersRef, ContactsRef;
    private FirebaseAuth mAuth;
    private String currentUserID;
    ProgressDialog loading;
    public RequestsFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RequestsFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        myRequestsList = RequestsFragmentView.findViewById(R.id.chat_requests_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return RequestsFragmentView;
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(ChatRequestsRef.child(currentUserID), Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Contacts model) {
                        holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                        holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);

                        final String list_user_id = getRef(position).getKey();
                        DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();
                        getTypeRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    String type = dataSnapshot.getValue().toString();
                                    if (type.equals("received")) {
                                        UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChild("image")) {
                                                    final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                                    final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Profile Images");
                                                    storageReference.child(list_user_id+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            Log.d("UriImageChat",uri.toString());
                                                            Picasso.get().load(uri).into(holder.profileImage);

                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception exception) {
                                                            // Handle any errors
                                                        }
                                                    });
                                                }
                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                                final String requestUserStatus = dataSnapshot.child("address").getValue().toString();

                                                holder.userName.setText(requestUserName);
                                                holder.userStatus.setText("wants to connect with you.");

                                                holder.AcceptButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        loading = ProgressDialog.show(getContext(), null, "Adding Contact...", true, true);
                                                        ContactsRef.child(currentUserID).child(list_user_id).child("Contact")
                                                                .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    ContactsRef.child(list_user_id).child(currentUserID).child("Contact")
                                                                            .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                ChatRequestsRef.child(currentUserID).child(list_user_id)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if (task.isSuccessful()) {
                                                                                                    ChatRequestsRef.child(list_user_id).child(currentUserID)
                                                                                                            .removeValue()
                                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                @Override
                                                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                                                    if (task.isSuccessful()) {
                                                                                                                        Toast.makeText(getContext(), "New Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                                                        loading.dismiss();
                                                                                                                    }
                                                                                                                }
                                                                                                            });
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    }
                                                });
                                                holder.CancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        loading = ProgressDialog.show(getContext(), null, "Removing Contact...", true, true);
                                                        ChatRequestsRef.child(currentUserID).child(list_user_id)
                                                                .removeValue()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                    {
                                                                        if (task.isSuccessful()) {
                                                                            ChatRequestsRef.child(list_user_id).child(currentUserID)
                                                                                    .removeValue()
                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                                        {
                                                                                            if (task.isSuccessful()) {
                                                                                                Toast.makeText(getContext(), "Contact Deleted", Toast.LENGTH_SHORT).show();
                                                                                                loading.dismiss();
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                });

                                            }
                                            @Override
                                            public void onCancelled(DatabaseError databaseError) { }
                                        });
                                    }
                                    else if (type.equals("sent")) {
                                        Button request_sent_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                        request_sent_btn.setText("Req Sent");
                                        holder.CancelButton.setText("Cancel Req");
                                        holder.CancelButton.setVisibility(View.VISIBLE);
                                        UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChild("image")) {
                                                    final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                                    final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Profile Images");
                                                    storageReference.child(list_user_id+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            Log.d("UriImageChat",uri.toString());
                                                            Picasso.get().load(uri).into(holder.profileImage);

                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception exception) {
                                                            // Handle any errors
                                                        }
                                                    });
                                                }

                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();

                                                holder.userName.setText(requestUserName);
                                                holder.userStatus.setText("you have sent a request to " + requestUserName);
                                                holder.CancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        loading = ProgressDialog.show(getContext(), null, "Cancelling the Chat Request...", true, true);
                                                        ChatRequestsRef.child(currentUserID).child(list_user_id)
                                                                .removeValue()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                    {
                                                                        if (task.isSuccessful())
                                                                        {
                                                                            ChatRequestsRef.child(list_user_id).child(currentUserID)
                                                                                    .removeValue()
                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                                        {
                                                                                            if (task.isSuccessful()) {
                                                                                                loading.dismiss();
                                                                                                Toast.makeText(getContext(), "you have cancelled the chat request.", Toast.LENGTH_SHORT).show();
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onCancelled(DatabaseError databaseError) { }
                                        });
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                    }
                    @NonNull
                    @Override
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                        RequestsViewHolder holder = new RequestsViewHolder(view);
                        return holder;
                    }
                };
        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        Button AcceptButton, CancelButton;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            AcceptButton = itemView.findViewById(R.id.request_accept_btn);
            CancelButton = itemView.findViewById(R.id.request_cancel_btn);
        }
    }
}
