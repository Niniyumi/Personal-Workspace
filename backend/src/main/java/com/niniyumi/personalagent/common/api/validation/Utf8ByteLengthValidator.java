package com.niniyumi.personalagent.common.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, CharSequence> {
    private int max;

    @Override
    public void initialize(Utf8ByteLength constraint) {
        max = constraint.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        // BCrypt 上限是 72 个字节，中文等多字节字符不能按 Java 字符数判断。
        return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
