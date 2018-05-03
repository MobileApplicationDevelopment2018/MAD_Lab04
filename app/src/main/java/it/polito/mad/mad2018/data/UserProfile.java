package it.polito.mad.mad2018.data;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.algolia.search.saas.CompletionHandler;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.mad2018.MAD2018Application;
import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.utils.PictureUtilities;
import it.polito.mad.mad2018.utils.Utilities;

public class UserProfile implements Serializable {

    public static final String PROFILE_INFO_KEY = "profile_info_key";

    private static final String FIREBASE_USERS_KEY = "users";
    private static final String FIREBASE_BOOKS_KEY = "books";
    private static final String FIREBASE_OWNED_BOOKS_KEY = "ownedBooks";

    private static final String FIREBASE_PROFILE_KEY = "profile";
    private static final String FIREBASE_STORAGE_USERS_FOLDER = "users";
    private static final String FIREBASE_STORAGE_IMAGE_NAME = "profile";

    private static final int PROFILE_PICTURE_SIZE = 1024;
    private static final int PROFILE_PICTURE_THUMBNAIL_SIZE = 64;
    private static final int PROFILE_PICTURE_QUALITY = 50;

    public static UserProfile localInstance;

    private final String uid;
    private final Data data;
    private boolean localImageToBeDeleted;
    private String localImagePath;

    public UserProfile(@NonNull Data data, @NonNull Resources resources) {
        this(getCurrentUserId(), data, resources);
    }

    public UserProfile(@NonNull String uid, @NonNull Data data, @NonNull Resources resources) {
        this.uid = uid;
        this.data = data;
        this.localImageToBeDeleted = false;
        this.localImagePath = null;
        trimFields(resources);
    }

    public UserProfile(@NonNull UserProfile other) {
        this.uid = other.uid;
        this.data = new Data(other.data);
        this.localImageToBeDeleted = false;
        this.localImagePath = null;
    }

    public UserProfile(@NonNull FirebaseUser user) {
        this.uid = user.getUid();
        this.data = new Data();
        this.localImageToBeDeleted = false;
        this.localImagePath = null;

        this.data.profile.email = user.getEmail();
        this.data.profile.username = user.getDisplayName();

        for (UserInfo profile : user.getProviderData()) {
            if (this.data.profile.username == null && profile.getDisplayName() != null) {
                this.data.profile.username = profile.getDisplayName();
            }
        }

        if (this.data.profile.username == null) {
            this.data.profile.username = getUsernameFromEmail(this.data.profile.email);

        }

        this.data.profile.location.latitude = 45.116177;
        this.data.profile.location.longitude = 7.742615;
        this.data.profile.location.name = MAD2018Application.applicationContext.getString(R.string.default_city_turin);
    }

    private static String getUsernameFromEmail(@NonNull String email) {
        return email.substring(0, email.indexOf('@'));
    }

