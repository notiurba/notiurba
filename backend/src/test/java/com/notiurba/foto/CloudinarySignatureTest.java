package com.notiurba.foto;
import org.junit.jupiter.api.Test;
class CloudinarySignatureTest {
    @Test void signaturesUseSortedSignableParameters() { CloudinarySignatureCheck.run(); }
}
