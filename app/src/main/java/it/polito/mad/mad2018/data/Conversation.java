package it.polito.mad.mad2018.data;

import android.support.annotation.NonNull;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.mad2018.MAD2018Application;
import it.polito.mad.mad2018.utils.Utilities;

public class Conversation implements Serializable {

    public static final String CONVERSATION_KEY = "conversation_key";
    public static final String CONVERSATION_ID_KEY = "conversation_id_tkey";

    private static final String FIREBASE_CONVERSATIONS_KEY = "conversations";
    private static final String FIREBASE_MESSAGES_KEY = "messages";

    private static final String FIREBASE_CONVERSATION_ORDER_BY_KEY = "timestamp";

    private static UserProfile localUser;

    public String getConversationId() {
        return conversationId;
    }

    private final String conversationId;
    private final Conversation.Data data;

    public Conversation(@NonNull Book book) {
        Conversation.localUser = UserProfile.localInstance;
        this.conversationId = Conversation.generateConversationId();

        this.data = new Data();
        this.data.bookId = book.getBookId();
        this.data.owner = book.getOwnerId();
        this.data.peer = Conversation.localUser.getUserId();
    }

    public Conversation(@NonNull String conversationId,
                        @NonNull Data data) {
        Conversation.localUser = UserProfile.localInstance;
        this.conversationId = conversationId;
        this.data = data;
    }

    private static String generateConversationId() {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY).push().getKey();
    }

    private static FirebaseRecyclerOptions<Conversation> getConversations(
            @NonNull DatabaseReference keyQuery) {

        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY);

        return new FirebaseRecyclerOptions.Builder<Conversation>()
                .setIndexedQuery(keyQuery.orderByChild(FIREBASE_CONVERSATION_ORDER_BY_KEY), dataRef,
                        snapshot -> {
                            String conversationId = snapshot.getKey();
                            Conversation.Data data = snapshot.getValue(Conversation.Data.class);
                            assert data != null;
                            return new Conversation(conversationId, data);
                        })
                .build();
    }

    public static FirebaseRecyclerOptions<Conversation> getActiveConversations() {
        return Conversation.getConversations(UserProfile.getActiveConversationsReference());
    }

    public static FirebaseRecyclerOptions<Conversation> getArchivedConversations() {
        return Conversation.getConversations(UserProfile.getArchivedConversationsReference());
    }

    public static ValueEventListener setOnConversationLoadedListener(@NonNull String conversationId,
                                                                     @NonNull ValueEventListener listener) {

        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(conversationId)
                .addValueEventListener(listener);
    }

    public static void unsetOnConversationLoadedListener(@NonNull String conversationId,
                                                         @NonNull ValueEventListener listener) {

        FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(conversationId)
                .removeEventListener(listener);
    }

    public static FirebaseRecyclerOptions<Message> getMessages(String conversationId) {
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(conversationId)
                .child(FIREBASE_MESSAGES_KEY);

        return new FirebaseRecyclerOptions.Builder<Conversation.Message>()
                .setQuery(dataRef,
                        snapshot -> {
                            Conversation.Data.Message data = snapshot.getValue(Conversation.Data.Message.class);
                            assert data != null;
                            if (Conversation.localUser == null)
                                localUser = UserProfile.localInstance;
                            return new Conversation.Message(data, Conversation.localUser.getUserId());
                        })
                .build();
    }

    public FirebaseRecyclerOptions<Message> getMessages() {
        return Conversation.getMessages(this.conversationId);
    }

    public boolean isBookOwner() {
        return Utilities.equals(Conversation.localUser.getUserId(), this.data.owner);
    }

    public String getBookOwnerId() {
        return this.data.owner;
    }

    public String getPeerUserId() {
        return isBookOwner() ? this.data.peer : this.data.owner;
    }

    public String getBookId() {
        return this.data.bookId;
    }

    public int getUnreadMessagesCount() {
        return Conversation.localUser.getUnreadMessagesCount(conversationId);
    }

    public void setMessagesAllRead() {
        Conversation.localUser.setMessagesAllRead(conversationId);
    }

    public Message getLastMessage() {
        if (this.data.messages.size() == 0) {
            return null;
        }

        String last = Collections.max(this.data.messages.keySet());
        return new Message(this.data.messages.get(last), Conversation.localUser.getUserId());
    }

    public Task<?> sendMessage(@NonNull String text) {
        Data.Message message = new Data.Message();
        message.recipient = getPeerUserId();
        message.text = text;

        List<Task<?>> tasks = new ArrayList<>();
        DatabaseReference conversationReference = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(this.conversationId);

        if (this.data.messages.size() == 0) {
            tasks.add(conversationReference.setValue(this.data));
            tasks.add(Conversation.localUser.addConversation(this.conversationId, this.data.bookId));
        }

        DatabaseReference messageReference = conversationReference
                .child(FIREBASE_MESSAGES_KEY).push();
        tasks.add(messageReference.setValue(message));

        this.data.messages.put(messageReference.getKey(), message);

        return Tasks.whenAllSuccess(tasks);
    }


    public static class Message implements Serializable {

        private final String localUserId;
        private final Conversation.Data.Message message;

        private Message(@NonNull Conversation.Data.Message message,
                        @NonNull String localUserId) {

            this.localUserId = localUserId;
            this.message = message;
        }

        public boolean isRecipient() {
            return Utilities.equals(message.recipient, localUserId);
        }

        public String getText() {
            return message.text;
        }

        public String getDateTime() {
            // TODO: improve the style (e.g. use today, yesterday and so on)
            // The best approach depends on how it will be displayed

            DateFormat dateFormat = android.text.format.DateFormat.
                    getDateFormat(MAD2018Application.applicationContext);
            DateFormat timeFormat = android.text.format.DateFormat.
                    getTimeFormat(MAD2018Application.applicationContext);

            Date date = new Date(message.getTimestamp());
            return dateFormat.format(date) + " " + timeFormat.format(date);
        }
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public static class Data implements Serializable {

        public String bookId;
        public String owner;
        public String peer;
        public Conversation.Data.Flags flags;

        public Map<String, Message> messages;

        public Data() {
            this.bookId = null;
            this.owner = null;
            this.peer = null;
            this.flags = new Conversation.Data.Flags();
            this.messages = new HashMap<>();
        }

        private static class Flags implements Serializable {
            public boolean archived;

            public Flags() {
                this.archived = false;
            }
        }

        private static class Message implements Serializable {
            public String recipient;
            public String text;
            public Object timestamp;

            public Message() {
                this.recipient = null;
                this.text = null;
                this.timestamp = ServerValue.TIMESTAMP;
            }

            @Exclude
            private long getTimestamp() {
                return this.timestamp instanceof Long
                        ? (long) this.timestamp
                        : System.currentTimeMillis();
            }
        }
    }
}
