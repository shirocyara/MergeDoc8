/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import javax.swing.JCheckBox;

import mergedoc.xml.ReplaceEntry;

/**
 * 置換エントリを持つチェックボックスです。
 * @author Shinji Kashihara
 */
public class EntryCheckBox extends JCheckBox {

    /** 置換エントリ */
    private ReplaceEntry entry;

    /**
     * コンストラクタです。 
     * @param entry 置換エントリ
     */
    public EntryCheckBox(ReplaceEntry entry) {
        this.entry = entry;
        setText(entry.getDescription());
    }
    
    /**
     * 置換エントリを取得します。
     * @return 置換エントリ
     */
    public ReplaceEntry getReplaceEntry() {
        return entry;
    }
}
