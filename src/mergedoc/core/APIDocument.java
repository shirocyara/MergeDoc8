/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Javadoc API ドキュメントです。
 * @author Shinji Kashihara
 */
public class APIDocument {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(APIDocument.class);

    /** シグネチャをキーとしたコメントのテーブル */
    private final Map<Signature,Comment> contextTable = new HashMap<Signature,Comment>();
    
    /**
     * see タグや link タグの埋め込みリンクパターン。
     * <pre>
     *   group(1): URL
     *   group(2): ラベル
     * </pre>
     */
    private static final Pattern linkClassPattern = PatternCache.getPattern(
        "(?si)<A\\s+HREF=\"([^\"]+)\"[^>]*><CODE>(.+?)</CODE></A>");

    /**
     * コンストラクタです。
     * @param docDir API ドキュメントディレクトリ
     * @param className クラス名
     * @param charsetName 文字セット名
     * @throws IOException 入出力例外が発生した場合
     */
    public APIDocument(File docDir, String className, String charsetName) throws IOException {

        // API ドキュメント絶対パスを生成
        StringBuilder path = new StringBuilder();
        path.append( docDir.getPath() );
        path.append( File.separator );
        path.append( className.replace('.', File.separatorChar) );
        path.append( ".html" );

        // API ドキュメントファイルのロード        
        File docFile = new CachedFile(path.toString());
        load(docDir, docFile, charsetName);

        // インナークラス API ドキュメントファイルのロード
        // prefix は毎回異なるため PatternCache は使用しない
        String prefix = FastStringUtils.replaceFirst(docFile.getName(), "\\.html$", "");
        Pattern innerClass = Pattern.compile(prefix + "\\..+\\.html$");
        
        for (File f : docFile.listFiles()) {
            if (innerClass.matcher(f.getName()).matches()) {
                load(docDir, f, charsetName);
            }
        }
    }

    /**
     * ファイルクラスのプロキシです。<br>
     * リストキャッシュ機能を持ちます。
     */
    private static class CachedFile extends File {

        private static File cachedDir;
        private static File[] cachedFiles;
        private static final File[] EMPTY_FILES = new File[0]; 

        public CachedFile(String path) {
            super(path);
        }
        public File[] listFiles() {
            File dir = getParentFile();
            if (!dir.equals(cachedDir)) {
                cachedDir = dir;
                cachedFiles = dir.listFiles();
                if (cachedFiles == null) cachedFiles = EMPTY_FILES;
            }
            return cachedFiles;
        }
    }
    
    /**
     * API ドキュメント HTML ファイルを読み込みます。
     * @param docDir API ドキュメントディレクトリ
     * @param docFile API ドキュメントファイル
     * @param charsetName 文字セット名
     * @throws IOException 入出力例外が発生した場合
     */    
    private void load(File docDir, File docFile, String charsetName) throws IOException {

        // 存在しない場合は何もしない
        if (!docFile.exists()) return;
        
        // API ドキュメント読み込み
        InputStream is = new FileInputStream(docFile);
        byte[] buf = new byte[is.available()];
        is.read(buf);
        is.close();
        String docHtml = new String(buf, charsetName);
        docHtml = FastStringUtils.optimizeLineSeparator(docHtml);
        docHtml = docHtml.replace('\t', ' ');

        // WAVE DASH 文字化け回避
        char wavaDash = (char) Integer.decode("0x301c").intValue();
        docHtml = docHtml.replace(wavaDash, '～');
        
        // API ドキュメントファイルパスからクラス名取得
        String className = FastStringUtils.replaceFirst(docFile.getPath(), "\\.html$", "");
        className = className.replace(docDir.getPath() + File.separator, ""); //Patternキャッシュしない
        className = className.replace(File.separatorChar, '.');
        
        // StringBuffer、StringBuilder だけの特殊処理
        if (className.equals("java.lang.StringBuffer") || className.equals("java.lang.StringBuilder")) {
            docHtml = docHtml.replace("%20", "");
        }

        // API ドキュメントのコメント解析
        parseClassComment(className, docHtml);
        parseMethodComment(className, docHtml);
    }

