/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * 進捗パネルです。
 * @author Shinji Kashihara
 */
public class ProgressPanel extends JPanel {

    /** 進捗バー */
    private JProgressBar progressBar = new JProgressBar();

    /** 進捗テキストエリア */
    private JTextArea textArea = new JTextArea(3, 100);

    /**
     * コンストラクタです。 
     */
    public ProgressPanel() {

        // レイアウト設定
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setMaximumSize(ComponentFactory.createMaxDimension());

        // 進捗バーの設定
        int maxWidth = (int) ComponentFactory.createMaxDimension().getWidth();
        progressBar.setMaximumSize(new Dimension(maxWidth, 10));
        progressBar.setStringPainted(true);

        // 進捗テキストエリアの設定
        textArea.setMaximumSize(ComponentFactory.createMaxDimension());
        textArea.setEditable(false);

        // このパネルに追加
        add(progressBar);
        add(ComponentFactory.createSpacer(0, 5));
        add(ComponentFactory.createScrollPane(textArea));
    }

    /**
     * 進捗パネルを初期化します。
     * 進捗バーは不確定モードとなり、アニメーションを表示します。
     */
    public void init() {
        progressBar.setString(null);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        textArea.setText(null);
    }

    /**
     * 進捗の最大値をセットします。
     * 進捗バーは確定モードとなり、アニメーションを停止します。<br>
     * このメソッドはスレッドに対して安全です。
     * @param max 進捗の最大値
     */
    public void setMaximum(int max) {
        progressBar.setMaximum(max);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }

    /**
     * 進捗リストに文字列を追加します。<br>
     * このメソッドはスレッドに対して安全です。
     * @param text 進捗リストに追加する文字列
     */
    public void addListText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int value = progressBar.getValue() + 1;
                progressBar.setValue(value);
                if (value > 1) textArea.append("\n");
                textArea.append(text);
            }
        });
    }
    
    /**
     * 進捗バーに表示する文字列をセットします。<br>
     * このメソッドはスレッドに対して安全です。
     * @param text 進捗バーに表示する文字列
     */    
    public void setBarText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setString(text);
            }
        });
    }
    
    /**
     * 進捗バーの文字列を取得します。
     * @return 進捗バーの文字列
     */    
    public String getBarText() {
    	return progressBar.getString();
    }
}
