/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Java ソース出力バッファです。
 * @author Shinji Kashihara
 */
public class JavaBuffer {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(JavaBuffer.class);

	/** クラス種類（class|interface|@interface|enum） */
	private String classKind;

    /** クラス名 */
    private final String className;
    
    /** Java ソース文字列 */
    private final String source;

    /** ブロックコメントのパターン */
    private static final Pattern commentPattern = PatternCache.getPattern(
        "(?sm)^ *?/\\*\\*.*?\\*/ *?\n");

    /** ブロックコメントのマッチャー */
    private final Matcher commentMatcher;

    /** 出力バッファ */
    private final StringBuffer outputBuffer = new StringBuffer();
    
    /** クラス名と終了位置を保持するクラス */
    private static class ClassBlock {
        final String name;
        final int end;
        ClassBlock(String name, int end) {
            this.name = name;
            this.end = end;
        }
    }

    /** ClassBlock スタック */
    private final Stack<ClassBlock> classStack = new Stack<ClassBlock>();
    
    /** インナークラスコメントが無いことを示す一時的なコメント */
    private static final String DUMMY_COMMENT =
        "/** Empty comment. " + JavaBuffer.class.getName() + ". */\n";

    /**
     * コンストラクタです。
     * @param classKind クラス種類
     * @param className クラス名
     * @param javaSource Java ソース
     */
    public JavaBuffer(String classKind, String className, String javaSource) {

        this.classKind = classKind;
        this.className = className;
        this.source = setupDummyComment(javaSource);
        commentMatcher = commentPattern.matcher(source);

        // トップクラスをクラススタックにプッシュ        
        ClassBlock cb = new ClassBlock(className, source.length());
        classStack.push(cb);
    }
    
