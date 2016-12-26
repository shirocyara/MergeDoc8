/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

/**
 * ソース文字列の置換処理を行うハンドラです。
 * @author Shinji Kashihara
 */
public class ReplaceHandler extends AbstractHandler {

    /** ソース文字列 */
    private String source;

    /**
     * コンストラクタです。
     * @param source ソース文字列
     */
    public ReplaceHandler(String source) {
        this.source = source;
    }
    
    /**
     * 置換エントリを処理します。
     * @param entry 置換エントリ
     */
    protected void handle(ReplaceEntry entry) {
        source = entry.replace(source);
    }
    
    /**
     * 置換後のソース文字列を取得します。
     * @return 置換後のソース文字列
     */
    public String getResult() {
        return source;
    }
}
