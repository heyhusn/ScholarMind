package com.example.scholarmind.network;

import com.example.scholarmind.models.*;
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
    Call<PaperAnalysisResponse> analyzePdf(@Part okhttp3.MultipartBody.Part file);

    /** Retrieve a cached analysis by doc_id */
    @GET("api/pdf/analysis/{doc_id}")
    Call<PaperAnalysisResponse> getAnalysis(@Path("doc_id") String docId);

    @POST("api/chat/{mode}")
    Call<TextResponse> sendChatMessage(@Path("mode") String mode, @Body ChatRequest request);
}