/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import mergedoc.MergeDocException;
import mergedoc.core.FastStringUtils;
import mergedoc.core.PatternCache;
import mergedoc.xml.ConfigManager;
import mergedoc.xml.ReplaceEntry;

/**
 * プレビュースクロールペインです。
 * @author Shinji Kashihara
 */
public class PreviewScrollPane extends JScrollPane {

    /** オリジナルのプレビュー文字列 */
    private final String originPreviewText;

    /** テキストペイン */
    private final JTextPane textPane = new JTextPane();

    /** メッセージダイアログ */
    private final MessageDialog dialog = new MessageDialog(this);

    /**
     * コンストラクタです。
     * デフォルトではスクロールモードはバッキングストアモードです。updatePreview
     * メソッドによる書き換えはオフスクリーンイメージ上で行われます。
     * @see JViewport#setScrollMode
     */
    public PreviewScrollPane() throws MergeDocException {

        // ビューポートの設定
        JViewport viewport = getViewport();
        viewport.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        viewport.setView(textPane);
        textPane.setEditable(false);
        
        // プレビュー文字列を ConfigManager から取得
        originPreviewText = ConfigManager.getInstance().getPrevewTemplate();
    }

    /**
     * 置換エントリチェックリストの設定によりプレビューを更新します。
     * @param entryCheckList 置換エントリチェックリスト
     */
    public void updatePreview(List<EntryCheckBox> entryCheckList) {

        // 置換エントリによる処理
        String text = originPreviewText;
        for (EntryCheckBox cb : entryCheckList) {
            if (cb.isSelected()) {
                ReplaceEntry entry = cb.getReplaceEntry();
                try {
                    text = entry.replace(text);
                } catch (IllegalStateException e) {
                    dialog.showErrorMessage(e.getMessage());
                }
            }
        }
        text = FastStringUtils.optimizeLineSeparator(text);

        // プレビュー上のタブ表示のためにタブをダミー文字に置換
        final int tabSize = 4;
        final char tabChar = '#';
        char[] fakeTab = new char[tabSize];
        Arrays.fill(fakeTab, tabChar);
        text = FastStringUtils.replaceAll(text, "\t", String.valueOf(fakeTab));
        textPane.setText(text);
        
        // イベント終了後にスクロールバーを元の位置に戻す
        final JScrollBar bar = getVerticalScrollBar();
        final int pos = bar.getValue();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                bar.setValue(pos);
            }
        });

        // 文字の色づけ（Eclipse風味）
        updateColor("(?s)(/\\*[^\\*].+?\\*/)", new Color(63,127,95));
        updateColor("(?s)(/\\*\\*.+?\\*/)", new Color(63,95,191));
        updateColor("(class|public|private|void|extends|super)", new Color(127,0,85));
        updateColor("(?m).*?[ " + tabChar + "](//.*)", new Color(63,127,95));
        updateColor(" (\".*?\")", new Color(42,0,255));
        updateColor(" (@\\w*) ", new Color(127,159,191));

        // タブのダミー文字を背景と同じ色にして不可視にする
        StyledDocument doc = textPane.getStyledDocument();
        MutableAttributeSet atr = new SimpleAttributeSet();
        Color tabColor = new Color(255,255,200);
        StyleConstants.setForeground(atr, tabColor);
        StyleConstants.setBackground(atr, tabColor);
        Pattern pat = PatternCache.getPattern(tabChar + "{" + tabSize + "}");
        Matcher mat = pat.matcher(textPane.getText());
        while (mat.find()) {
            int start = mat.start();
            int len = mat.end() - start;
            doc.setCharacterAttributes(start, len, atr, false);
        }
    }

    /**
     * プレビューの文字の色づけを行います。
     * @param pattern 色づけする文字の正規表現
     * @param color 色
     */
    private void updateColor(String pattern, Color color) {

        StyledDocument doc = textPane.getStyledDocument();
        MutableAttributeSet atr = new SimpleAttributeSet();

        Pattern pat = PatternCache.getPattern(pattern);
        Matcher mat = pat.matcher(textPane.getText());
        while (mat.find()) {
            int start = mat.start(1);
            int len = mat.end(1) - start;
            StyleConstants.setForeground(atr, color);
            doc.setCharacterAttributes(start, len, atr, false);
        }
    }
}
