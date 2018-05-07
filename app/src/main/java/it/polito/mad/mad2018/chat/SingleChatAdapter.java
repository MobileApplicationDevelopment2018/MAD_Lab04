package it.polito.mad.mad2018.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.ObservableSnapshotArray;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Conversation;

public class SingleChatAdapter extends FirebaseRecyclerAdapter<Conversation.Message, SingleChatAdapter.ChatHolder> {

    private final OnItemClickListener onItemClickListener;
    private final OnItemCountChangedListener onItemCountChangedListener;
    private final ObservableSnapshotArray<Conversation.Message> conversation;

    SingleChatAdapter(@NonNull FirebaseRecyclerOptions<Conversation.Message> options,
                      @NonNull OnItemClickListener onItemClickListener,
                      @NonNull OnItemCountChangedListener onItemCountChangedListener) {
        super(options);
        this.conversation = options.getSnapshots();
        this.onItemClickListener = onItemClickListener;
        this.onItemCountChangedListener = onItemCountChangedListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Conversation.Message model) {
        holder.update(model);
    }

    @Override
    public int getItemViewType(int position) {
        // 0 = user_message
        // 1 = owner_message

        return(this.conversation.get(position).isRecipient())? 1 : 0;
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate((viewType == 0) ? R.layout.item_message_user : R.layout.item_message_book_owner, parent, false);
        return new SingleChatAdapter.ChatHolder(view, onItemClickListener);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        onItemCountChangedListener.onCountChangedListener(this.getItemCount());
    }

    interface OnItemClickListener {
        void onClick(View view, Conversation.Message message);
    }

    interface OnItemCountChangedListener {
        void onCountChangedListener(int count);
    }

    static class ChatHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final TextView peerId;
        private final TextView message;
        private final TextView date;

        private Conversation.Message model;

        ChatHolder(View view, @NonNull OnItemClickListener listener) {
            super(view);

            this.context = view.getContext();
            this.peerId = view.findViewById(R.id.cl_chat_item_peer);
            this.message = view.findViewById(R.id.cl_chat_item_message);
            this.date = view.findViewById(R.id.cl_chat_item_date);

            view.setOnClickListener(v -> listener.onClick(v, model));
        }

        private void update(Conversation.Message model) {

            this.model = model;

        }
    }
}
