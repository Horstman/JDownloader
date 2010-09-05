//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.ClickPositionDialog;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.utils.BinaryLogic;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.TextAreaDialog;

import com.sun.istack.internal.Nullable;

public class UserIO {

    public static final int       FILES_ONLY                     = JFileChooser.FILES_ONLY;
    public static final int       DIRECTORIES_ONLY               = JFileChooser.DIRECTORIES_ONLY;
    public static final int       FILES_AND_DIRECTORIES          = JFileChooser.FILES_AND_DIRECTORIES;
    public static final int       OPEN_DIALOG                    = JFileChooser.OPEN_DIALOG;
    public static final int       SAVE_DIALOG                    = JFileChooser.SAVE_DIALOG;

    /**
     * TO not query user. Try to fill automaticly, or return null
     */
    public static final int       NO_USER_INTERACTION            = 1 << 1;
    /**
     * do not display a countdown
     */
    public static final int       NO_COUNTDOWN                   = 1 << 2;
    /**
     * do not display ok option
     */
    public static final int       NO_OK_OPTION                   = 1 << 3;
    /**
     * do not display cancel option
     */
    public static final int       NO_CANCEL_OPTION               = 1 << 4;
    /**
     * displays a do not show this question again checkbox
     */
    public static final int       DONT_SHOW_AGAIN                = 1 << 5;
    /**
     * IF available a large evrsion of the dialog is used
     */
    public static final int       STYLE_LARGE                    = 1 << 6;
    /**
     * Render html
     */
    public static final int       STYLE_HTML                     = 1 << 7;
    /**
     * Does not display an icon
     */
    public static final int       NO_ICON                        = 1 << 8;
    /**
     * Cancel option ignores Don't show again checkbox
     */
    public static final int       DONT_SHOW_AGAIN_IGNORES_CANCEL = 1 << 9;
    /**
     * If user selects OK Option, the don't show again option is ignored
     */
    public static final int       DONT_SHOW_AGAIN_IGNORES_OK     = 1 << 10;
    /**
     * the textfield will be renderer as a passwordfield
     */
    public static final int       STYLE_PASSWORD                 = 1 << 11;

    /**
     * pressed ok
     */
    public static final int       RETURN_OK                      = 1 << 1;
    /**
     * pressed cancel
     */
    public static final int       RETURN_CANCEL                  = 1 << 2;
    /**
     * don'tz sho again flag ahs been set. the dialog may has been visible. if
     * RETURN_SKIPPED_BY_DONT_SHOW is not set. the user set this flag latly
     */
    public static final int       RETURN_DONT_SHOW_AGAIN         = 1 << 3;
    /**
     * don't show again flag has been set the dialog has not been visible
     */
    public static final int       RETURN_SKIPPED_BY_DONT_SHOW    = 1 << 4;
    /**
     * Timeout has run out. Returns current settings or default values
     */
    public static final int       RETURN_COUNTDOWN_TIMEOUT       = 1 << 5;
    public static final int       ICON_INFO                      = 0;
    public static final int       ICON_WARNING                   = 1;
    public static final int       ICON_ERROR                     = 2;
    public static final int       ICON_QUESTION                  = 3;

    protected static final UserIO INSTANCE                       = new UserIO();
    private static int            COUNTDOWN_TIME                 = -1;

