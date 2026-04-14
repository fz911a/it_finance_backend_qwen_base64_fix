package com.example.itfinance.service;

import com.example.itfinance.dto.FaceRecognizeRequest;
import com.example.itfinance.entity.FaceProfile;
import java.util.List;
import java.util.Map;

public interface FaceService {
    List<FaceProfile> list();
    FaceProfile enroll(FaceProfile faceProfile);
    Map<String, Object> recognize(FaceRecognizeRequest request);
}
