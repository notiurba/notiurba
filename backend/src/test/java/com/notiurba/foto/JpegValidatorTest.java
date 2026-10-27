package com.notiurba.foto;
import org.junit.jupiter.api.Test;
class JpegValidatorTest {
    @Test void validatesAndSanitizesSyntheticImages() throws Exception { JpegValidatorCheck.run(); }
}
