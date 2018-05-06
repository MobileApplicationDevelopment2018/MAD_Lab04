package it.polito.mad.mad2018.data;

import android.support.annotation.NonNull;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import it.polito.mad.mad2018.MAD2018Application;
import it.polito.mad.mad2018.utils.Utilities;

public class Conversation implements Serializable {

    public static final String CONVERSATION_KEY = "conversation_key";

    private static final String FIREBASE_CONVERSATIONS_KEY = "conversations";
    private static final String FIREBASE_MESSAGES_KEY = "messages";

    private final UserProfile localUser;
    private final String conversationId;
    private final Conversation.Data data;

    public Conversation(@NonNull Book book) {
        this.localUser = UserProfile.localInstance;
        this.conversationId = Conversation.generateConversationId();

        this.data = new Data();
        this.data.bookId = book.getBookId();
        this.data.owner = book.getOwnerId();
        this.data.peer = localUser.getUserId();
    }

    private Conversation(@NonNull String conversationId,
                         @NonNull Data data) {
        this.localUser = UserProfile.localInstance;
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
                .setIndexedQuery(keyQuery, dataRef,
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

    public FirebaseRecyclerOptions<Message> getMessages() {
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(conversationId)
                .child(FIREBASE_MESSAGES_KEY);

        return new FirebaseRecyclerOptions.Builder<Conversation.Message>()
                .setQuery(dataRef,
                        snapshot -> {
                            Conversation.Data.Message data = snapshot.getValue(Conversation.Data.Message.class);
                            assert data != null;
                            return new Conversation.Message(data, localUser.getUserId());
                        })
                .build();
    }

    public boolean isBookOwner() {
        return Utilities.equals(localUser.getUserId(), this.data.owner);
    }

    public String getPeerUserId() {
        return isBookOwner() ? this.data.peer : this.data.owner;
    }

    public int getUnreadMessagesCount() {
        return localUser.getUnreadMessagesCount(conversationId);
    }

    public void setMessagesAllRead() {
        localUser.setMessagesAllRead(conversationId);
    }

    public Message getLastMessage() {
        return this.data.messages.size() == 0
                ? null
                : new Message(this.data.messages.lastEntry().getValue(), localUser.getUserId());
    }

    public Task<?> sendMessage(@NonNull String text) {
        Data.Message message = new Data.Message();
        message.recipient = getPeerUserId();
        message.text = text;
        message.timestamp = System.currentTimeMillis();

        List<Task<?>> tasks = new ArrayList<>();
        DatabaseReference conversationReference = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_CONVERSATIONS_KEY)
                .child(this.conversationId);

        if (this.data.messages.size() == 0) {
            tasks.add(conversationReference.setValue(this.data));
            tasks.add(localUser.addConversation(this.conversationId));
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

            Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
            calendar.setTimeInMillis(message.timestamp);

            return dateFormat.format(calendar) + " " + timeFormat.format(calendar);
        }
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public static class Data implements Serializable {

        public String bookId;
        public String owner;
        public String peer;
        public Conversation.Data.Flags flags;

        public NavigableMap<String, Message> messages;

        public Data() {
            this.bookId = null;
            this.owner = null;
            this.peer = null;
            this.flags = new Conversation.Data.Flags();
            this.messages = new TreeMap<>();
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
            public long timestamp;

            public Message() {
                this.recipient = null;
                this.text = null;
                this.timestamp = 0;
            }
        }
    }
}
