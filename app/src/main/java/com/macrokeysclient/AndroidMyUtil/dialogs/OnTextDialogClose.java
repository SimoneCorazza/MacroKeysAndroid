package com.macrokeysclient.AndroidMyUtil.dialogs;

import org.jetbrains.annotations.NotNull;

/**
 * Associata all'evento di chiusura di una dialog avente un campo di testo
 */
public interface OnTextDialogClose {
    /**
     * Testo digitato fino a quel momento nella text box della dialog
     * @param text Testo scritto nella text box
     */
    void onClose(@NotNull String text);
}
