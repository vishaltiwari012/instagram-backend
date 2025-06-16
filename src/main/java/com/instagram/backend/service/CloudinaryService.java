package com.instagram.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Folder where all images will be stored
    private static final String CLOUDINARY_FOLDER = "instagram/posts/";

    // Allowed image types
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    // Max size: 5 MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {
        validateFile(file);

        File tempFile = null;
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : UUID.randomUUID() + ".jpg";
        try {
            tempFile = File.createTempFile("upload-", originalFilename);
            file.transferTo(tempFile);

            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "folder", CLOUDINARY_FOLDER,
                    "resource_type", "image"
            );

            log.info("Uploading image to Cloudinary in folder '{}': {}", CLOUDINARY_FOLDER, originalFilename);
            Map<String, Object> result = cloudinary.uploader().upload(tempFile, uploadOptions);

            log.info("Upload successful. Public ID: {}, URL: {}", result.get("public_id"), result.get("secure_url"));

            if (!result.containsKey("public_id") || !result.containsKey("secure_url")) {
                throw new IOException("Cloudinary did not return expected keys.");
            }

            return result;
        } catch (IOException e) {
            log.error("Upload failed for file: {}", originalFilename, e);
            throw new IOException("Upload failed: " + originalFilename, e);
        }finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Temporary file deletion failed: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    public void deleteFile(String publicId) throws IOException {
        try {
            log.info("Deleting image from Cloudinary: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", publicId, e);
            throw e;
        }
    }

    public String getOptimizedImageUrl(String publicId) {
        int defaultWidth = 600;
        int defaultHeight = 600;

        String url = cloudinary.url()
                .transformation(new Transformation()
                        .width(defaultWidth)
                        .height(defaultHeight)
                        .crop("fill")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);

        log.debug("Generated optimized URL for {}: {}", publicId, url);
        return url;
    }


    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            log.warn("Invalid file type: {}", contentType);
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP image types are allowed.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File too large: {} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB.");
        }
    }
}
