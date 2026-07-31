package com.bank.util;

import com.bank.model.User;

/** Holds the currently authenticated user for the duration of the app session. */
public class Session {
    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void clear() { currentUser = null; }
}