    public static String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        return currentUser.getUid();
    }

    public static ValueEventListener setOnProfileLoadedListener(@NonNull ValueEventListener listener) {
        return setOnProfileLoadedListener(getCurrentUserId(), listener);
    }

    public static ValueEventListener setOnProfileLoadedListener(@NonNull String userId,
                                                                @NonNull ValueEventListener listener) {

        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(userId)
                .addValueEventListener(listener);
    }

    public static void unsetOnProfileLoadedListener(@NonNull ValueEventListener listener) {
        unsetOnProfileLoadedListener(getCurrentUserId(), listener);
    }

    public static void unsetOnProfileLoadedListener(@NonNull String userId,
                                                    @NonNull ValueEventListener listener) {

        FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(userId)
                .removeEventListener(listener);
    }

    static DatabaseReference getOwnedBooksReference(@NonNull String userId) {
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(userId)
                .child(FIREBASE_BOOKS_KEY)
                .child(FIREBASE_OWNED_BOOKS_KEY);
    }

    static StorageReference getStorageFolderReference(@NonNull String userId) {
        return FirebaseStorage.getInstance().getReference()
                .child(FIREBASE_STORAGE_USERS_FOLDER)
                .child(userId);
    }

    public static boolean isLocal(String uid) {
        return Utilities.equals(uid, UserProfile.getCurrentUserId());
    }

    public void setProfilePicture(String path, boolean toBeDeleted) {
        this.data.profile.hasProfilePicture = path != null;
        this.data.profile.profilePictureLastModified = System.currentTimeMillis();
        this.data.profile.profilePictureThumbnail = null;
        localImageToBeDeleted = toBeDeleted;
        localImagePath = path;
    }

    public void resetProfilePicture() {
        setProfilePicture(null, false);
    }

    public void update(@NonNull String username, @NonNull String biography) {
        this.data.profile.username = username;
        this.data.profile.biography = biography;
    }

    public void update(Place place) {
        this.data.profile.location = place == null
                ? new Data.Location()
                : new Data.Location(place);
    }

    private void trimFields(@NonNull Resources resources) {
        this.data.profile.username = Utilities.trimString(this.data.profile.username, resources.getInteger(R.integer.max_length_username));
        this.data.profile.biography = Utilities.trimString(this.data.profile.biography, resources.getInteger(R.integer.max_length_biography));
    }

    public String getUserId() {
        return this.uid;
    }

    public String getEmail() {
        return this.data.profile.email;
    }

    public String getUsername() {
        return this.data.profile.username;
    }

    public String getLocation() {
        return this.data.profile.location.name;
    }

    public double[] getCoordinates() {
        return new double[]{this.data.profile.location.latitude, this.data.profile.location.longitude};
    }

    JSONObject getLocationAlgolia() {
        return this.data.profile.location.toAlgoliaGeoLoc();
    }

    public String getLocationOrDefault() {
        return getLocation() == null
                ? MAD2018Application.applicationContext.getString(R.string.default_city)
                : getLocation();

    }

    public String getBiography() {
        return this.data.profile.biography;
    }

    public boolean hasProfilePicture() {
        return this.data.profile.hasProfilePicture;
    }

    public long getProfilePictureLastModified() {
        return this.data.profile.profilePictureLastModified;
    }

    public Object getProfilePictureReference() {
        return this.localImagePath == null
                ? this.hasProfilePicture()
                ? this.getProfilePictureReferenceFirebase()
                : null
                : this.getLocalImagePath();
    }

    public byte[] getProfilePictureThumbnail() {
        return this.data.profile.profilePictureThumbnail == null
                ? null
                : Base64.decode(this.data.profile.profilePictureThumbnail, Base64.DEFAULT);
    }

    public void setProfilePictureThumbnail(ByteArrayOutputStream thumbnail) {
        this.data.profile.profilePictureThumbnail =
                Base64.encodeToString(thumbnail.toByteArray(), Base64.DEFAULT);
    }

    private StorageReference getProfilePictureReferenceFirebase() {
        return UserProfile.getStorageFolderReference(this.uid)
                .child(FIREBASE_STORAGE_IMAGE_NAME);
    }

    public String getLocalImagePath() {
        return this.localImagePath;
    }

    public boolean isLocalImageToBeDeleted() {
        return localImageToBeDeleted;
    }

    public boolean profileUpdated(UserProfile other) {
        return !Utilities.equals(this.getEmail(), other.getEmail()) ||
                !Utilities.equals(this.getUsername(), other.getUsername()) ||
                !Utilities.equals(this.getLocation(), other.getLocation()) ||
                !Utilities.equalsNullOrWhiteSpace(this.getBiography(), other.getBiography()) ||
                imageUpdated(other);
    }

    public boolean imageUpdated(UserProfile other) {
        return this.hasProfilePicture() != other.hasProfilePicture() ||
                !Utilities.equals(this.localImagePath, other.localImagePath);
    }

    public float getRating() {
        return this.data.statistics.rating;
    }

    public int getOwnedBooksCount() {
        return this.data.books.ownedBooks.size();
    }

    public int getLentBooksCount() {
        return this.data.statistics.lentBooks;
    }

    public int getBorrowedBooksCount() {
        return this.data.statistics.borrowedBooks;
    }

    public int getToBeReturnedBooksCount() {
        return this.data.statistics.toBeReturnedBooks;
    }

    public boolean isLocal() {
        return UserProfile.isLocal(this.uid);
    }

    public Task<Void> saveToFirebase(@NonNull Resources resources) {
        this.trimFields(resources);

        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(this.uid)
                .child(FIREBASE_PROFILE_KEY)
                .setValue(this.data.profile);
    }

    public void deleteProfilePictureFromFirebase() {
        getProfilePictureReferenceFirebase().delete();
    }

    public AsyncTask<Void, Void, PictureUtilities.CompressedImage> processProfilePictureAsync(
            @NonNull PictureUtilities.CompressImageAsync.OnCompleteListener onCompleteListener) {

        return new PictureUtilities.CompressImageAsync(
                localImagePath, PROFILE_PICTURE_SIZE, PROFILE_PICTURE_THUMBNAIL_SIZE,
                PROFILE_PICTURE_QUALITY, onCompleteListener)
                .execute();
    }

    public Task<?> uploadProfilePictureToFirebase(@NonNull ByteArrayOutputStream picture) {
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(PictureUtilities.IMAGE_CONTENT_TYPE_UPLOAD)
                .build();

        return getProfilePictureReferenceFirebase()
                .putBytes(picture.toByteArray(), metadata);
    }

    public void postCommit() {
        this.localImageToBeDeleted = false;
        this.localImagePath = null;
    }

    public Task<?> addBook(String bookId) {
        this.data.books.ownedBooks.put(bookId, true);
        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(getCurrentUserId())
                .child(FIREBASE_BOOKS_KEY)
                .child(FIREBASE_OWNED_BOOKS_KEY)
                .child(bookId)
                .setValue(true);
    }

    public void removeBook(String bookId) {
        this.data.books.ownedBooks.remove(bookId);
        FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(getCurrentUserId())
                .child(FIREBASE_BOOKS_KEY)
                .child(FIREBASE_OWNED_BOOKS_KEY)
                .child(bookId)
                .removeValue();
    }


    public void updateAlgoliaGeoLoc(UserProfile other, @NonNull CompletionHandler completionHandler) {

        if ((other != null && Utilities.equals(this.data.profile.location, other.data.profile.location)) ||
                this.data.books.ownedBooks.size() == 0) {
            completionHandler.requestCompleted(null, null);
            return;
        }

        JSONObject geoloc = this.getLocationAlgolia();
        List<JSONObject> bookUpdates = new ArrayList<>();

        for (String bookId : this.data.books.ownedBooks.keySet()) {
            try {
                bookUpdates.add(new JSONObject()
                        .put(Book.ALGOLIA_GEOLOC_KEY, geoloc)
                        .put(Book.ALGOLIA_BOOK_ID_KEY, bookId));
            } catch (JSONException e) { /* Do nothing */ }
        }

        Book.AlgoliaBookIndex.getInstance()
                .partialUpdateObjectsAsync(new JSONArray(bookUpdates), completionHandler);
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public static class Data implements Serializable {

        public Profile profile;
        public Statistics statistics;
        public Books books;

        public Data() {
            this.profile = new Profile();
            this.statistics = new Statistics();
            this.books = new Books();
        }

        public Data(@NonNull Data other) {
            this.profile = new Profile(other.profile);
            this.statistics = new Statistics(other.statistics);
            this.books = new Books(other.books);
        }

        private static class Profile implements Serializable {

            public String email;
            public String username;
            public Location location;
            public String biography;
            public boolean hasProfilePicture;
            public long profilePictureLastModified;
            public String profilePictureThumbnail;

            public Profile() {
                this.email = null;
                this.username = null;
                this.location = new Location();
                this.biography = null;
                this.hasProfilePicture = false;
                this.profilePictureLastModified = 0;
                this.profilePictureThumbnail = null;
            }

            public Profile(@NonNull Profile other) {
                this.email = other.email;
                this.username = other.username;
                this.location = new Location(other.location);
                this.biography = other.biography;
                this.hasProfilePicture = other.hasProfilePicture;
                this.profilePictureLastModified = other.profilePictureLastModified;
                this.profilePictureThumbnail = other.profilePictureThumbnail;
            }
        }

        private static class Statistics implements Serializable {
            public float rating;
            public int lentBooks;
            public int borrowedBooks;
            public int toBeReturnedBooks;

            public Statistics() {
                this.rating = 0;
                this.lentBooks = 0;
                this.borrowedBooks = 0;
                this.toBeReturnedBooks = 0;
            }

            public Statistics(@NonNull Statistics other) {
                this.rating = other.rating;
                this.lentBooks = other.lentBooks;
                this.borrowedBooks = other.borrowedBooks;
                this.toBeReturnedBooks = other.toBeReturnedBooks;
            }
        }

        private static class Books implements Serializable {
            public Map<String, Boolean> ownedBooks;

            public Books() {
                this.ownedBooks = new HashMap<>();
            }

            public Books(@NonNull Books other) {
                this.ownedBooks = new HashMap<>(other.ownedBooks);
            }
        }

        private static class Location implements Serializable {
            public String name;
            public double latitude;
            public double longitude;

            public Location() { /* Required by Firebase */ }

            public Location(Location other) {
                this.name = other.name;
                this.latitude = other.latitude;
                this.longitude = other.longitude;
            }

            public Location(@NonNull Place place) {
                this.name = place.getName().toString();
                this.latitude = place.getLatLng().latitude;
                this.longitude = place.getLatLng().longitude;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                }

                if (!(other instanceof Location)) {
                    return false;
                }

                Location otherL = (Location) other;
                return this.name.equals(otherL.name) &&
                        Double.compare(this.latitude, otherL.latitude) == 0 &&
                        Double.compare(this.longitude, otherL.longitude) == 0;
            }

            private JSONObject toAlgoliaGeoLoc() {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("lat", this.latitude);
                    jsonObject.put("lon", this.longitude);
                    return jsonObject;
                } catch (JSONException e) {
                    return null;
                }

            }
        }
    }
}
