package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.InspectionPhotoResponse;
import com.cisnebranco.entity.InspectionPhoto;
import com.cisnebranco.entity.TechnicalOs;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionPhotoMapperTest {

    // toPhotoUrl is a default method — test it without a Spring context by providing
    // a stub implementation of the single abstract method toResponse
    private final InspectionPhotoMapper mapper = new InspectionPhotoMapper() {
        @Override
        public InspectionPhotoResponse toResponse(InspectionPhoto photo) {
            return null; // not under test
        }
    };

    @Test
    void toPhotoUrl_returnsPathWithoutApiPrefix() {
        TechnicalOs os = new TechnicalOs();
        os.setId(42L);

        InspectionPhoto photo = new InspectionPhoto();
        photo.setTechnicalOs(os);
        photo.setFilePath("/var/uploads/photos/42/abc123_pet.jpg");

        String url = mapper.toPhotoUrl(photo);

        assertThat(url).isEqualTo("/photos/42/abc123_pet.jpg");
        // Regression guard: double /api/api/ path must never come back
        assertThat(url).doesNotContain("/api/");
    }

    @Test
    void toPhotoUrl_extractsFilenameOnly_fromAbsoluteFilePath() {
        TechnicalOs os = new TechnicalOs();
        os.setId(7L);

        InspectionPhoto photo = new InspectionPhoto();
        photo.setTechnicalOs(os);
        photo.setFilePath("/some/deep/nested/dir/7/photo_uuid.jpg");

        assertThat(mapper.toPhotoUrl(photo)).isEqualTo("/photos/7/photo_uuid.jpg");
    }

    @Test
    void toPhotoUrl_returnsNull_whenFilePathIsNull() {
        InspectionPhoto photo = new InspectionPhoto();
        photo.setTechnicalOs(new TechnicalOs());
        photo.setFilePath(null);

        assertThat(mapper.toPhotoUrl(photo)).isNull();
    }

    @Test
    void toPhotoUrl_returnsNull_whenTechnicalOsIsNull() {
        InspectionPhoto photo = new InspectionPhoto();
        photo.setTechnicalOs(null);
        photo.setFilePath("/var/uploads/photos/1/file.jpg");

        assertThat(mapper.toPhotoUrl(photo)).isNull();
    }
}
