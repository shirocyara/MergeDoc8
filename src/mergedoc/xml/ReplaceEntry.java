/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import mergedoc.core.FastStringUtils;
import mergedoc.core.PatternCache;


/**
 * 置換エントリです。置換エントリは子を階層的に持つことが出来ます。
 * @author Shinji Kashihara
 */
public class ReplaceEntry {

    /** 説明 */
    private String description = "";

    /** 置換前文字列 */
    private String before = "";
    
    /** 置換後文字列 */
    private String after = "";

    /** 対象 */
    private String target = "";

    /** 子となる置換エントリ */
    private List<ReplaceEntry> entries;

    /**
     * コンストラクタです。
     */
    public ReplaceEntry() {
    }
    
    /**
     * 説明をセットします。
     * @param description 説明
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 説明を取得します。
     * @return 説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 置換前文字列をセットします。
     * @param before 置換前文字列
     */
    public void setBefore(String before) {
        this.before = before;
    }

    /**
     * 置換前文字列を取得します。
     * @return 置換前文字列
     */
    public String getBefore() {
        return before;
    }

    /**
     * 置換後文字列をセットします。
     * @param after 置換後文字列
     */
    public void setAfter(String after) {
        this.after = after;
    }
    
    /**
     * 置換後文字列を取得します。
     * @return 置換後文字列
     */
    public String getAfter() {
        return after;
    }

    /**
     * 対象をセットします。
     * @param target 対象
     */
    public void setTarget(String target) {
        this.target = target;
    }
    
    /**
     * 対象を取得します。
     * @return 対象
     */
    public String getTarget() {
        return target;
    }
    
    /**
     * 子となる置換エントリを追加します。
     * @param entry 子となる置換エントリ
     */
    public void addChild(ReplaceEntry entry) {
        if (entries == null) {
            entries = new LinkedList<ReplaceEntry>();
        }
        entries.add(entry);
    }
        
    /**
     * 指定した文字列をこの置換エントリの設定で処理します。
     * 子を持つ場合は再起的に処理されます。
     * @param source ソース文字列
     * @return 処理後のソース文字列
     * @throws IllegalStateException 置換エントリの正規表現の構文が無効な場合
     */
    public String replace(String source) {

        // この置換エントリの処理
        if (before.length() > 0) {
            try {
                source = replaceSelf(source);
            } catch(PatternSyntaxException e) {
                throw new IllegalStateException(
                    "置換エントリの処理でエラーが発生しました。\n" +                     "原因: " + e.getMessage() + "\n" +                    "前: " + before + "\n" + "後: " + after);
            }
        }
        // 子置換エントリの処理
        if (entries != null) {
            for (ReplaceEntry entry : entries) {
                source = entry.replace(source);
            }
        }
        return source;
    }

    /**
     * 指定した文字列をこの置換エントリの設定で処理します。
     * @param source ソース文字列
     * @return 処理後のソース文字列
     * @throws PatternSyntaxException 置換エントリの正規表現の構文が無効な場合
     */
    private String replaceSelf(String source) {

        if ("Javadocコメント".equalsIgnoreCase(target)) {

            // 対象が Javadocコメント の場合
            Pattern pat = PatternCache.getPattern("(?s)/\\*\\*.+?\\*/");
            Matcher mat = pat.matcher(source);
            StringBuffer sb = new StringBuffer(source.length());
            while (mat.find()) {
                String str = mat.group();
                str = FastStringUtils.replaceAll(str, before, after);
                str = FastStringUtils.quoteReplacement(str);
                mat.appendReplacement(sb, str);
            }
            mat.appendTail(sb);
            source = sb.toString();

        } else {
                
            // 対象が指定されていない場合
            source = FastStringUtils.replaceAll(source, before, after);
        }
        return source;
    }
    
    /**
     * このインスタンスの文字列表現を取得します。
     * @return 文字列表現
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n説明 [");
        sb.append(description);
        sb.append("]\n前 [");
        sb.append(before);
        sb.append("]\n後 [");
        sb.append(after);
        sb.append("]\n");
        if (entries != null) {
            for (ReplaceEntry entry : entries) {
                sb.append(entry);
            }
        }
        return sb.toString();
    }
}
