/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Pattern オブジェクトのキャッシュです。
 * キャッシュの同期化は行われません。
 * 
 * @author Shinji Kashihara
 */
public class PatternCache {
    
    /** ロガー */
    private static final Log log = LogFactory.getLog(PatternCache.class);

    /** 正規表現キャッシュ（キー：正規表現文字列、値：Pattern オブジェクト） */
    private static final Map<String, Pattern> regexCache =
        new HashMap<String, Pattern>();
    
    /** リテラルキャッシュ（キー：リテラル文字列、値：Pattern オブジェクト） */
    private static final Map<String, Pattern> literalCache =
        new HashMap<String, Pattern>();

    /**
     * コンストラクタです。生成不可。
     */
    private PatternCache() {
    }
    
    /**
     * 正規表現 Pattern オブジェクトを取得します。
     * 引数の regex が同じ場合は、同じ Pattern オブジェクトが返されます。
     * 
     * <p>注意：<br>
     * regex が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param regex 正規表現文字列
     * @return Pattern オブジェクト
     */
    public static Pattern getPattern(String regex) {
        
        Pattern pattern = regexCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            regexCache.put(regex, pattern);
        }
        return pattern;
    }
    
    /**
     * リテラル Pattern オブジェクトを取得します。
     * 引数の target が同じ場合は、同じ Pattern オブジェクトが返されます。
     * 
     * <p>注意：<br>
     * target が毎回異なるようなケースでは、無駄な Pattern オブジェクトが
     * キャッシュされるため、使用しないようにしてください。
     * 
     * @param target リテラル文字列
     * @return Pattern オブジェクト
     */
    public static Pattern getLiteralPattern(String target) {
        
        Pattern pattern = literalCache.get(target);
        if (pattern == null) {
            pattern = Pattern.compile(target, Pattern.LITERAL);
            literalCache.put(target, pattern);
        }
        return pattern;
    }
}
