/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * メッセージダイアログです。
 * @author Shinji Kashihara
 */
public class MessageDialog {
    
    /** 親のコンポーネント */
    private final Component parent;
    
    /**
     * コンストラクタです。
     * @param parent 親のコンポーネント
     */
    public MessageDialog(Component parent) {
        this.parent = parent;
    }
    
    /**
     * 確認メッセージを表示します．
     * @param message メッセージ
     * @return ユーザの選択結果。JOptionPane の定数フィールドを参照。
     */    
    public int showConfirmMessage(String message) {
        return JOptionPane.showConfirmDialog(
            parent, message, "確認", JOptionPane.YES_NO_OPTION);
    }
    
    /**
     * エラーメッセージを表示します．
     * @param message メッセージ
     */    
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(
            parent, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }
}
