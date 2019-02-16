package com.macrokeysclient.views;

import com.macrokeys.MacroKey;

import org.jetbrains.annotations.NotNull;

/**
 * Interfaccia associata all'evento della view {@link MacroView} della pressione di un tasto macro
 * @see MacroView
 */
public interface OnMacroKeyStroke {
    /**
     * Eseguito alla pressione di un taso di una macro
     * @param macroKey Tasto premuto
     */
     void onSendKeyStroke(@NotNull MacroKey macroKey);
}