    /**
     * コンテキストが空か判定します。
     * @return コンテキストが空の場合は true
     */
    public boolean isEmpty() {
        return contextTable.isEmpty();
    }

    /**
     * 指定したシグネチャを持つ Javadoc コメントを取得します。
     * @param signature シグネチャ
     * @return Javadoc コメント
     */
    public Comment getComment(Signature signature) {
        Comment comment = contextTable.get(signature);
        return comment;
    }

    /**
     * クラスの Javadoc コメント情報を作成します。
     * author, version タグは Javadoc デフォルトでは存在しないため解析しません。<br>
     * @param className クラス名
     * @param docHtml API ドキュメントソース
     */
    private void parseClassComment(String className, CharSequence docHtml) {

        String baseRegex =
        	"(?si)" +
            "<HR>\\s*" +
            "(|<B>.+?<P>\\s*)" +      //推奨されない
            "<DL>\\s*" +
            "<DT>\\s*" +
            "<PRE>(.+?)</B>.*?" +     //シグネチャ

            "(</DL>\\s*</PRE>\\s*<P>|" +         //通常 Javadoc
            "</PRE>\\s*</DT>\\s*</DL>\\s*<P>)" + //特殊？ StringBuffer など
            "\\s*" +

            "(.+?)\\s*" +             //評価コンテキスト
            "<P>\\s*" +
            "<!-- =";

        Pattern pattern = PatternCache.getPattern(baseRegex);
        Matcher matcher = pattern.matcher(docHtml);

        if (matcher.find()) {

            // シグネチャの作成
            String sigStr = matcher.group(2);
            Signature sig = createSignature(className, sigStr);
            Comment comment = new Comment(sig);

            // deprecated タグ
            String depre = matcher.group(1);
            parseDeprecatedTag(className, depre, comment);

            // 評価コンテキストの取得
            String context = matcher.group(4);

            // コメント本文
            Pattern pat = PatternCache.getPattern(
                "(?si)" +
                "(.+?)\\s*" + // 本文
                "(<P>(|</P>)\\s*){2}");
            Matcher mat = pat.matcher(context);
            if (mat.find()) {
                String body = mat.group(1);
                body = formatLinkTag(className, body);
                comment.setDocumentBody(body);
            }

            // 共通タグ
            parseCommonTag(className, context, comment);

            // debug parseClassComment メソッドのシグネチャ、コメント確認
            //log.debug(sig);
            contextTable.put(sig, comment);
        }
    }
    
