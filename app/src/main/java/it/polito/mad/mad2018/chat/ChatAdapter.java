package it.polito.mad.mad2018.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.mad2018.MAD2018Application;
import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Conversation;
import it.polito.mad.mad2018.data.UserProfile;
import it.polito.mad.mad2018.utils.Utilities;

public class ChatAdapter extends FirebaseRecyclerAdapter<Conversation, ChatAdapter.ChatHolder> {

    private static final String TAG = "ChatAdapter";
    private final OnItemClickListener onItemClickListener;
    private final OnItemCountChangedListener onItemCountChangedListener;
    private ValueEventListener profileListener;

    ChatAdapter(@NonNull FirebaseRecyclerOptions<Conversation> options,
                @NonNull OnItemClickListener onItemClickListener,
                @NonNull OnItemCountChangedListener onItemCountChangedListener) {
        super(options);
        this.onItemClickListener = onItemClickListener;
        this.onItemCountChangedListener = onItemCountChangedListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Conversation model) {
        holder.update(model, "");

    }

    @Override
    public void onViewAttachedToWindow(@NonNull ChatHolder holder) {
        super.onViewAttachedToWindow(holder);
        setOnProfileLoadedListener(holder.model, holder);
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatAdapter.ChatHolder(view, onItemClickListener);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        onItemCountChangedListener.onCountChangedListener(this.getItemCount());
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ChatHolder holder) {
        super.onViewDetachedFromWindow(holder);
        unsetOnProfileLoadedListener(holder.model);
    }

    private void setOnProfileLoadedListener(Conversation model, ChatHolder holder) {

        this.profileListener = UserProfile.setOnProfileLoadedListener(
                model.getPeerUserId(),
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnProfileLoadedListener(model)) {
                            return;
                        }

                        UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                        if (data != null) {
                            UserProfile user = new UserProfile(model.getPeerUserId(), data, MAD2018Application.applicationContext.getResources());
                            holder.update(model, user.getUsername());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        unsetOnProfileLoadedListener(model);
                    }
                });
    }

    private boolean unsetOnProfileLoadedListener(Conversation model) {
        if (this.profileListener != null) {
            UserProfile.unsetOnProfileLoadedListener(model.getPeerUserId(), this.profileListener);
            this.profileListener = null;
            return true;
        }
        return false;
    }

    interface OnItemClickListener {
        void onClick(View view, Conversation conversation);
    }

    interface OnItemCountChangedListener {
        void onCountChangedListener(int count);
    }

    static class ChatHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final TextView peerId;
        private final TextView bookTitle;
        private final TextView message;
        private final TextView date;

        private Conversation model;

        ChatHolder(View view, @NonNull OnItemClickListener listener) {
            super(view);

            this.context = view.getContext();
            this.peerId = view.findViewById(R.id.cl_chat_item_peer);
            this.message = view.findViewById(R.id.cl_chat_item_message);
            this.date = view.findViewById(R.id.cl_chat_item_date);
            this.bookTitle = view.findViewById(R.id.cl_chat_item_book_title);

            view.setOnClickListener(v -> listener.onClick(v, model));
        }

        public void update(Conversation model, String peer) {
            this.model = model;
            this.bookTitle.setText("bookTitle");//TODO: set book Title
            if(!peer.equals("")) {
                this.peerId.setText(peer);
                //this.message.setText(model.getLastMessage().getText());
                //this.date.setText(model.getLastMessage().getDateTime());
            }
        }
    }
}
