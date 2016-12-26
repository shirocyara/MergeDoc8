/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * タイトル付きパネルです。
 * @author Shinji Kashihara
 */
public class TitledPanel extends JPanel {

    /** コンポーネントを配置する内側パネル */
    private JPanel innerPanel;

    /**
     * コンストラクタです。 
     * @param title タイトル文字列
     */
    public TitledPanel(String title) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new TitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP));
    }
    
    /**
     * コンポーネントをこのコンテナの最後に追加します。
     * @param comp 追加されるコンポーネント
     * @return コンポーネント引数
     * @see JPanel#add(Component)
     */
    public Component add(Component comp) {
        
        if (innerPanel == null) {
            innerPanel = new JPanel();
            innerPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
            innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
            super.add(innerPanel);
        } else {
            innerPanel.add(ComponentFactory.createSpacer(0, 5));
        }
        innerPanel.add(comp);
        return comp;
    }
}
