package com.example.mobproject.validations;

import com.example.mobproject.constants.UserType;
import com.example.mobproject.models.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserValidation {
    public static boolean isEmail(String email) {
        Pattern p = Pattern.compile("\\b[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b");
        Matcher m = p.matcher(email);

        return m.find();
    }

    public static boolean isCorrectType(int type) {
        return (type >= 0) && (type < UserType.TYPE_COUNT);
    }

    public static boolean isEmpty(User item) {
        return item.getEmail().isEmpty() || item.getName().isEmpty() || isCorrectType(item.getUserType());
    }
}