    /**
     * メソッドやフィールドの Javadoc コメント情報を作成します。
     * @param className クラス名
     * @param docHtml API ドキュメントソース
     */
    private void parseMethodComment(String className, CharSequence docHtml) {

        // メソッド・フィールドのシグネチャとコメントを抜き出す正規表現
        String baseRegex =
        	"(?si)" +
            "<A NAME=.+?<!-- --></A>" +
            ".+?</H3>\n" +
            "<PRE>\\s*" +
            "(.*?)</PRE>\n" +     //シグネチャ
            "(.+?)(<HR>|<!-- =)"; //評価コンテキスト

        Pattern pattern = PatternCache.getPattern(baseRegex);
        Matcher matcher = pattern.matcher(docHtml);

        // メソッド・フィールドの数でループ
        while (matcher.find()) {

            // シグネチャの作成
            String sigStr = matcher.group(1);
            Signature sig = createSignature(className, sigStr);
            Comment comment = new Comment(sig);

            // 評価コンテキストの取得
            String context = matcher.group(2);

            // コメント本文
            String bodyRegex =
                "(?si)" +
                "<DL>\\s*" +
                "<DD>(.*?)\\s*" + //本文
                "(|</DL>\\s*)" +

                "<P>(|</P>)\\s*" +

                "(</DL>\\s*|" +
                "<DL>\\s*<DT>|" +
                "<DL>\\s*</DL>|" +
                "<DD>\\s*<DL>|" +
                "</DD>\\s*</DL>|" +
                "</DD>\\s*<DD>)";

            Pattern pat = PatternCache.getPattern(bodyRegex);
            Matcher mat = pat.matcher(context);
            if (mat.find()) {
                String body = mat.group(1);
                body = FastStringUtils.replaceFirst(body,
                    "(?si)<B>推奨されていません。.*?(<P>\\s*<DD>|$|</B>(\\s*|&nbsp;)<DD>)", "");
                body = formatLinkTag(className, body);
                comment.setDocumentBody(body);
            }

            // param タグ
            if (context.contains("パラメータ:")) {
                pat = PatternCache.getPattern("(?si)<DT><B>パラメータ:</B>(.+?(</DL>|<DT>|$))");
                mat = pat.matcher(context);
                if (mat.find()) {
                    String items = mat.group(1);
                    Pattern p = PatternCache.getPattern(
                        "(?si)<CODE>(.+?)</CODE> - (.*?)(<DD>|</DD>|</DL>|<DT>|$)");
                    Matcher m = p.matcher(items);
                    while (m.find()) {
                        String name = m.group(1);
                        String desc = formatLinkTag(className, m.group(2));
                        String param = name + " " + desc;
                        comment.addParam(param);
                    }
                }
            }

            // return タグ
            if (context.contains("戻り値:")) {
                pat = PatternCache.getPattern("(?si)<DT><B>戻り値:</B><DD>(.+?)(</DL>|<DT>)");
                mat = pat.matcher(context);
                if (mat.find()) {
                    String str = mat.group(1);
                    str = formatLinkTag(className, str);
                    comment.addReturn(str);
                }
            }

            // throws (exception) タグ
            if (context.contains("例外:")) {
                pat = PatternCache.getPattern("(?si)<DT><B>例外:</B>\\s*(<DD>.+?(</DL>|<DT>|$))");
                mat = pat.matcher(context);
                if (mat.find()) {
                    String items = mat.group(1);
                    Pattern p = PatternCache.getPattern(
                        "(?si)<CODE>(.+?)</CODE>\\s*-\\s*(.*?)(<DD>|</DD>|</DL>|<DT>|$)");
                    Matcher m = p.matcher(items);
                    while (m.find()) {
                        String name = FastStringUtils.replaceAll(m.group(1), "(?si)(<A\\s.+?>|</A>)", "");
                        String desc = m.group(2);
                        desc = formatLinkTag(className, desc);
                        comment.addThrows(name + " " + desc);
                    }
                }
            }

            // deprecated タグ
            parseDeprecatedTag(className, context, comment);

            // 共通タグ
            parseCommonTag(className, context, comment);

            // debug parseMethodComment メソッドのシグネチャ確認
            //log.debug(sig);
            contextTable.put(sig, comment);
        }
    }
    
    /**
     * シグネチャを作成します。
     * @param className クラス名
     * @param sig シグネチャ文字列
     */
    private Signature createSignature(String className, String sig) {
        sig = FastStringUtils.replaceAll(sig, "(?s)<.+?>", " "); // タグ除去
        sig = FastStringUtils.replaceAll(sig, "\\&nbsp;", " ");  // 空白置換
        sig = FastStringUtils.replaceAll(sig, "\\&lt;", "<");    // 型引数開始タグ
        sig = FastStringUtils.replaceAll(sig, "\\&gt;", ">");    // 型引数終了タグ
        sig = FastStringUtils.replaceFirst(sig, "(?s)\\sthrows\\s.*", "");
        Signature signature = new Signature(className, sig);
        return signature;
    }