    /**
     * Javadoc コメントが無いクラス宣言にダミーコメントを挿入します。
     * <p>
     * Javadoc コメントの取込みは Java ソースを nextComment メソッドで
     * ブロックコメント単位で走査し、そのコメントがどのクラス（インナークラス）
     * に属するかを classBlock クラスのスタックで制御します。
     * <p>
     * しかし、インナークラス宣言に Javadoc コメントが無い場合があるので
     * ここでダミー Javadoc コメントを挿入します（後で削除）。なお、Javadoc
     * コメントが無い場合は Javadoc API ドキュメントも存在しません。
     * <pre>
     * JDK1.4 javax.swing.JEditorPane のインナークラス である 
     *        JEditorPaneAccessibleHypertextSupport.HTMLLink など
     * </pre>
     * <p>
     * JDK1.4 java.beans.beancontext.BeanContextServicesSupport#BCSSChild は
     * /* で始まるクラスコメントがありますが、Javadoc コメントでないのに
     * Javadoc API ドキュメントが存在します。今のところ、そのようなコメントは
     * 日本語化の対象とはなりません。
     * 
     * @param src Java ソース文字列
     * @return ダミーコメントを挿入したソース文字列
     */    
    private String setupDummyComment(String src) {

        // トップクラスは Javadoc コメントとシグネチャの間に行コメントがある
        // 場合があり「コメントが無い」という判定が困難なため、一旦ソースを
        // 最初のブロック開始位置で分割し、コメントの無いクラス宣言にダミー
        // コメントを挿入する
        //Matcher mat = Pattern.compile("\\{[^@]").matcher(src);
        //int pos = 0;
        //if (mat.find()) pos = mat.start();
        //String head = src.substring(0, pos);
        //String body = src.substring(pos);
        //body = body.replaceAll(
        //    "([^/\\s]( *?\n)+)((\\s*)[\\w\\s]*\\s(class|interface)\\s)",
        //    "$1$4/\\*\\*" + DUMMY_COMMENT + "\\*/\n$3");
        //return head + body;

        //----------------------------------------------------------------------
        // 上記正規表現によるダミーの挿入は Profiler で検査した結果、パフォーマ
        // ンスが悪いため廃止し、下記のような地道に 1 文字ずつ評価する方法に
        // 変更した。パフォーマンスは約 6 倍にアップ。
        // 下記の方法ではコメント部分を厳密に判定しているため、上記方法とは
        // 異なり Javadoc コメントとクラス宣言の間に行コメントが存在する場合も
        // 正常に動作する。
        // 
        //   トップクラス宣言の前に行コメントがあるクラス
        //     → JDK1.4 java.net.Authenticator など
        //   インナークラス宣言の前に行コメントがあるクラス
        //     → JDK1.4 javax.swing.plaf.basic.BasicTableUI など
        //----------------------------------------------------------------------

        char[] c = src.toCharArray();
        int last = c.length - 1;
        List<Integer> dummyInsertPositions = new ArrayList<Integer>();
        int declareMaxLength = 12;
        
        for (int i = declareMaxLength; i <= last; i++) {

            if (c[i-1] == '/' && c[i] == '*') {
            	// ブロックコメントを読み飛ばし 
                for (i++; i <= last; i++) {
                    if (c[i-1] == '*' && c[i] == '/') {
                        break;
                    }
                }
            } else if (c[i-1] == '/' && c[i] == '/') {
            	// 行コメントを読み飛ばし
                for (i++; i <= last; i++) {
                    if (c[i] == '\n') {
                        i++;
                        break;
                    }
                }
            } else if (c[i-1] != '\\' && c[i-1] != '\'' && c[i] == '"') {
            	// リテラル（シングルまたはダブルクォートで囲まれた部分）を読み飛ばし
                for (i++; i <= last; i++) {
                    if (c[i-1] != '\\' && c[i-1] != '\'' && c[i] == '"') {
                        break;
                    }
                }
            }
            if (i >= last) break;

            // 型宣言位置を見つける
            // class|interface|@interface|enum 
            int declaPos = -1;
            if (c[i] == ' ' || c[i] == '\n' || c[i] == '<') {
            	
                if (c[i-5] == 'c' && c[i-4] == 'l' && c[i-3] == 'a' &&
                    c[i-2] == 's' && c[i-1] == 's')
                {
                	// class 宣言の場合
                    if (c[i-6] == ' ' || c[i-6] == '\n') {
                        declaPos = i - 7;
                    }
                } else if (c[i-9] == 'i' && c[i-8] == 'n' && c[i-7] == 't' &&
                           c[i-6] == 'e' && c[i-5] == 'r' && c[i-4] == 'f' &&
                           c[i-3] == 'a' && c[i-2] == 'c' && c[i-1] == 'e')
                {
                	// interface 宣言の場合
                    if (c[i-10] == ' ' || c[i-10] == '\n') {
                        declaPos = i - 11;
                    } else if (c[i-10] == '@') {
                    	// @interface 宣言の場合
                        if (c[i-11] == ' ' || c[i-11] == '\n') {
                            declaPos = i - 12;
                        }
                    }
                } else if (c[i-4] == 'e' && c[i-3] == 'n' && c[i-2] == 'u' &&
                		   c[i-1] == 'm')
                {
                	// enum 宣言の場合
                    if (c[i-5] == ' ' || c[i-5] == '\n') {
                        declaPos = i - 6;
                    }
                }
            }

            // クラス宣言に Javadoc コメントが無ければ、ダミー挿入位置リストに追加
            for (int j = declaPos; j > 0; j--) {
                
                if (c[j-1] == '*' && c[j] == '/') break;
                if (c[j] == ';' || c[j] == '}' || c[j] == '{') {
                    
                    for (int k = j - 1; k > 0; k--) {
                        if (c[k-1] == '/' && c[k] == '/') break;
                        if (c[k] == '\n') {
                            
                            for (int l = j + 1; l < i; l++) {
                                if (c[l] == '\n') {
                                    dummyInsertPositions.add(l + 1);
                                    k = -1;
                                    j = -1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // ダミー挿入位置リストを元に、ダミー Javadoc コメントをソースに挿入
        StringBuilder sb = new StringBuilder(src);
        for (int i = dummyInsertPositions.size() - 1; i >= 0; i--) {
            int pos = dummyInsertPositions.get(i);
            sb.insert(pos, DUMMY_COMMENT);
        }
        return sb.toString();
    }
    
    /**
     * 次のブロックコメントがあるか判定します。
     * ある場合は現在位置をブロックコメントに移動します。
     * ブロックコメントが連続している場合は最後のブロックコメントが有効になります。
     * @return 次のブロックコメントがある場合は true
     */    
    public boolean nextComment() {

        while (commentMatcher.find()) {

            // 下記のようなコメントは無視して次へ
            // /*******************/
            // JDK1.4 java.io.ObjectStreamConstants とか
            String sourceComment = getSourceComment();
            if (FastStringUtils.matches(sourceComment, "\\s*/\\*+/\\s*\n")) {
                continue;
            }
            
            // ダミーコメントの場合は削除して次へ
            if (sourceComment.contains(DUMMY_COMMENT)) {
                getSignature(); //クラススタック操作のため呼び出す
                commentMatcher.appendReplacement(outputBuffer, "");
                continue;
            }

            // 次のコメントとの間が空白の場合は次へコメントへ
            Matcher nextMat = commentPattern.matcher(source);
            int currentEnd = commentMatcher.end();
            if (nextMat.find(currentEnd)) {
                String c2c = source.substring(currentEnd, nextMat.start());
                if (FastStringUtils.matches(c2c, "\\s*")) {
                    continue;
                }
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * 現在位置のブロックコメントを取得します。
     * @return 現在位置のブロックコメント
     */
    private String getSourceComment() {
        return commentMatcher.group();
    }
    
    /**
     * 現在位置のシグネチャを取得します。
     * @return シグネチャ。取得できない場合は null。
     */    
    public Signature getSignature() {

        int commentEndPos = commentMatcher.end();
        String commentEndToEOF = source.substring(commentEndPos, source.length());
        
        // シグネチャを取得しやすくするためにアノテーション宣言を除去
        commentEndToEOF = FastStringUtils.replaceFirst(
        		commentEndToEOF, "(?s)^(\\s*@[\\w]+\\s*\\(.*?\\))*\\s*", "");
        
        Pattern sigPattern = PatternCache.getPattern("(?s)(.+?)(throws|\\{|\\=|;|,\\s*/\\*|\\})");
        Matcher sigMatcher = sigPattern.matcher(commentEndToEOF);

        if (sigMatcher.find()) {
            
            // インナークラスの終端より後の場合はクラススタックを減らす
            ClassBlock classBlock = classStack.peek();
            while (commentEndPos > classBlock.end && classStack.size() > 1) {
                classStack.pop();
                classBlock = classStack.peek();
            }

            // シグネチャ作成。Javadoc コメントとシグネチャの間に
            // ブロックコメントがある場合はそれを取り除く
            String sigStr = sigMatcher.group(1);
            sigStr = FastStringUtils.replaceFirst(sigStr, "(?s)/\\*[^\\*].*?\\*/\\s*", "");
            if (classKind.equals("@interface")) {
            	sigStr = sigStr.replace("()", "");
            }
            Signature sig = new Signature(classBlock.name, sigStr);

            // インナークラス宣言の場合はクラススタックに追加
            if (sig.isDeclareInnerClass()) {
                String name = sig.getClassName();
                int end = searchEndOfInner(commentEndToEOF, name);
                classBlock = new ClassBlock(name, end);
                classStack.push(classBlock);
            }

            return sig;
        }
        
        log.warn("Javadoc コメントの後のシグネチャを取得できませんでした。\n" +
        	commentEndToEOF);
        return null;
    }
    
    /**
     * インナークラスの終了位置を取得します。
     * @param currentToEnd ソースの現在位置から最後までの文字列
     * @param iClassName インナークラス名
     * @return インナークラスの終了位置
     */    
    private int searchEndOfInner(String currentToEnd, String iClassName) {

        int nestLevel = 1;
        for (int i = currentToEnd.indexOf('{') + 1; i < currentToEnd.length(); i++) {
            char c = currentToEnd.charAt(i);
            if (c == '{') nestLevel++; 
            if (c == '}') nestLevel--;
            if (nestLevel == 0) {
                return commentMatcher.end() + i;
            }
        }
        
        log.warn(
            "インナークラス " + className + "#" + iClassName +
            " の終了位置が検出できませんでした。");
        return -1;
    }
    
    /**
     * 現在位置にローカライズされたブロックコメントをセットします。
     * 指定されたブロックコメントが null の場合は無視します。
     * @param sig シグネチャ
     * @param comment ローカライズされたブロックコメント
     */
    public void setLocalizedComment(Signature sig, Comment comment) {
    	
        if (comment == null) return;
        String srcComment = getSourceComment();
        comment.setSourceBody(srcComment);
        String docComment = comment.buildComment();
        
        // debug setLocalizedComment シグネチャ、コメントの確認
    	//log.debug("シグネチャ: " + sig);
    	//log.debug("英語 Java ソースコメント:\n" + srcComment);
    	//log.debug("日本語 API ドキュメントコメント:\n" + docComment + "\n--------------");
        
        if (docComment == null || docComment.length() == 0) return;
        docComment = FastStringUtils.quoteReplacement(docComment);
        commentMatcher.appendReplacement(outputBuffer, docComment);
    }

    /**
     * 処理を終了し置換後の Java ソース文字列を取得します。
     * @return ブロックコメント置換後の Java ソース文字列
     */
    public String finishToString() {
        commentMatcher.appendTail(outputBuffer);
        String str = outputBuffer.toString();
        return str;
    }
}
