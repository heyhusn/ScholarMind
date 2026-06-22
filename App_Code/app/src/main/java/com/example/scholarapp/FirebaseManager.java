package com.example.scholarapp;

import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.PaperSection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {

    private static FirebaseManager instance;
    private static final String USERS_COLLECTION = "users";
    private static final String PAPERS_SUBCOLLECTION = "papers";
    private static final String CHATS_SUBCOLLECTION = "chats";

    public final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Gson gson = new Gson();

    private FirebaseManager() {}

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public interface OnSuccessCallback {
        void onSuccess(DocumentReference documentReference);
    }

    public interface OnFetchCallback {
        void onFetch(Map<String, Object> data);
    }

    public interface OnFetchListCallback {
        void onFetch(List<Map<String, Object>> papers);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }

    public interface OnVoidSuccessCallback {
        void onSuccess();
    }

    public interface OnProfileCallback {
        void onProfile(DocumentSnapshot documentSnapshot);
    }

    public String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    private DocumentReference currentUserRef() {
        String userId = getCurrentUserId();
        return userId == null ? null : db.collection(USERS_COLLECTION).document(userId);
    }

    private CollectionReference currentUserPapersRef() {
        DocumentReference userRef = currentUserRef();
        return userRef == null ? null : userRef.collection(PAPERS_SUBCOLLECTION);
    }

    private DocumentReference currentUserPaperRef(String paperId) {
        CollectionReference papersRef = currentUserPapersRef();
        return papersRef == null ? null : papersRef.document(paperId);
    }

    private void ensureUserDocument(String userId) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("uploadCount", 0L);
        defaults.put("updatedAt", System.currentTimeMillis());
        db.collection(USERS_COLLECTION).document(userId).set(defaults, com.google.firebase.firestore.SetOptions.merge());
    }

    private void updateUploadCount(long delta) {
        DocumentReference userRef = currentUserRef();
        if (userRef == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("uploadCount", FieldValue.increment(delta));
        updates.put("updatedAt", System.currentTimeMillis());
        userRef.set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    public void getUserProfile(
            String userId,
            OnProfileCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(onSuccess::onProfile)
                .addOnFailureListener(onFailure::onFailure);
    }

    public void addPaper(
            String title,
            String author,
            String year,
            String size,
            String category,
            OnSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        String userId = getCurrentUserId();
        CollectionReference papersRef = currentUserPapersRef();
        if (userId == null || papersRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        ensureUserDocument(userId);
        Map<String, Object> paper = buildBasePaperMap(title, author, year, size, category);
        papersRef.add(paper)
                .addOnSuccessListener(ref -> {
                    updateUploadCount(1);
                    onSuccess.onSuccess(ref);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public void addPaperWithId(
            String docId,
            String title,
            String author,
            String year,
            String size,
            String category,
            PaperAnalysisResponse analysis,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        String userId = getCurrentUserId();
        DocumentReference paperRef = currentUserPaperRef(docId);
        if (userId == null || paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        ensureUserDocument(userId);
        paperRef.get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> paper = buildPaperMap(docId, title, author, year, size, category, analysis);
                    paperRef.set(paper)
                            .addOnSuccessListener(aVoid -> {
                                if (!snapshot.exists()) {
                                    updateUploadCount(1);
                                }
                                onSuccess.onSuccess();
                            })
                            .addOnFailureListener(onFailure::onFailure);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public void getPaper(
            String paperId,
            OnFetchCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        paperRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        onFailure.onFailure(new Exception("Paper not found: " + paperId));
                        return;
                    }
                    Map<String, Object> data = snapshot.getData();
                    if (data == null) {
                        onFailure.onFailure(new Exception("Paper payload missing."));
                        return;
                    }
                    data.put("id", snapshot.getId());
                    onSuccess.onFetch(data);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public static PaperAnalysisResponse parseAnalysisFromFirestoreMap(Map<String, Object> data) {
        if (data == null) return null;
        try {
            Map<String, Object> mapped = new HashMap<>();
            mapped.put("doc_id", data.get("docId"));
            mapped.put("title", data.get("title"));
            mapped.put("authors", data.get("authors") != null ? data.get("authors") : data.get("author"));
            mapped.put("year", data.get("year"));
            mapped.put("venue", data.get("venue"));
            mapped.put("field", data.get("field"));
            mapped.put("citation_count", data.get("citationCount"));
            mapped.put("citation_impact", data.get("citationImpact"));
            mapped.put("citation_score", data.get("citationScore"));
            mapped.put("ai_overview_title", data.get("aiOverviewTitle"));
            mapped.put("ai_overview_body", data.get("aiOverviewBody"));
            mapped.put("citations_list", data.get("citationsList"));
            mapped.put("sections", data.get("sections"));

            String json = new Gson().toJson(mapped);
            return new Gson().fromJson(json, PaperAnalysisResponse.class);
        } catch (Exception e) {
            android.util.Log.e("FirebaseManager", "Error parsing analysis from Firestore", e);
            return null;
        }
    }

    public void getAllPapers(
            OnFetchListCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        CollectionReference papersRef = currentUserPapersRef();
        if (papersRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        papersRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            list.add(data);
                        }
                    }
                    onSuccess.onFetch(list);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public void updatePaperStatus(
            String paperId,
            String status,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        paperRef.update(updates)
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    public void getQuizSrsQuestion(
            String paperId,
            String questionId,
            OnFetchCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        paperRef.collection("quiz_srs").document(questionId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.getData() != null) {
                        onSuccess.onFetch(snapshot.getData());
                    } else {
                        onSuccess.onFetch(new HashMap<>());
                    }
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public void saveQuizSrsQuestion(
            String paperId,
            String questionId,
            Map<String, Object> data,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        paperRef.collection("quiz_srs").document(questionId).set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }


    public void updatePaper(
            String paperId,
            Map<String, Object> updates,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        updates.put("updatedAt", System.currentTimeMillis());
        paperRef.update(updates)
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    public void saveChatMessage(
            String paperId,
            String role,
            String message,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        String userId = getCurrentUserId();
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (userId == null || paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("userId", userId);
        chatData.put("paperId", paperId);
        chatData.put("role", role);
        chatData.put("message", message);
        chatData.put("timestamp", System.currentTimeMillis());

        paperRef.collection(CHATS_SUBCOLLECTION)
                .add(chatData)
                .addOnSuccessListener(ref -> onSuccess.onSuccess())
                .addOnFailureListener(onFailure::onFailure);
    }

    public void getChatMessages(
            String paperId,
            OnFetchListCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        String userId = getCurrentUserId();
        DocumentReference paperRef = currentUserPaperRef(paperId);
        if (userId == null || paperRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        paperRef.collection(CHATS_SUBCOLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            list.add(data);
                        }
                    }
                    onSuccess.onFetch(list);
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public void deletePaper(
            String paperId,
            OnVoidSuccessCallback onSuccess,
            OnFailureCallback onFailure
    ) {
        DocumentReference paperRef = currentUserPaperRef(paperId);
        DocumentReference userRef = currentUserRef();
        if (paperRef == null || userRef == null) {
            onFailure.onFailure(new Exception("No signed-in user."));
            return;
        }

        paperRef.get()
                .addOnSuccessListener(snapshot -> paperRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            if (snapshot.exists()) {
                                updateUploadCount(-1);
                            }
                            onSuccess.onSuccess();
                        })
                        .addOnFailureListener(onFailure::onFailure))
                .addOnFailureListener(onFailure::onFailure);
    }

    private Map<String, Object> buildBasePaperMap(
            String title,
            String author,
            String year,
            String size,
            String category
    ) {
        String userId = getCurrentUserId();
        Map<String, Object> paper = new HashMap<>();
        paper.put("userId", userId);
        paper.put("title", title);
        paper.put("author", author);
        paper.put("authors", author);
        paper.put("year", year);
        paper.put("size", size);
        paper.put("category", category);
        paper.put("status", "pending");
        paper.put("createdAt", System.currentTimeMillis());
        paper.put("updatedAt", System.currentTimeMillis());
        return paper;
    }

    private Map<String, Object> buildPaperMap(
            String docId,
            String title,
            String author,
            String year,
            String size,
            String category,
            PaperAnalysisResponse analysis
    ) {
        Map<String, Object> paper = buildBasePaperMap(title, author, year, size, category);
        paper.put("docId", docId);
        paper.put("status", "done");

        if (analysis != null) {
            paper.put("venue", safeText(analysis.getVenue()));
            paper.put("field", safeText(analysis.getField()));
            paper.put("citationCount", analysis.getCitationCount());
            paper.put("citationImpact", safeText(analysis.getCitationImpact()));
            paper.put("citationScore", analysis.getCitationScore());
            paper.put("aiOverviewTitle", safeText(analysis.getAiOverviewTitle()));
            paper.put("aiOverviewBody", safeText(analysis.getAiOverviewBody()));
            paper.put("citationsList", safeText(analysis.getCitationsList()));
            paper.put("sections", sectionsToMaps(analysis.getSections()));
        } else {
            paper.put("sections", new ArrayList<>());
        }
        return paper;
    }

    private List<Map<String, Object>> sectionsToMaps(List<PaperSection> sections) {
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        List<Map<String, Object>> result = new ArrayList<>();
        if (sections == null) {
            return result;
        }
        for (PaperSection section : sections) {
            if (section == null) {
                continue;
            }
            Map<String, Object> raw = gson.fromJson(gson.toJson(section), mapType);
            result.add(raw);
        }
        return result;
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }
}
