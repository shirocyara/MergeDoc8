/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * マージを行うクラスです。
 * @author Shinji Kashihara
 */
public class Merger {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(Merger.class);

	/** クラス種類（class|interface|@interface|enum） */
	private String classKind;
	
    /** クラス名（パッケージを含む） */
    private String className;

    /** API ドキュメントディレクトリ */
    private File docDirectory;

    /** API ドキュメントエンコーディング */
    private String docEncoding = System.getProperty("file.encoding");

    /**
     * コンストラクタです。
     * @param docDirectory
     */
    public Merger(File docDirectory) {
        this.docDirectory = docDirectory;
    }

    /**
     * API ドキュメントエンコーディングを設定します。
     * 設定されなかった場合はデフォルトエンコーディングを使用します。
     * @param docEncoding API ドキュメントエンコーディング
     */
    public void setDocEncoding(String docEncoding) {
        this.docEncoding = docEncoding;
    }
    
    /**
     * Java ソースと Javadoc コメントをマージします。
     * Java ソースに package 宣言が無い場合や、対応する API
     * ドキュメントが見つからない場合はそのまま Java ソースを返します。
     * @param javaSource Java ソース文字列
     * @return マージ後の Java ソース文字列
     * @throws IOException 入出力例外が発生した場合
     */
    public String merge(String source) throws IOException {

        scanClassName(source);
        if (className == null) return source;
        APIDocument apiDoc = new APIDocument(docDirectory, className, docEncoding);
        if (apiDoc.isEmpty()) return source;

        JavaBuffer javaBuf = new JavaBuffer(classKind, className, source);
        while (javaBuf.nextComment()) {
            Signature sig = javaBuf.getSignature();
            Comment com = apiDoc.getComment(sig);
            javaBuf.setLocalizedComment(sig, com);
        }

        String result = javaBuf.finishToString();
        return result;
    }

    /**
     * Java ソース文字列をスキャンし、クラス名（パッケージを含む）を設定します。
     * package 宣言が無い場合は常に null になります。
     * @param source Java ソース文字列
     */
    private void scanClassName(String source) {
        
        Pattern pkgPat = PatternCache.getPattern("(?m)^\\s*package\\s+([\\w\\.]+)");
        Matcher pkgMat = pkgPat.matcher(source);

        if (pkgMat.find()) {
            String packageName = pkgMat.group(1);
            String clsReg = "(?m)^(|[\\w\\s]*?\\s+|.*?\\*/\\s+)(class|interface|@interface|enum)\\s+(\\w+)";
            Pattern clsPat = PatternCache.getPattern(clsReg);
            Matcher clsMat = clsPat.matcher(source);
            if (clsMat.find()) {
            	classKind = clsMat.group(2);
                className = packageName + "." + clsMat.group(3);
                return;
            }
            throw new IllegalArgumentException(
                    "Java ソースからクラス名を取得することが出来ませんでした。\n" +
                    source);
        }
    }
    
    /**
     * 直前のマージした Java ソースのクラス名（パッケージを含む）を取得します。
     * @return クラス名（パッケージを含む）
     */
    public String getMergedClassName() {
        return className;
    }
}
