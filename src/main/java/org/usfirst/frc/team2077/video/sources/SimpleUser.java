package org.usfirst.frc.team2077.video.sources;

import com.jcraft.jsch.*;

public final class SimpleUser implements UserInfo {
    private final String phrase, word;

    public SimpleUser(String passwordAndPhrase) {
        this(passwordAndPhrase, passwordAndPhrase);
    }

    public SimpleUser(String password, String passphrase) {
        this.phrase = passphrase;
        this.word = password;
    }

    @Override
    public String getPassphrase() {
        return phrase;
    }

    @Override
    public String getPassword() {
        return word;
    }

    @Override
    public boolean promptPassword(String message) {
        return true;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return true;
    }

    @Override
    public boolean promptYesNo(String message) {
        return true;
    }

    @Override
    public void showMessage(String message) {

    }
}
