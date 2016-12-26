/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ボタンバーです。
 * @author Shinji Kashihara
 */
public class ButtonBar extends JPanel {

    /** ロガー */
    private static final Log log = LogFactory.getLog(ButtonBar.class);

    /** 実行／戻るボタン（共用） */
    private JButton runButton;

    /** 実行時に呼ばれるリスナ */
    private ActionListener runListener;

    /** 戻る時に呼ばれるリスナ */
    private ActionListener backListener;

    /** キャンセル／終了ボタン（共用） */
    private JButton endButton;

    /** キャンセル時に呼ばれるリスナ */
    private ActionListener cancelListener;

    /** 終了時に呼ばれるリスナ */
    private ActionListener endListener;

    /** ボタン状態インタフェース */
    private static interface ButtonState {
        void apply();
    }

    /** ボタン状態：初期 */
    public final ButtonState INIT_STATE = new ButtonState() {
        public void apply() {
            runButton.setText("実行(R)");
            runButton.setMnemonic(KeyEvent.VK_R);
            runButton.setEnabled(true);
            runButton.addActionListener(runListener);
            endButton.setText("終了(E)");
            endButton.setMnemonic(KeyEvent.VK_E);
            endButton.setEnabled(true);
            endButton.addActionListener(endListener);
        }
    };

    /** ボタン状態：処理中 */
    public final ButtonState WORKING_STATE = new ButtonState() {
        public void apply() {
            runButton.setText("実行(R)");
            runButton.setEnabled(false);
            endButton.setText("キャンセル(C)");
            endButton.setMnemonic(KeyEvent.VK_C);
            endButton.setEnabled(true);
            endButton.addActionListener(cancelListener);
        }
    };

    /** ボタン状態：完了後 */
    public final ButtonState FINISH_STATE = new ButtonState() {
        public void apply() {
            runButton.setText("戻る(B)");
            runButton.setMnemonic(KeyEvent.VK_B);
            runButton.setEnabled(true);
            runButton.addActionListener(backListener);
            endButton.setText("終了(E)");
            endButton.setMnemonic(KeyEvent.VK_E);
            endButton.setEnabled(true);
            endButton.addActionListener(endListener);
        }
    };

    /** ボタン状態：キャンセル後 */
    public final ButtonState CANCEL_STATE = new ButtonState() {
        public void apply() {
            runButton.setText("戻る(B)");
            runButton.setMnemonic(KeyEvent.VK_B);
            runButton.setEnabled(true);
            runButton.addActionListener(backListener);
            endButton.setText("キャンセル(C)");
            endButton.setEnabled(false);
        }
    };

    /**
     * コンストラクタです。
     */
    public ButtonBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    }

    /**
     * ボタン状態をセットします。
     * このメソッドを呼び出す前に適切なリスナーがセットされている必要があります。<br>
     * このメソッドはスレッドに対して安全です。
     * @param state ボタン状態
     */
    public void setState(final ButtonState state) {
        
        if (runListener == null || backListener == null ||
            cancelListener == null || endListener == null)
        {
            String m = "ボタンバーのリスナーが設定されていないため状態を変更出来ません。";
            RuntimeException e = new IllegalStateException(m);
            log.fatal(m, e);
            throw e;
        }

        // ボタン作成
        final JLabel filler = new JLabel();
        int maxWidth = (int) ComponentFactory.createMaxDimension().getWidth();
        filler.setMaximumSize(new Dimension(maxWidth, 0));
        runButton = createButton();
        endButton = createButton();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // このパネルにコンポーネントを追加
                removeAll();
                add(filler);
                add(runButton);
                add(ComponentFactory.createSpacer(10, 0));
                add(endButton);

                // ボタン状態を適用
                state.apply();
            }
        });
    }

    /**
     * サイズが一意なボタンを作成します。
     * @return ボタン
     */
    private JButton createButton() {
        JButton button = new JButton();
        ComponentFactory.ensureSize(button, 100, 21);
        return button;
    }

    /**
     * 実行時に呼ばれるリスナをセットします。
     * @param runListener 実行時に呼ばれるリスナ
     */
    public void setRunListener(ActionListener runListener) {
        this.runListener = runListener;
    }

    /**
     * 戻る時に呼ばれるリスナをセットします。
     * @param runListener 戻る時に呼ばれるリスナ
     */
    public void setBackListener(ActionListener backListener) {
        this.backListener = backListener;
    }

    /**
     * キャンセル時に呼ばれるリスナをセットします。
     * @param cancelListener キャンセル時に呼ばれるリスナ
     */
    public void setCancelListener(ActionListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    /**
     * 終了時に呼ばれるリスナをセットします。
     * @param endListener 終了時に呼ばれるリスナ
     */
    public void setEndListener(ActionListener endListener) {
        this.endListener = endListener;
    }

    /**
     * このボタンバーの有効無効を切り替えます。<br>
     * このメソッドはスレッドに対して安全です。
     * @param enabled 有効にする場合は true
     */
    public void setEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                runButton.setEnabled(enabled);
                endButton.setEnabled(enabled);
            }
        });
    }
}
