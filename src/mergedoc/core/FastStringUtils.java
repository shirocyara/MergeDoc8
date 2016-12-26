/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 高速文字列ユーティリティです。
 * @author Shinji Kashihara
 */
public class FastStringUtils {
    
    /** ロガー */
    private static final Log log = LogFactory.getLog(FastStringUtils.class);

    /**
     * コンストラクタです。生成不可。
     */
    private FastStringUtils() {
    }

    /**
     * 指定された target に一致するすべての文字列を置換します。
     * 
     * <p>これは以下のような JDK の String#replace(CharSequence,CharSequence)
     * メソッドと処理内容は同等ですが、内部的に使用する Pattern オブジェクトを
     * キャッシュするため、同じ target の場合、2 回目以降は高速です。
     * 
     * <pre>
     *     input.replace(target, replacement);
     * </pre>
     * 
     * 注意：<br>
     * target が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param input マッチされる文字列
     * @param target 置換される文字列
     * @param replacement 置換文字列
     * @return 結果の文字列
     */
    public static String replace(String input, String target, String replacement) {
        Pattern pattern = PatternCache.getLiteralPattern(target);
        return pattern.matcher(input).replaceAll(Matcher.quoteReplacement(replacement));
    }

    /**
     * 指定された regex に最初に一致する文字列を置換します。
     * 
     * <p>これは以下のような JDK の String#replaceFirst(String,String)
     * メソッドと処理内容は同等ですが、内部的に使用する Pattern オブジェクトを
     * キャッシュするため、同じ regex の場合、2 回目以降は高速です。
     * 
     * <pre>
     *     input.replaceFirst(regex, replacement);
     * </pre>
     * 
     * 注意：<br>
     * regex が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param input マッチされる文字列
     * @param regex 置換される正規表現文字列
     * @param replacement 置換文字列
     * @return 結果の文字列
     */
    public static String replaceFirst(String input, String regex, String replacement) {
        Pattern pattern = PatternCache.getPattern(regex);
        return pattern.matcher(input).replaceFirst(replacement);
    }

    /**
     * 指定された regex に一致するすべての文字列を置換します。
     * 
     * <p>これは以下のような JDK の String#replaceAll(String,String)
     * メソッドと処理内容は同等ですが、内部的に使用する Pattern オブジェクトを
     * キャッシュするため、同じ regex の場合、2 回目以降は高速です。
     * 
     * <pre>
     *     input.replaceAll(regex, replacement);
     * </pre>
     * 
     * 注意：<br>
     * regex が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param input マッチされる文字列
     * @param regex 置換される正規表現文字列
     * @param replacement 置換文字列
     * @return 結果の文字列
     */
    public static String replaceAll(String input, String regex, String replacement) {
        Pattern pattern = PatternCache.getPattern(regex);
        return pattern.matcher(input).replaceAll(replacement);
    }

    /**
     * 指定された regex に一致するか判定します。
     * 
     * <p>これは以下のような JDK の String#matches(String)
     * メソッドと処理内容は同等ですが、内部的に使用する Pattern オブジェクトを
     * キャッシュするため、同じ regex の場合、2 回目以降は高速です。
     * 
     * <pre>
     *     input.matches(regex);
     * </pre>
     * 
     * 注意：<br>
     * regex が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param input マッチされる文字列
     * @param regex 置換される正規表現文字列
     * @return 一致する場合は true
     */
    public static boolean matches(String input, String regex) {
        Pattern pattern = PatternCache.getPattern(regex);
        return pattern.matcher(input).matches();
    }

    /**
     * 指定された regex で文字列を分割します。
     * 
     * <p>これは以下のような JDK の String#split(String,int)
     * メソッドと処理内容は同等ですが、内部的に使用する Pattern オブジェクトを
     * キャッシュするため、同じ regex の場合、2 回目以降は高速です。
     * 
     * <pre>
     *     input.split(regex, limit);
     * </pre>
     * 
     * 注意：<br>
     * regex が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param input 対象となる文字列
     * @param regex 正規表現の区切り文字列
     * @param limit 結果のしきい値
     * @return 区切り文字で分割された文字列配列
     */
    public static String[] split(String input, String regex, int limit) {
        Pattern pattern = PatternCache.getPattern(regex);
        return pattern.split(input, limit);
    }

    /**
     * 正規表現のメタ文字を \ でエスケープします。
     * 対象となるメタ文字はエスケープ文字 \ および前方参照の部分参照を示す $ です。
     * 
     * <p>これは JDK の Matcher.quoteReplacement(String) メソッドと処理内容は
     * 同等ですが高速に動作します。
     * 
     * @param  str  対象文字列
     * @return エスケープした文字列
     */
    public static String quoteReplacement(String str) {
        
        //return str.replaceAll("(\\$|\\\\)", "\\\\$1");
        // パフォーマンスを優先し、正規表現は使用しない
        
        if (!str.contains("\\") && !str.contains("$")) {
            return str;
        }
        int size = str.length();
        int expectSize = (int) (size * 1.1);
        StringBuilder sb = new StringBuilder(expectSize);
        
        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            if (c == '\\' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    /**
     * 改行文字を最適化します。
     * <ol>
     * <li>CRLF → LF
     * <li>CR → LF
     * </ol>
     * 
     * @param str 対象文字列
     * @return 最適化した文字列
     */
    public static String optimizeLineSeparator(String str) {
        
        //return str.replaceAll("(\r\n|\r)", "\n");
        // パフォーマンスを優先し、正規表現は使用しない
        
        str += " ";
        int size = str.length();
        StringBuilder sb = new StringBuilder(size);
        
        for (int i = 0; i < size - 1; i++) {
            char c = str.charAt(i);
            if (c == '\r') {
                if (str.charAt(i+1) != '\n') {
                    sb.append('\n');
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 文字列に含まれるタブをスペースに置換します。
     * 今のところ、JDK のソースに合わせスペース 8 文字として処理します。
     * 
     * @param str 文字列
     * @return タブをスペースに展開した文字列
     */
    public static String untabify(String str) {
        
        final int TAB_WIDTH = 8;
        int size = str.length();
        int expectSize = (int) (size * 1.2);
        StringBuilder sb = new StringBuilder(expectSize);
        
        for (int pos = 0, hPos = -1; pos < size; pos++) {
            char c = str.charAt(pos);
            hPos = (c == '\n') ? -1 : hPos + 1;
            
            if (c == '\t') {
                int fillSize = TAB_WIDTH - hPos % TAB_WIDTH;
                for (int f = 0; f < fillSize; f++) {
                    sb.append(' ');
                }
                hPos = hPos + fillSize - 1;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 文字列の高さ（行数）を求めます。
     * この値は文字列中に含まれる改行（LF）の数と等価です。
     * 
     * @param str 文字列
     * @return 高さ（行数）
     */
    public static int heightOf(String str) {
        
        //return str.split("\n", -1).length - 1;
        // パフォーマンスを優先し split は使用しない
        
        int height = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\n') height++;
        }
        return height;
    }
    
    /**
     * 文字列を改行（LF）で分割し、文字列リストを返します。
     * @param str 対象となる文字列
     * @return 文字列リスト
     */
    public static List<String> splitLine(String str) {
        
        List<String> list = new LinkedList<String>();
        int size = str.length();
        int expectSize = 120;
        StringBuilder sb = new StringBuilder(expectSize);
        
        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            if (c == '\n') {
                list.add(sb.toString());
                sb = new StringBuilder(expectSize);
            } else {
                sb.append(c);
            }
        }
        
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }
}
