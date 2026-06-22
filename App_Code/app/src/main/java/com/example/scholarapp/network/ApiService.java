package com.example.scholarapp.network;

import com.example.scholarapp.models.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/v1/auth/signup")
    Call<AuthResponse> signup(@Body SignupRequest request);

    @POST("api/v1/auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @Multipart
    @POST("api/pdf/extract")
    Call<PdfExtractResponse> uploadPdf(@Part okhttp3.MultipartBody.Part file);

    /** Full AI analysis — used by HomeFragment */
    @Multipart
    @POST("api/pdf/analyze")
    Call<PaperAnalysisResponse> analyzePdf(
            @Part okhttp3.MultipartBody.Part file,
            @Part("user_id") okhttp3.RequestBody userId
    );

    /** Retrieve a cached analysis by doc_id */
    @GET("api/pdf/analysis/{doc_id}")
    Call<PaperAnalysisResponse> getAnalysis(@Path("doc_id") String docId, @Query("user_id") String userId);

    @POST("api/chat/{mode}")
    Call<TextResponse> sendChatMessage(@Path("mode") String mode, @Body ChatRequest request);

    @POST("api/podcast/generate")
    Call<TextResponse> generatePodcast(@Body DocumentRequest request);

    @POST("api/summary/generate")
    Call<TextResponse> generateSummary(@Body DocumentRequest request);

    @POST("api/text/simplify")
    Call<TextResponse> simplifyText(@Body SimplifyRequest request);

    @POST("api/quiz/generate")
    Call<QuizResponse> generateQuiz(@Body DocumentRequest request);

    @GET("api/papers/search")
    Call<OpenAlexSearchResponse> searchPapers(
            @Query("query") String query,
            @Query("page") int page,
            @Query("filter") String filter
    );

    @GET("api/papers/trending")
    Call<OpenAlexSearchResponse> getTrendingPapers();

    @GET("api/papers/{paper_id}")
    Call<OpenAlexPaper> getPaperDetails(@Path("paper_id") String paperId);

    @POST("api/papers/insights")
    Call<PaperInsightsResponse> getPaperInsights(@Body PaperInsightsRequest request);

    @Multipart
    @POST("api/peer-review/analyze")
    Call<PeerReviewResponse> analyzePeerReview(
            @Part okhttp3.MultipartBody.Part file
    );

    @Multipart
    @POST("api/references/generate")
    Call<ReferencesResponse> generateReferences(
            @Part okhttp3.MultipartBody.Part file,
            @Part("style") okhttp3.RequestBody style
    );

    @POST("api/references/export/docx")
    Call<okhttp3.ResponseBody> downloadReferencesDocx(
            @Body ExportReferencesRequest request
    );

    @POST("api/references/export/pdf")
    Call<okhttp3.ResponseBody> downloadReferencesPdf(
            @Body ExportReferencesRequest request
    );
}



