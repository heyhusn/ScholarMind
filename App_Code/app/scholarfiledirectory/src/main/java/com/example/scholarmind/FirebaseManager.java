package com.example.scholarmind;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseManager — Singleton wrapper for all Firestore CRUD operations
 * used in ScholarMind.
 *
 * Collection structure:
 *   papers/{paperId}
 *     - title        : String
 *     - author       : String
 *     - year         : String
 *     - size         : String
 *     - category     : String
 *     - status       : String  ("pending" | "processing" | "done" | "error")
 *     - createdAt    : long    (System.currentTimeMillis())
 */
public class FirebaseManager {

    // ─── Singleton ────────────────────────────────────────────────────────────

    private static FirebaseManager instance;

    private FirebaseManager() {}

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ─── Firestore reference ──────────────────────────────────────────────────

    public final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference papersRef = db.collection("papers");

    // ─── Callback interfaces ──────────────────────────────────────────────────

    /** Called with the new DocumentReference on success. */
    public interface OnSuccessCallback {
        void onSuccess(DocumentReference documentReference);
    }

    /** Called with the paper data map on success. */
    public interface OnFetchCallback {
        void onFetch(Map<String, Object> data);
    }

    /** Called with a list of paper maps on success. */
    public interface OnFetchListCallback {
        void onFetch(List<Map<String, Object>> papers);
    }

    /** Called with an Exception on failure. */
    public interface OnFailureCallback {
        void onFailure(Exception e);
    }

    /** Called when a void operation (update / delete) succeeds. */
    public interface OnVoidSuccessCallback {
        void onSuccess();
    }

    /** Called with a Firestore DocumentSnapshot (used for user profiles). */
    public interface OnProfileCallback {
        void onProfile(DocumentSnapshot documentSnapshot);
    }

    // ─── AUTH HELPERS ─────────────────────────────────────────────────────────

    /**
     * Returns the UID of the currently signed-in Firebase user, or null if none.
     * Used in HomeActivity to personalise the greeting.
     */
    public String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    /**
     * Fetches a user's profile document from the "users" collection.
     *
     * Usage (HomeActivity):
     *   FirebaseManager.getInstance().getUserProfile(userId,
     *       snapshot -> { String name = snapshot.getString("fullName"); },
     *       e        -> { });
     */
    public void getUserProfile(
            String userId,
            OnProfileCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(onSuccess::onProfile)
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Add a new paper document to Firestore.
     * Matches the call in UploadActivity:
     *   FirebaseManager.getInstance().addPaper(title, author, year, size, category,
     *                                          onSuccess, onFailure);
     */
    public void addPaper(
            String title,
            String author,
            String year,
            String size,
            String category,
            OnSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        Map<String, Object> paper = new HashMap<>();
        paper.put("title",     title);
        paper.put("author",    author);
        paper.put("year",      year);
        paper.put("size",      size);
        paper.put("category",  category);
        paper.put("status",    "pending");
        paper.put("createdAt", System.currentTimeMillis());

        papersRef
                .add(paper)
                .addOnSuccessListener(onSuccess::onSuccess)
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── READ (single) ────────────────────────────────────────────────────────

    /**
     * Fetch a single paper by its Firestore document ID.
     *
     * Usage:
     *   FirebaseManager.getInstance().getPaper(paperId,
     *       data -> { String title = (String) data.get("title"); },
     *       e    -> { Log.e("TAG", e.getMessage()); });
     */
    public void getPaper(
            String paperId,
            OnFetchCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        papersRef
                .document(paperId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onFetch(snapshot.getData());
                    } else {
                        onFailure.onFailure(new Exception("Paper not found: " + paperId));
                    }
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── READ (all) ───────────────────────────────────────────────────────────

    /**
     * Fetch all papers, ordered by creation date (newest first).
     *
     * Usage:
     *   FirebaseManager.getInstance().getAllPapers(
     *       papers -> recyclerAdapter.submitList(papers),
     *       e      -> Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_SHORT).show());
     */
    public void getAllPapers(
            OnFetchListCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        papersRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId()); // inject document ID
                            list.add(data);
                        }
                    }
                    onSuccess.onFetch(list);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── UPDATE (status) ──────────────────────────────────────────────────────

    /**
     * Update the processing status of a paper.
     * Allowed values: "pending" | "processing" | "done" | "error"
     *
     * Usage (e.g. in ProcessingActivity):
     *   FirebaseManager.getInstance().updatePaperStatus(paperId, "done",
     *       ()  -> { /* navigate next *\/ },
     *       e   -> { Toast… });
     */
    public void updatePaperStatus(
            String paperId,
            String status,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        papersRef
                .document(paperId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    /**
     * Update one or more arbitrary fields of a paper document.
     *
     * Usage:
     *   Map<String, Object> updates = new HashMap<>();
     *   updates.put("title", "New Title");
     *   updates.put("category", "Review");
     *   FirebaseManager.getInstance().updatePaper(paperId, updates, () -> {}, e -> {});
     */
    public void updatePaper(
            String paperId,
            Map<String, Object> updates,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        papersRef
                .document(paperId)
                .update(updates)
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── CHAT (sub-collection) ────────────────────────────────────────────────

    /**
     * Save a single chat message under papers/{paperId}/chats
     *
     * Usage (ChatModeActivity):
     *   FirebaseManager.getInstance().saveChatMessage(paperId, "user", "What is this about?",
     *       () -> {},
     *       e  -> Log.e("TAG", e.getMessage()));
     */
    public void saveChatMessage(
            String paperId,
            String role,
            String message,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("role",      role);
        chatData.put("message",   message);
        chatData.put("timestamp", System.currentTimeMillis());

        db.collection("papers")
                .document(paperId)
                .collection("chats")
                .add(chatData)
                .addOnSuccessListener(ref -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    /**
     * Load all chat messages for a paper, ordered oldest → newest.
     *
     * Usage (ChatModeActivity):
     *   FirebaseManager.getInstance().getChatMessages(paperId,
     *       messages -> { for (Map<String,Object> m : messages) { ... } },
     *       e        -> Log.e("TAG", e.getMessage()));
     */
    public void getChatMessages(
            String paperId,
            OnFetchListCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        db.collection("papers")
                .document(paperId)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) list.add(data);
                    }
                    onSuccess.onFetch(list);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    /**
     * Permanently delete a paper document from Firestore.
     *
     * Usage:
     *   FirebaseManager.getInstance().deletePaper(paperId,
     *       ()  -> Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show(),
     *       e   -> Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_SHORT).show());
     */
    public void deletePaper(
            String paperId,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        papersRef
                .document(paperId)
                .delete()
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }
}