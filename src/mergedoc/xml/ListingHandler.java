/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

import java.util.LinkedList;
import java.util.List;


/**
 * 置換エントリのリストを作成するハンドラです。
 * @author Shinji Kashihara
 */
public class ListingHandler extends AbstractHandler {

    /** 置換エントリのリスト */
    private List<ReplaceEntry> replaceEntries = new LinkedList<ReplaceEntry>();

    /**
     * コンストラクタです。
     */
    public ListingHandler() {
    }
    
    /**
     * 置換エントリを処理します。
     * @param entry 置換エントリ
     */
    protected void handle(ReplaceEntry entry) {
        replaceEntries.add(entry);
    }
    
    /**
     * 置換エントリのリストを取得します。
     * @return 置換エントリのリスト
     */
    public List<ReplaceEntry> getReplaceEntries() {
        return replaceEntries;
    }
}
