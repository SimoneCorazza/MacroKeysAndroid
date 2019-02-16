package com.macrokeysclient.AndroidMyUtil.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

/**
 * Classe statica che racchiude metodi per la semplificazione della creazione di finestre per
 * Android
 */
public final class Dialogs {

    private Dialogs() { }

    /**
     * Mostra una dialog per l'inserimento dei testo
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param hint Placeholder da mostrare nella text box
     * @param inputType Topologia di input usare {@link InputType}
     * @param confirm Evento scaturito quando l'utente conferma
     * @param cancel Evento scaturito quando l'utente cancella l'operazione
     * @see InputType
     */
    public static void text(@NotNull Context context, boolean cancelable, String hint,
                            int inputType, final @NotNull OnTextDialogClose confirm,
                            final OnTextDialogClose cancel) {
        final EditText passwordText = new EditText(context);
        passwordText.setInputType(inputType);
        passwordText.setHint(hint);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(passwordText);


        alertDialogBuilder.setCancelable(cancelable);
        alertDialogBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                confirm.onClose(passwordText.getText().toString());
            }
        });
        alertDialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(cancel != null) {
                    cancel.onClose(passwordText.getText().toString());
                }
            }
        });
        //Per mostrare la tastiera da subito:
        AlertDialog passwDialog = alertDialogBuilder.create();
        passwDialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        passwDialog.show();
    }

    /**
     * Mostra una dialog per l'inserimento dei testo
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param hint Placeholder da mostrare nella textt box
     * @param confirm Evento scaturito quando l'utente conferma
     * @param cancel Evento scaturito quando l'utente cancella l'operazione
     */
    public static void text(@NotNull Context context, boolean cancelable, String hint,
                            final @NotNull OnTextDialogClose confirm,
                            final OnTextDialogClose cancel) {
        text(context, cancelable, hint, InputType.TYPE_NULL, confirm, cancel);
    }

    /**
     * Mostra una dialog per l'inserimento dei testo
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param hint Placeholder da mostrare nella textt box
     * @param inputType Topologia di input usare {@link InputType}
     * @param confirm Evento scaturito quando l'utente conferma
     * @see InputType
     */
    public static void text(@NotNull Context context, boolean cancelable, String hint,
                            int inputType, final @NotNull OnTextDialogClose confirm) {
        text(context, cancelable, hint, inputType, confirm, null);
    }

    /**
     * Mostra una dialog per l'inserimento dei testo
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param hint Placeholder da mostrare nella textt box
     * @param confirm Evento scaturito quando l'utente conferma
     */
    public static void text(@NotNull Context context, boolean cancelable, String hint,
                            final @NotNull OnTextDialogClose confirm) {
        text(context, cancelable, hint, InputType.TYPE_NULL, confirm, null);
    }

    /**
     * Mostra una dialog per l'inserimento di una password
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param confirm Evento scaturito quando l'utente conferma
     * @param cancel Evento scaturito quando l'utente cancella l'operazione
     */
    public static void password(@NotNull Context context, boolean cancelable,
                                final @NotNull OnTextDialogClose confirm, final OnTextDialogClose cancel) {
        text(context, cancelable, "Password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                confirm,
                cancel
                );
    }

    /**
     * Mostra una dialog per l'inserimento di una password
     * @param context Contesto
     * @param cancelable True è cancellabile dall'utente
     * @param confirm Evento scaturito quando l'utente conferma
     */
    public static void password(@NotNull Context context, boolean cancelable,
                                final @NotNull OnTextDialogClose confirm) {
        password(context, cancelable, confirm, null);
    }

}
