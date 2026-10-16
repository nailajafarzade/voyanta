package com.voyanta.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    void sha256IsStableAndNotTheRawValue() {
        String hash = HashUtil.sha256("127.0.0.1");
        assertThat(hash).isEqualTo(HashUtil.sha256("127.0.0.1"));
        assertThat(hash).isNotEqualTo("127.0.0.1");
        assertThat(hash).isNotBlank();
    }
}