    public static int getCountdownTime() {
        return UserIO.COUNTDOWN_TIME > 0 ? UserIO.COUNTDOWN_TIME : Math.max(2, GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_INPUTTIMEOUT, 20));
    }

    public static UserIO getInstance() {

        return UserIO.INSTANCE;
    }

    /**
     * Checks wether this answerfalg contains the ok option
     * 
     * @param answer
     * @return
     */
    public static boolean isOK(final int answer) {
        return JDFlags.hasSomeFlags(answer, UserIO.RETURN_OK);
    }

    /**
     * Sets the countdowntime for this session. does not save!
     * 
     * @param time
     */
    public static void setCountdownTime(final int time) {
        UserIO.COUNTDOWN_TIME = time > 0 ? time : -1;
    }

    /**
     * COnverts the flag mask of AW Dialogs to UserIO
     * 
     * @param ret
     * @return
     */
    private int convertAWAnswer(final int ret) {
        int response = 0;
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_CANCEL)) {
            response |= UserIO.RETURN_CANCEL;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_OK)) {
            response |= UserIO.RETURN_OK;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_CLOSED)) {
            response |= UserIO.RETURN_CANCEL;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_DONT_SHOW_AGAIN)) {
            response |= UserIO.RETURN_DONT_SHOW_AGAIN;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_SKIPPED_BY_DONT_SHOW)) {
            response |= UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_TIMEOUT)) {
            response |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
        }
        return response;
    }

    /**
     * The flags in org.appwork.utils.swing.dialog.Dialog are different, so we
     * need a converter
     * 
     * @param flag
     * @return
     */
    private int convertFlagToAWDialog(final int flag) {
        int ret = 0;

        if (BinaryLogic.containsAll(flag, UserIO.NO_USER_INTERACTION)) {
            // flag|=
            // TODO
        }

        if (BinaryLogic.containsNone(flag, UserIO.NO_COUNTDOWN)) {
            ret |= Dialog.LOGIC_COUNTDOWN;
        }
        if (BinaryLogic.containsAll(flag, UserIO.NO_OK_OPTION)) {
            ret |= Dialog.BUTTONS_HIDE_OK;
        }
        if (BinaryLogic.containsAll(flag, UserIO.NO_CANCEL_OPTION)) {
            ret |= Dialog.BUTTONS_HIDE_CANCEL;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN)) {
            ret |= Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_LARGE)) {
            ret |= Dialog.STYLE_LARGE;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_HTML)) {
            ret |= Dialog.STYLE_HTML;
        }

        if (BinaryLogic.containsAll(flag, UserIO.NO_ICON)) {
            ret |= Dialog.STYLE_HIDE_ICON;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL)) {
            ret |= Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK)) {
            ret |= Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_PASSWORD)) {
            ret |= Dialog.STYLE_PASSWORD;

        }
        return ret;
    }

    private ImageIcon getDefaultIcon(final String text) {
        if (text.contains("?")) {
            return this.getIcon(UserIO.ICON_QUESTION);
        } else if (text.matches(JDL.L("userio.errorregex", ".*(error|failed).*"))) {
            return this.getIcon(UserIO.ICON_ERROR);
        } else if (text.contains("!")) {
            return this.getIcon(UserIO.ICON_WARNING);
        } else {
            return this.getIcon(UserIO.ICON_INFO);
        }
    }

    public ImageIcon getIcon(final int iconInfo) {
        switch (iconInfo) {
        case UserIO.ICON_ERROR:
            return JDTheme.II("gui.images.stop", 32, 32);
        case UserIO.ICON_WARNING:
            return JDTheme.II("gui.images.warning", 32, 32);
        case UserIO.ICON_QUESTION:
            return JDTheme.II("gui.images.help", 32, 32);
        default:
            return JDTheme.II("gui.images.config.tip", 32, 32);
        }
    }

    public String requestCaptchaDialog(final int flag, final String host, final ImageIcon icon, final File captchafile, final String suggestion, final String explain) {
        return new EDTHelper<String>() {

            @Override
            public String edtRun() {
                AbstractDialog<String> dialog;

                dialog = new CaptchaDialog(flag | Dialog.LOGIC_COUNTDOWN, host, captchafile, suggestion, explain);
                return Dialog.getInstance().showDialog(dialog);

            }

        }.getReturnValue();

    }

    public Point requestClickPositionDialog(final File imagefile, final String title, final String explain) {

        return new EDTHelper<Point>() {

            @Override
            public Point edtRun() {
                AbstractDialog<Point> dialog;

                dialog = new ClickPositionDialog(0, imagefile, title, explain);
                return Dialog.getInstance().showDialog(dialog);

            }

        }.getReturnValue();

    }

    /**
     * Shows a combobox dialog. returns the options id if the user confirmed, or
     * -1 if the user canceled
     * 
     * @param flag
     * @param title
     * @param question
     * @param options
     * @param defaultSelection
     * @param icon
     * @param okText
     * @param cancelText
     * @param renderer
     *            TODO
     * @return
     */
    public int requestComboDialog(final int flag, final String title, final String question, final Object[] options, final int defaultSelection, final ImageIcon icon, final String okText, final String cancelText, final ListCellRenderer renderer) {
        return this.convertAWAnswer(Dialog.getInstance().showComboDialog(this.convertFlagToAWDialog(flag), title, question, options, defaultSelection, icon, okText, cancelText, renderer));
    }

    public int requestConfirmDialog(final int flag, final String question) {
        return this.requestConfirmDialog(flag, JDL.L("jd.gui.userio.defaulttitle.confirm", "Please confirm!"), question, this.getDefaultIcon(question), null, null);
    }

    public int requestConfirmDialog(final int flag, final String title, final String question) {
        return this.requestConfirmDialog(flag, title, question, this.getDefaultIcon(title + question), null, null);
    }

    public int requestConfirmDialog(final int flag, final String title, final String message, final ImageIcon icon, final String okOption, final String cancelOption) {

        return this.convertAWAnswer(Dialog.getInstance().showConfirmDialog(this.convertFlagToAWDialog(flag), title, message, icon, okOption, cancelOption));
    }

    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final FileFilter fileFilter, final Boolean multiSelection) {
        return this.requestFileChooser(id, title, fileSelectionMode, fileFilter, multiSelection, null, null);
    }

    /**
     * Requests a FileChooserDialog.
     * 
     * @param id
     *            ID of the dialog (used to save and restore the old directory)
     * @param title
     *            dialog-title or null for default
     * @param fileSelectionMode
     *            mode for selecting files (like {@link UserIO#FILES_ONLY}) or
     *            null for default
     * @param fileFilter
     *            filters the choosable files or null for default
     * @param multiSelection
     *            multible files choosable? or null for default
     * @param startDirectory
     *            the start directory
     * @param dialogType
     *            mode for the dialog type (like {@link UserIO#OPEN_DIALOG}) or
     *            null for default
     * @return an array of files or null if the user cancel the dialog
     */
    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final FileFilter fileFilter, final Boolean multiSelection, final File startDirectory, final Integer dialogType) {

        return Dialog.getInstance().showFileChooser(id, title, fileSelectionMode, fileFilter, multiSelection, dialogType, null);
    }

    /**
     * 
     * @param flag
     *            flag
     * @param question
     *            question
     * @param defaultvalue
     *            defaultvalue
     * @return
     */
    public String requestInputDialog(final int flag, final String question, final String defaultvalue) {
        return this.requestInputDialog(flag, JDL.L("jd.gui.userio.defaulttitle.input", "Please enter!"), question, defaultvalue, this.getDefaultIcon(question), null, null);
    }

    public String requestInputDialog(final int flag, final String title, final String message, final String defaultMessage, final ImageIcon icon, final String okOption, final String cancelOption) {

        return Dialog.getInstance().showInputDialog(this.convertFlagToAWDialog(flag), title, message, defaultMessage, icon, okOption, cancelOption);
    }

    public String requestInputDialog(final String message) {
        return this.requestInputDialog(0, message, null);
    }

    public void requestMessageDialog(final int flag, final String message) {
        this.requestMessageDialog(flag, JDL.L("gui.dialogs.message.title", "Message"), message);
    }

    public void requestMessageDialog(final int flag, final String title, final String message) {

        this.requestConfirmDialog(UserIO.NO_CANCEL_OPTION | flag, title, message, this.getIcon(UserIO.ICON_INFO), null, null);

    }

    public void requestMessageDialog(final String message) {
        this.requestMessageDialog(0, JDL.L("gui.dialogs.message.title", "Message"), message);
    }

    public void requestMessageDialog(final String title, final String message) {
        this.requestMessageDialog(0, title, message);
    }

    /**
     * Displays a Dialog with a title, a message, and an editable Textpane. USe
     * it to give the user a dialog to enter Multilined text
     * 
     * @param title
     * @param message
     * @param def
     * @return
     */
    @Nullable
    public String requestTextAreaDialog(final String title, final String message, final String def) {
        return new EDTHelper<String>() {

            @Override
            public String edtRun() {
                TextAreaDialog dialog;
                try {
                    dialog = new TextAreaDialog(title, message, def);

                    // or is it enough to use edt when it comes up to display
                    // the
                    // dialog
                    return Dialog.getInstance().showDialog(dialog);
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

        }.getReturnValue();

    }
    //
    // public String[] requestTwoTextFieldDialog(final String title, final
    // String messageOne, final String defOne, final String messageTwo, final
    // String defTwo) {
    // synchronized (UserIO.INSTANCE) {
    // return this.showTwoTextFieldDialog(title, messageOne, defOne, messageTwo,
    // defTwo);
    // }
    // }

}