    /**
     * Javadoc の 共通タグを解析しコメントに追加します。
     * @param className クラス名
     * @param context 評価コンテキスト
     * @param comment コメント
     */
    private void parseCommonTag(String className, String context, Comment comment) {

        // see タグ
        if (context.contains("関連項目:")) {
            Pattern pat = PatternCache.getPattern(
            		"(?si)<DT><B>関連項目:.+?<DD>(.+?)</DL>");
            Matcher mat = pat.matcher(context);

            if (mat.find()) {
                String items = mat.group(1);

                // クラスへの参照
                // linkClassPattern の group(2) はパッケージ情報が無いため
                // group(1) の URL から取得する。
                Matcher linkMatcher = linkClassPattern.matcher(items);
                while (linkMatcher.find()) {
                    String url = linkMatcher.group(1);
                    String ref = formatClassName(className, url);
                    ref = FastStringUtils.replace(ref, "%28", "(");
                    ref = FastStringUtils.replace(ref, "%29", ")");
                    comment.addSee(ref);
                }
            }
        }

        // since タグ
        if (context.contains("導入されたバージョン:")) {
            Pattern pat = PatternCache.getPattern(
            		"(?si)<DT><B>導入されたバージョン:.*?<DD>(.+?)\\s*(</DL>|</DD>)");
            Matcher mat = pat.matcher(context);

            if (mat.find()) {
                comment.addSince(mat.group(1));
            }
        }
    }

    /**
     * Javadoc の deprecated タグを解析しコメントに追加します。
     * @param context 評価コンテキスト
     * @param comment コメント
     */
    private void parseDeprecatedTag(String className, String context, Comment comment) {

        if (context.contains("推奨されていません。")) {
            Pattern pat = PatternCache.getPattern("(?si)<B>推奨されていません。.+?<I>(.+?)</I>");
            Matcher mat = pat.matcher(context);
            if (mat.find()) {
                String str = mat.group(1);
                str = formatLinkTag(className, str);
                comment.addDeprecated(str);
            }
        }
    }
    
    /**
     * HTML の A タグを Javadoc の link タグにフォーマットします。
     * <p>
     * 責務的には Javadoc タグの整形は Comment クラスで行うべきですが、
     * 今のところ、Javadoc の see タグとのからみもあり、このクラスで
     * 処理しています。
     * 
     * @param className クラス名
     * @param html HTML の A タグを含む文字列
     * @return Javadoc link タグ文字列
     */
    private String formatLinkTag(String className, String html) {

        StringBuffer sb = new StringBuffer();
        Matcher linkMatcher = linkClassPattern.matcher(html);

        while (linkMatcher.find()) {
            
            String url = linkMatcher.group(1).trim();
            String label = linkMatcher.group(2).trim();
            String ref = formatClassName(className, url);
            
            StringBuilder link = new StringBuilder();
            link.append("{@link ");
            link.append(ref);
            if (label.length() > 0) {
                
                ref = ref.replace('#', '.');
                if (!ref.endsWith(label)) {
                    link.append(" ");
                    link.append(label);
                }
            }
            link.append("}");
            
            linkMatcher.appendReplacement(sb, link.toString());
        }
        linkMatcher.appendTail(sb);
        html = sb.toString();
        
        return html;
    }

    /**
     * see タグや link タグの URL を package.class#member 形式にフォーマットします。
     * 同一パッケージの場合は package が省略され、同一クラスの場合は class も省略されます。
     * @param className クラス名
     * @param path パス
     * @return package.class#member 形式の文字列
     */
    private String formatClassName(String className, String path) {

        String lastClassName = FastStringUtils.replaceFirst(className, ".+\\.", "");
        String packageName = className.replace("." + lastClassName, ""); //Patternキャッシュしない
        String lastClassPrefix = "\\.([A-Z])";

        path = FastStringUtils.replace(path, ".html", "");
        path = path.replace('/', '.');
        path = FastStringUtils.replaceFirst(path, "^\\.*", "");
        path = FastStringUtils.replaceAll(path, "java.lang" + lastClassPrefix, "$1");
        path = path.replaceAll(packageName + lastClassPrefix, "$1");    //Patternキャッシュしない
        path = path.replaceAll(lastClassName + "#", "#");               //Patternキャッシュしない
        return path;
    }
}
