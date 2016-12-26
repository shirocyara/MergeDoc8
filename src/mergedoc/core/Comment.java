/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ブロックコメントをあらわすクラスです。
 * <p>
 * @author Shinji Kashihara
 */
public class Comment {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(Comment.class);

    /** 出力するコメントのデフォルトの横幅 */
    private static final int DEFAULT_WIDTH = 80;

    //-----------------------------------------
    // 日本語 API ドキュメントから取得する情報
    //-----------------------------------------

    /** シグネチャ */
    private final Signature sig;
    
    /** コメント本文 */
    private String docBody;

    /** deprecated タグコメント */
    private String deprecate;

    /** see タグコメントのリスト */
    private List<String> sees;

    /** since タグコメントのリスト */
    private List<String> sinces;

    /** param タグコメントのリスト */
    private List<String> params;

    /** return タグコメントのリスト */
    private List<String> returns;

    /** throws タグコメントのリスト */
    private List<String> throwses;

    //-----------------------------------------
    // Java ソースから取得する情報
    //-----------------------------------------

    /** 元の Java ソースコメント（飾り付け部分含む） */
    private String srcBody;

    /** author タグコメントのリスト */
    private List<String> srcAuthors;

    /** version タグコメントのリスト */
    private List<String> srcVersions;

    /** serial タグコメントのリスト */
    private List<String> srcSerials;

    /** serialField タグコメントのリスト */
    private List<String> srcSerialFields;

    /** serialData タグコメントのリスト */
    private List<String> srcSerialDatas;

    /** spec タグコメントのリスト（JSR No.記述．非標準タグ） */
    private List<String> srcSpecs;

    /**
     * コンストラクタです。
     * <p>
     * @param sig シグネチャ
     */
    public Comment(Signature sig) {
        this.sig = sig;
    }
    
    /**
     * コメント本文をセットします。
     * <p>
     * @param docBody コメント本文
     */    
    public void setDocumentBody(String docBody) {
        this.docBody = formatHTML(docBody);
    }
    
    /**
     * コメントに含まれる HTML を整形します。
     * <p>
     * @param comment コメント
     * @return HTML 整形後のコメント
     */
    private String formatHTML(String comment) {
        
    	// HTML タグが含まれている可能性があるか
    	boolean hasHtmlTag = comment.contains("<");
    	
        // HTML タグを小文字に
        if (hasHtmlTag) {
            Pattern pat = PatternCache.getPattern("</?\\w+");
            Matcher mat = pat.matcher(comment);
            StringBuffer sb = new StringBuffer();
            while (mat.find()) {
                String tag = mat.group().toLowerCase();
                mat.appendReplacement(sb, tag);
            }
            mat.appendTail(sb);
            comment = sb.toString();
        }
    	
        // 行頭空白を除去
        if (hasHtmlTag && comment.contains("<pre>")) {
        	// 今のところ、不要と思われる
        } else {
            comment = FastStringUtils.replaceAll(comment, "(?m)^ +", "");
        }
        
        // HTML タグの整形
        if (hasHtmlTag) {
            
            // </p> 除去
            comment = FastStringUtils.replace(comment, "</p>", "");
            
            // <blockquote> 整形
            comment = FastStringUtils.replaceAll(comment, "\\s*(</?blockquote>)\\s*", "\n$1\n");
            
            // <pre> 整形
            if (comment.contains("<pre>")) {
                comment = FastStringUtils.replaceAll(comment, "\\s*(</?pre>)\\s*", "\n$1\n");
                comment = FastStringUtils.replaceAll(comment, "(<blockquote>)\n(<pre>)", "$1$2");
                comment = FastStringUtils.replaceAll(comment, "(</pre>)\n(</blockquote>)", "$1$2");
            }
            
            // <table> 整形
            if (comment.contains("<table")) {
                comment = FastStringUtils.replaceAll(comment, "\\s*(</?table|</?tr>)", "\n$1");
                comment = FastStringUtils.replaceAll(comment, "\\s*(<(th|td))", "\n  $1");
                comment = FastStringUtils.replaceAll(comment, "\\s*(<blockquote>)\n(<table)", "\n\n$1$2");
                comment = FastStringUtils.replaceAll(comment, "(</table>)\n(</blockquote>)", "$1$2");
            }
            
            // <ol> <ul> <li> 整形
            comment = FastStringUtils.replaceAll(comment, "\\s*(</?(ol|ul|li)>)", "\n$1");
            
            // <p> 整形
            if (comment.contains("<p>")) {
                comment = FastStringUtils.replaceAll(comment, "\\s*(<p>)\\s*", "\n\n$1");
                comment = FastStringUtils.replaceAll(comment, "(\\s*<p>)+$", "");
            }
            
            // <br/> 整形
            if (comment.contains("<br")) {
                comment = FastStringUtils.replaceAll(comment, "<br\\s*/>", "<br>");
            }
        }
        
        // 先頭と末尾の余分な改行と空白を除去
        comment = comment.trim();
        
        return comment;
    }
    
    /**
     * deprecated タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addDeprecated(String comment) {
        comment = formatHTML(comment);
        deprecate = comment;
    }

    /**
     * see タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addSee(String comment) {
        if (sees == null) sees = new LinkedList<String>();
        sees.add(comment);
    }

    /**
     * since タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addSince(String comment) {
        if (sinces == null) sinces = new LinkedList<String>();
        sinces.add(comment);
    }

    /**
     * param タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addParam(String comment) {
        if (params == null) params = new LinkedList<String>();
        comment = formatHTML(comment);
        params.add(comment);
    }

    /**
     * return タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addReturn(String comment) {
        if (returns == null) returns = new LinkedList<String>();
        comment = formatHTML(comment);
        returns.add(comment);
    }

    /**
     * throws タグコメントを追加します。
     * <p>
     * @param comment コメント
     */    
    public void addThrows(String comment) {
        if (throwses == null) throwses = new LinkedList<String>();
        comment = formatHTML(comment);
        throwses.add(comment);
    }

    /**
     * 元の Java ソースコメントをセットします。
     * これには飾り付け部分が含まれます。
     * <p>
     * @param srcBody 元の Java ソースコメント
     */
    public void setSourceBody(String srcBody) {
        
        // @exception を @throws に置換
    	this.srcBody = FastStringUtils.replaceAll(
    			srcBody, "\\s@exception\\s", " @throws ");
        
        // author タグの内容リスト作成（タグの値に改行あり）
        srcAuthors      = createWrapTagList("@author");
        
        // throws タグの内容リスト作成（タグの値に改行あり）
        // {@inheritDoc} が指定されている場合は、API ドキュメントの
        // 内容を使用せず、{@inheritDoc} として上書きする。
        List<String> srcThrowses = createWrapTagList("@throws");
        if (srcThrowses != null && throwses != null) {
            for (String src : srcThrowses) {
            	
            	Pattern pat = PatternCache.getPattern("(?s)(\\w+)\\s+(.*)");
            	Matcher mat = pat.matcher(src);
            	
            	if (mat.find() && mat.group(2).contains("@inheritDoc")) {
            		
            		String exceptionClassName = mat.group(1);
            		
            		for (int i = 0; i < throwses.size(); i++) {
            			String doc = throwses.get(i);
            			if (doc.startsWith(exceptionClassName)) {
            				throwses.set(i, exceptionClassName + " {@inheritDoc}");
            			}
            		}
            	}
            }
        }
        
        // その他のタグの内容リスト作成（タグの値に改行なし）
        srcVersions     = createTagList("@version");
        srcSerials      = createTagList("@serial");
        srcSerialFields = createTagList("@serialField");
        srcSerialDatas  = createTagList("@serialData");
        srcSpecs        = createTagList("@spec");
    }
    
    /**
     * 元の Java ソースコメントから指定した Javadoc タグを探し、その値の
     * リストを作成します。タグの値に改行が含まれる可能性があるものが対象です。
     * <p>
     * @param tagName タグ名
     * @return タグの値リスト
     */
    private List<String> createWrapTagList(String tagName) {
        
        List<String> tagValues = null;
        if (srcBody.contains(tagName)) {
        	
            String undeco = FastStringUtils.replaceAll(srcBody, "(?m)^ *\\* *", "");
            Pattern pat = PatternCache.getPattern("(?s)" + tagName + " *(.*?)([^\\{]@\\w+|/\\s*$)");
            Matcher mat = pat.matcher(undeco);
            for (int start = 0; mat.find(start); start = mat.end(1)) {
                if (tagValues == null) tagValues = new LinkedList<String>();
                tagValues.add(mat.group(1));
            }
        }
        return tagValues;
    }
    
    /**
     * 元の Java ソースコメントから指定した Javadoc タグを探し、その値の
     * リストを作成します。タグの値に改行がないものが対象です。
     * <p>
     * @param tagName タグ名
     * @return タグの値リスト
     */
    private List<String> createTagList(String tagName) {
        
        List<String> tagValues = null;
        if (srcBody.contains(tagName)) {
            
            Pattern pat = PatternCache.getPattern(" +" + tagName + " *(.*)\n");
            Matcher mat = pat.matcher(srcBody);
            while (mat.find()) {
                if (tagValues == null) tagValues = new LinkedList<String>();
                tagValues.add(mat.group(1));
            }
        }
        return tagValues;
    }
    
    /**
     * 設定された情報を元にコメントをビルドします。
     * <p>
     * @return ビルドしたコメント
     */
    public String buildComment() {
        
        if (srcBody == null) {
            throw new IllegalStateException(
            "Source comment is null. require #setSourceComment.");
        }
        
        // Java ソースに @deprecated が含まれない場合は削除。
        // API ドキュメントはクラスが @deprecated であれば自動的に
        // すべてのメソッドに付加されてしまっているため。
    	if (!srcBody.contains("@deprecated")) {
    		deprecate = null;
    	}
        
        // 元 Java ソースのコメントにボディ部が無い（省略によるコメント継承）場合、
        // タグの数が API ドキュメントのより Java ソースのが少ないものを調整
        if (FastStringUtils.matches(srcBody, "(?s)\\s*/\\*\\*[\\s\\*]*@.*")) {
        	docBody = null;
        	params  = omitTags(params, "@param");
        	returns = omitTag(returns, "@return");
        	throwses = omitTags(throwses, "@throws");
        	sees    = omitTags(sees, "@see");
        }
        
        // 元 Java ソースのコメントから行数とインデント取得
        int decoSize = 2;
        int originDecoHeight = FastStringUtils.heightOf(srcBody);
        if (originDecoHeight <= 0) {
            throw new IllegalStateException(
            "Illegal comment height " + originDecoHeight + "\n" + srcBody);
        }
        int originHeight = originDecoHeight;
        if (originDecoHeight > decoSize) {
            originHeight = originDecoHeight - decoSize;
        }
        String indent = FastStringUtils.replaceFirst(srcBody, "(?s)^( *?)/\\*\\*.*", "$1");
    	
    	// API ドキュメントのコメント内の pre タグ内容を Java ソースのものに置換
        if (docBody != null) {
        	replacePreBody();
        }
        
        // 元 Java ソースの飾り付けを含むコメント行数が 2 行以下の場合
        if (originDecoHeight <= 2) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent);
            sb.append("/** ");
            if (docBody != null && docBody.length() > 0) {
                String str =  FastStringUtils.replaceAll(docBody, "\n", "");
                sb.append(str);
            } else if (sinces != null && sinces.size() > 0) {
                sb.append("@since ");
                String str = sinces.get(0);
                str = FastStringUtils.replace(str, "\n", "");
                sb.append(str);
            }
            if (originDecoHeight == 2) {
                sb.append("\n");
                sb.append(indent);
            }
            sb.append(" */\n");
            return sb.toString();
        }

        // 複数行コメントの作成    
        int width = DEFAULT_WIDTH - indent.length() - OutputComment.LINE_PREFIX.length();
        OutputComment o = new OutputComment(originHeight, width);
        String decoComment = o.toString();
        if (decoComment.length() > 0) {
            if (o.resultHeight() != o.originHeight) {
                decoComment = resizeComment(o, decoComment);
            }
            decoComment = FastStringUtils.replaceAll(decoComment, "(?m)^", indent);
            decoComment = FastStringUtils.replaceAll(decoComment, "(?m)^ +$", "");
        }

        return decoComment;
    }
    
    /**
     * Java ソース上ではスーパークラスやインタフェースに Javadoc
     * コメントがあれば省略することが可能ですが、API ドキュメントには
     * すべて出力されています。よって、元々 Java ソースに無いタグを
     * API ドキュメントから取り込んでしまわないように
     * API ドキュメントにタグがあっても Java ソースもタグが存在しなければ
     * null を返します。
     * <p>
     * タグが複数存在する可能性がある場合は {@link #omitTags(List, String)}
     * を使用してください。
     * <p>
     * @param docTagList API ドキュメントのタグリスト
     * @param tagName タグ名文字列
     * @return 調整後のタグリスト
     */
    private List<String> omitTag(List<String> docTagList, String tagName) {
    	
    	if (docTagList != null && docTagList.size() > 0) {
    		
            List<String> srcTagList = createWrapTagList(tagName);
            if (srcTagList == null || srcTagList.size() == 0) {
            	return null;
            }
    	}
    	return docTagList;
    }
    
    /**
     * Java ソース上ではスーパークラスやインタフェースに Javadoc
     * コメントがあれば省略することが可能ですが、API ドキュメントには
     * すべて出力されています。よって、元々 Java ソースに無いタグを
     * API ドキュメントから取り込んでしまわないように
     * API ドキュメントのタグ数と Java ソースのタグ数を比較し、
     * API ドキュメントのほうが少なければ、Java ソースのタグ数に合わせます。
     * <p>
     * @param docTagList API ドキュメントのタグリスト
     * @param tagName タグ名文字列
     * @return 調整後のタグリスト
     */
    private List<String> omitTags(List<String> docTagList, String tagName) {
    	
    	if (docTagList != null && docTagList.size() > 0) {
    		
            List<String> srcTagList = createWrapTagList(tagName);
            if (srcTagList == null || srcTagList.size() == 0) {
            	return null;
            }
        	if (docTagList.size() > srcTagList.size()) {
        		List<String> names = new ArrayList<String>();
        		for (String src : srcTagList) {
					String name =
						FastStringUtils.replaceFirst(src, "(?s)^(\\S+)\\s.*$", "$1");
					names.add(name);
				}
        		List<String> newTagList = new ArrayList<String>();
        		for (String doc : docTagList) {
					String name =
						FastStringUtils.replaceFirst(doc, "(?s)^(\\S+)\\s.*$", "$1");
					if (names.contains(name)) {
						newTagList.add(doc);
					}
				}
        		return newTagList;
        	}
    	}
    	return docTagList;
    }

    /**
     * JDK1.5 日本語 API ドキュメントの不具合？で pre HTML タグ内の値は
     * 改行情報が欠落している場合があるため、Java ソースコメントのものを使用します。
     * ただし、pre タグの個数が一致しない場合は、そのまま API ドキュメントの
     * 値を使用します。
     */
    private void replacePreBody() {
    	
    	// pre タグが含まれない場合は何もしない
    	if (!docBody.contains("<pre>")) {
    		return;
    	}
    	
    	// Java ソースコメントから pre タグの値を取得
        LinkedList<String> pres = null;
        String commentBody = FastStringUtils.replaceAll(srcBody, "(?m)^\\s*\\*( |)", "");
        Pattern pat = PatternCache.getPattern("(?s)(<pre>\n)(.+?)(\n</pre>)");
        Matcher mat = pat.matcher(commentBody);
        while (mat.find()) {
            if (pres == null) {
                pres = new LinkedList<String>();
            }
            pres.add(mat.group(2));
        }
        if (pres == null) return;
        
        // API ドキュメント説明の pre タグの値に Java ソースの内容を上書き
        Matcher descMatcher = pat.matcher(docBody);
        StringBuffer sb = new StringBuffer();
        while (descMatcher.find()) {
        	
        	// pre タグの数が一致しないため何もしない
            if (pres.size() == 0) {
            	return;
        	}
            String value = FastStringUtils.quoteReplacement(pres.removeFirst());
            descMatcher.appendReplacement(sb, "$1" + value + "$3");
        }
        descMatcher.appendTail(sb);
        
        // pre タグの数が一致する場合のみ反映
        if (pres.size() == 0) {
            docBody = sb.toString();
        }
    }

    /**
     * コメント構築結果を保持するクラスです。 
     */
    private class OutputComment {

        final static String LINE_PREFIX = " * ";
        final int originHeight;
        final int initWidth;
        int width;
        String comment;
        boolean enabledFirstLine;
        
        OutputComment(int originHeight, int width) {
            this.originHeight = originHeight;
            this.initWidth = width;
            this.width = width;
            build();
        }
        
        void build() {
            this.comment = formatComment(width, originHeight);
        }
        
        void rebuild() {
            resetWidth();
            build();
        }
        
        int resultHeight() {
            if (enabledFirstLine) {
                return FastStringUtils.heightOf(comment) - 1;
            } else {
                return FastStringUtils.heightOf(comment);
            }
        }

        void resetWidth() {
            width = initWidth;
        }
        
        public String toString() {
            String str = comment;
            if (str.length() > 0) {
                StringBuilder sb = new StringBuilder();
                str = FastStringUtils.replaceAll(str, "(?m)^", LINE_PREFIX);
                if (enabledFirstLine) {
                    sb.append( "/**" );
                    str = FastStringUtils.replaceFirst(str, "^ \\*", "");
                } else {
                    sb.append( "/**\n" );
                }
                sb.append( str );
                sb.append( " */\n" );
                str = sb.toString();
            }
            return str;
        }
    }
    
    /**
     * コメントサイズを調整します。
     * 
     * @param o コメント構築結果
     * @return サイズ調整済みのコメント（飾り付け含む）
     */
    private String resizeComment(OutputComment o, String decoComment) {

        // 作成したコメントが元ソースコメント行数より多い場合はコメントを小さくする
        if (o.resultHeight() > o.originHeight) {
            shrinkComment(o);
            
            // 小さくならない場合は
            // docBody の <pre> タグ外の改行をすべて除去し、再構築。
            if (docBody != null
            		&& o.resultHeight() > o.originHeight
            		&& docBody.contains("\n")) {
            	
                StringBuilder sb = new StringBuilder();
                boolean inPreTag = false;
                for (int i = 0; i < docBody.length(); i++) {
                    char c = docBody.charAt(i);
                    sb.append(c);
                    String buf = sb.toString();
                    if (buf.endsWith("<pre>")) {
                        inPreTag = true;
                        sb.insert(sb.length() - 5, '\n');
                    } else if (buf.endsWith("</pre>")) {
                        inPreTag = false;
                        sb.append('\n');
                    }
                    if (c == '\n' && !inPreTag) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                }
                docBody = sb.toString();
                
                o.rebuild();
                if (o.resultHeight() > o.originHeight) {
                    shrinkComment(o);
                }
            }
            
            // まだ小さくならない場合は説明の 1 つ目の「。」より後を削除して再構築。
            // 今のところ JDK5.0 API ドキュメントで shrinkComment メソッドで
            // オリジナルより小さく出来ないのは
            // BasicEditorPaneUI#installUI(JComponent) のひとつだけ。
            if (docBody != null
            		&& o.resultHeight() > o.originHeight) {
                
                int pos = docBody.indexOf('。');
                if (pos != -1) {
                    docBody = docBody.substring(0, pos + 1);
                    
                    o.rebuild();
                    if (o.resultHeight() > o.originHeight) {
                        shrinkComment(o);
                    }
                }
            }
            
            // まだ小さくならない場合...
            if (o.resultHeight() > o.originHeight) {
                
                // JDK5.0 API ドキュメントではここは通らない
                log.warn(sig
                		+ " 行数調整不可のためマージ出来ませんでした。\n"
                		+ "-------------------------------------------------\n"
                		+ "英語コメント:\n" + srcBody
                		+ "\n日本語コメント:\n" + o.toString()
                		+ "-------------------------------------------------\n");
                return srcBody;
            }
            decoComment = o.toString();
        }
        
        // 作成したコメントが元ソースコメント行数より少ない場合はコメントを大きくする
        if (o.resultHeight() < o.originHeight) {
            expandComment(o);
            decoComment = o.toString();
        }
        
        // まだ足りない場合は上部に空行を埋める。
        if (o.resultHeight() < o.originHeight) {
            int sub = o.originHeight - o.resultHeight();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sub; i++) {
                sb.append("\n");
            }
            decoComment = sb + decoComment;
        }
        
        return decoComment;
    }

    /**
     * コメントを小さくします。
     * <p>
     * @param o 出力コメント
     */
    private void shrinkComment(OutputComment o) {

        // 説明が元 Java ソースコメントに無い場合は除去
        String emptyDescRegex = "(?s)\\s*/\\*\\*\\s*\\*\\s*@.*";
        if (FastStringUtils.matches(srcBody, emptyDescRegex)) {
            docBody = null;
            o.build();
            if (o.resultHeight() <= o.originHeight) return;
        }

        // タグが元 Java ソースコメントに無い場合は除去
        // -> buildComment の最初でも行っているが、ここでは本文の有無関係なしで行う
        boolean b1 = shrinkTagList("@see", sees);
        boolean b2 = shrinkTagList("@throws", throwses);
        boolean b3 = shrinkTagList("@param", params);
        boolean b4 = shrinkTagList("@return", returns);
        if (b1 || b2 || b3 || b4) {
            o.build();
            if (o.resultHeight() <= o.originHeight) return;
        }
        
        // 高さ（行数）
        int height = o.resultHeight();
        
        // 連続する <p> タグをひとつに
        Pattern pTagPat = PatternCache.getPattern("<p>\n\n(<p>)");
        if (o.comment.contains("<p>\n\n<p>")) {
            StringBuffer sb = new StringBuffer();
            Matcher pTagMat = pTagPat.matcher(o.comment);
            while (height > o.originHeight && pTagMat.find()) {
                pTagMat.appendReplacement(sb, "$1");
                height -= 2;
            }
            pTagMat.appendTail(sb);
            o.comment = sb.toString();
            if (height <= o.originHeight) return;
        }
        
        // <th、<td、</tr タグの前の改行を除去
        Pattern tdTagPat = PatternCache.getPattern("\\s+(<(t[hd]|/tr))");
        if (o.comment.contains("<table")) {
            StringBuffer sb = new StringBuffer();
            Matcher tdTagMat = tdTagPat.matcher(o.comment);
            while (height > o.originHeight && tdTagMat.find()) {
                tdTagMat.appendReplacement(sb, "$1");
                height--;
            }
            tdTagMat.appendTail(sb);
            o.comment = sb.toString();
            if (height <= o.originHeight) return;
        }
        
        // <li、</ul、</ol タグの前の改行を除去
        Pattern liTagPat = PatternCache.getPattern("\\s+(<(li|/[uo]l))");
        if (o.comment.contains("<li")) {
            StringBuffer sb = new StringBuffer();
            Matcher liTagMat = liTagPat.matcher(o.comment);
            while (height > o.originHeight && liTagMat.find()) {
                liTagMat.appendReplacement(sb, "$1");
                height--;
            }
            liTagMat.appendTail(sb);
            o.comment = sb.toString();
            if (height <= o.originHeight) return;
        }

        // 空行を削除
        Pattern emptyLinePat = PatternCache.getPattern("(?m)^\\s*?\n");
        Matcher emptyLineMat = emptyLinePat.matcher(o.comment);
        StringBuffer sb = new StringBuffer();
        while (height > o.originHeight && emptyLineMat.find()) {
            emptyLineMat.appendReplacement(sb, "");
            height--;
        }
        emptyLineMat.appendTail(sb);
        o.comment = sb.toString();
        if (height <= o.originHeight) return;
        
        // 元 Java ソースコメントの 1 行目から説明がある場合
        String firstLineRegex = "(?s)\\s*/\\*\\*\\s*\n.*";
        if (!FastStringUtils.matches(srcBody, firstLineRegex)) {
            o.enabledFirstLine = true;
            if (o.resultHeight() <= o.originHeight) return;
        }

        // 横幅を増やして再ビルド
        final int maxWidth = 160;
        while (o.resultHeight() > o.originHeight && o.width < maxWidth) {
            
            o.build();
            if (o.resultHeight() <= o.originHeight) return;

            if (o.comment.contains("<")) {
                
                o.comment = pTagPat.matcher(o.comment).replaceAll("$1");
                if (o.resultHeight() <= o.originHeight) return;

                o.comment = tdTagPat.matcher(o.comment).replaceAll("$1");
                if (o.resultHeight() <= o.originHeight) return;

                o.comment = liTagPat.matcher(o.comment).replaceAll("$1");
                if (o.resultHeight() <= o.originHeight) return;
            }

            o.comment = emptyLinePat.matcher(o.comment).replaceAll("");
            if (o.resultHeight() <= o.originHeight) return;

            if (o.width < 100) {
                o.width += 4;
            } else {
                o.width += 8;
            }
        }
    }

    /**
     * 指定したタグが Java ソースコメントに無い場合はタグリストをクリアします。
     * <p>
     * @param tagRegex タグ正規表現
     * @param tagList タグリスト
     * @return タグリストをクリアした場合は true
     */
    private boolean shrinkTagList(String tagRegex, List<String> tagList) {
        if (tagList != null) {
            int tagCount = FastStringUtils.split(srcBody, "\\s" + tagRegex + "\\s", -1).length - 1;
            if (tagCount == 0) {
                tagList.clear();
                return true;
            }
        }
        return false;
    }
    
    /**
     * コメントを大きくします。
     * <p>
     * @param o 出力コメント
     */
    private void expandComment(OutputComment o) {
        
        // HTML タグが含まれない場合は何もしない
        if (!o.comment.contains("<")) {
            return;
        }
        
        // 高さ（行数）
        int height = o.resultHeight();
        
        // <pre>、<blockquote>、<ol>、<ul> の上に空行追加
        StringBuffer sb = new StringBuffer();
        Pattern pat = PatternCache.getPattern("([^\n])(\n(<blockquote>)?<pre>|\n<(blockquote|ol|ul)>)");
        Matcher mat = pat.matcher(o.comment);
        while (height < o.originHeight && mat.find()) {
            mat.appendReplacement(sb, "$1\n$2");
            height++;
        }
        mat.appendTail(sb);
        o.comment = sb.toString();
        if (height == o.originHeight) return;
        
        // </pre>、</blockquote>、</ol>、</ul> の下に空行追加
        sb = new StringBuffer();
        pat = PatternCache.getPattern("(</pre>(</blockquote>)?\n|</(blockquote|ol|ul)>\n)([^\n])");
        mat = pat.matcher(o.comment);
        while (height < o.originHeight && mat.find()) {
            mat.appendReplacement(sb, "$1\n$4");
            height++;
        }
        mat.appendTail(sb);
        o.comment = sb.toString();
        if (height == o.originHeight) return;
    }
    
    /**
     * 指定した横幅でコメントを組み立てます。
     * <p>
     * @param width 横幅
     * @param originHeight オリジナル行数
     * @return 組み立てたコメント
     */
    private String formatComment(int width, int originHeight) {

        StringBuilder sb = new StringBuilder();

        // 説明の組み立て
        if (docBody != null && docBody.length() > 0) {
            if (originHeight == 1) {
                sb.append( FastStringUtils.replaceAll(docBody, "\n", "") );
                sb.append( "\n" );
                return sb.toString();
            }
            sb.append( adjustWidth(docBody, width) );
            sb.append( "\n" );
        }

        // deprecated タグの組み立て
        if (deprecate != null && deprecate.length() > 0) {
            String depre = "@deprecated " + deprecate;
            sb.append( adjustWidth(depre, width) );
            sb.append( "\n" );
        }

        appendTo("@author  ",    srcAuthors,      sb, width);
        appendTo("@version ",    srcVersions,     sb, width);

        // param タグの組み立て
        if (params != null && params.size() > 0) {
            
            // name の文字数をカウント
            int paramsSize = params.size();
            int[] nameLens = new int[paramsSize];
            int nameLenMax = 3;
            final int nameLenLimit = 12;

            for (int i = 0; i < paramsSize; i++) {
				String comment = params.get(i);
                String name = FastStringUtils.replaceFirst(comment, "(?s)(\\w+)\\s.*", "$1");
                nameLens[i] = name.length();
                if (nameLens[i] > nameLenMax && nameLens[i] < nameLenLimit) {
                    nameLenMax = nameLens[i];
                }
			}

            // 2 行目以降のインデントを作成
            String tag = "@param   ";
            int indentCnt = tag.length() + nameLenMax + 1;
            StringBuilder indent = new StringBuilder(indentCnt);
            for ( ;indentCnt>0; indentCnt--) indent.append(' ');

            // バッファに出力
            for (int i = 0; i < paramsSize; i++) {

                int spaceCnt = nameLenMax - nameLens[i];
                StringBuilder space = new StringBuilder();
                for ( ;spaceCnt>0; spaceCnt--) space.append(' ');
                
                String comment = params.get(i);
                comment = FastStringUtils.replaceFirst(comment,
                    "(?s)(\\w+)\\s+(.*)", "$1" + space + " $2");
                comment = adjustWidth(comment, width - tag.length());
                StringTokenizer st = new StringTokenizer(comment, "\n");
            
                // 1 行目
                sb.append(tag);
                if (st.hasMoreTokens()) {
                    sb.append(st.nextToken());
                }
                sb.append("\n");

                // 2 行目以降
                while (st.hasMoreTokens()) {
                    sb.append(indent);
                    sb.append(st.nextToken());
                    sb.append("\n");
                }
            }
        }

        appendTo("@return  ",    returns,      sb, width);
        appendTo("@throws  ",    throwses,      sb, width);
        appendTo("@serialField", srcSerialFields, sb, width);
        appendTo("@serialData",  srcSerialDatas,  sb, width);
        appendTo("@see     ",    sees,         sb, width);
        appendTo("@since   ",    sinces,       sb, width);
        appendTo("@serial  ",    srcSerials,      sb, width);
        appendTo("@spec    ",    srcSpecs,        sb, width);
        
        String str = sb.toString();
        str = FastStringUtils.replaceFirst(str, "\n\n$", "\n");
        
        return str;
    }
    
    /**
     * タグコメントのリストを文字列バッファに追加します。 
     * 複数行になる場合はインデントします。
     * <p>
     * @param tag タグ文字列
     * @param tagList タグコメントのリスト
     * @param sb 文字列バッファ
     * @param width 横幅
     */    
    private void appendTo(String tag, List<String> tagList, StringBuilder sb, int width) {

        if (tagList == null) return;

        for (String comment : tagList) {
            
            comment = adjustWidth(comment, width - tag.length());
            StringTokenizer st = new StringTokenizer(comment, "\n");
            
            sb.append(tag);
            if (st.hasMoreTokens()) {
                sb.append(st.nextToken());
            }
            sb.append("\n");

            while (st.hasMoreTokens()) {
                sb.append(FastStringUtils.replaceAll(tag, ".", " "));
                sb.append(st.nextToken());
                sb.append("\n");
            }
        }
    }
    
    /**
     * 改行を含む文字列の横幅を調整します。
     * <p>
     * @param value 調整する文字列
     * @param width 折り返し幅（バイト）
     * @return 横幅を調整した文字列
     */    
    private String adjustWidth(String value, int width) {

        // 1 行だけの場合
        if (value.getBytes().length < width) {
            return value + "\n";
        }

        List<String> lineValues = FastStringUtils.splitLine(value);
        StringBuilder resultBuf = new StringBuilder();
        boolean preTagArea = false;
        final int longWordWidth = width - 20;

        // 行単位で解析。ただし pre タグ内はスルー。
        // 今のところ行頭と行末の pre タグのみ対応。
        for (String lineValue : lineValues) {
            
            if (lineValue.equals("")) {
                resultBuf.append("\n");
                continue;
            }
            
            // pre タグ判定
            if (lineValue.startsWith("</pre>")) {
                preTagArea = false;
            } else if (lineValue.startsWith("<pre>" )) {
                preTagArea = true;
            }
            if (preTagArea) {
                resultBuf.append(lineValue);
                resultBuf.append("\n");
                if (lineValue.endsWith("</pre>")) preTagArea = false;
                continue;
            }
            if (lineValue.endsWith("<pre>" )) preTagArea = true;

            // 横幅が収まる場合
            if (lineValue.getBytes().length < width) {
                resultBuf.append(lineValue);
                resultBuf.append("\n");
                continue;
            }
            
            // <table タグがある場合は summary 属性の前で改行
            if (lineValue.contains("<table")) {
                String s = FastStringUtils.replaceFirst(lineValue, "(?i)\\s(summary=)", "\n$1");
                resultBuf.append(s);
                resultBuf.append("\n");
                continue;
            }
            
            // 長い英数字文字列は先頭に改行を付加
            String multiLineValue = FastStringUtils.replaceAll(lineValue,
                "\\s?(\\p{Graph}{" + longWordWidth + ",})", "\n$1");

            StringTokenizer st = new StringTokenizer(multiLineValue, "\n");
            while (st.hasMoreTokens()) {
                lineValue = st.nextToken();
                wrap(lineValue, resultBuf, width);
            }

        }
        return resultBuf.toString();
    }
    
    /**
     * 単一行の文字列を指定した折り返し幅になるように改行を挿入します。
     * 入力行の値により最大折り返し幅を超える場合があります。
     * <p>
     * @param lineValue 入力行
     * @param resultBuf 横幅を調整した結果文字列を追加するバッファ
     * @param width 折り返し幅（バイト）
     */    
    private void wrap(String lineValue, StringBuilder resultBuf, int width) {
        
        final int minWidth = width - 10;
        final int maxWidth = width + 10;
        final int ADJUST_SKIP_WIDTH = width + 4;
        final int lastPos = lineValue.length() - 1;
        final String PUNCTS = "。、」・)}";
        final String PARTICLES = "はがのをにへとらてるや";

        StringBuilder buf = new StringBuilder();
        int bufLen = 0;
        for (int pos = 0; pos < lastPos; pos++) {

            if (bufLen == 0) {
                String after = lineValue.substring(pos, lastPos);
                int afterLen = after.getBytes().length;
                if (afterLen <= ADJUST_SKIP_WIDTH) {
                    buf.append(after);
                    break;
                }
            }

            char c = lineValue.charAt(pos);
            int cLen = String.valueOf(c).getBytes().length;
            bufLen += cLen;
            boolean isChangeLine = false;

            if (bufLen > minWidth) {
                // 最小折り返し幅を超えている場合は句読点などで改行を挿入
                
                if (c == ' ') {

                    isChangeLine = true;
                    buf.append('\n');

                } else if (PUNCTS.indexOf(c) != -1 || PARTICLES.indexOf(c) != -1) {
                	
                    char next = lineValue.charAt(pos + 1);
                    if (PUNCTS.indexOf(next) == -1 && next != ' ' && next != '.') {
                        
                        isChangeLine = true;
                        buf.append(c);
                        buf.append('\n');
                    }

                } else if (bufLen > width) {
                    // 通常折り返し幅を超えている場合は改行を挿入
                    // ただし現在の文字が半角英数字の場合を除く
                        
                    if (c == '<' || cLen > 1) {
                        
                        isChangeLine = true;
                        buf.append('\n');
                        buf.append(c);
                        
                    } else if (bufLen > maxWidth) {
                        // 最大折り返し幅を超えている場合は
                        // 全角文字まで戻って改行を挿入
                        
                        for (int bPos = buf.length() - 1; bPos > 0; bPos--) {
                            char bc = buf.charAt(bPos);
                            
                            if (bc == ' ') {
                                buf.replace(bPos, bPos+1, "\n");
                                bufLen = buf.substring(bPos+1).getBytes().length;
                                break;
                                
                            } else {
                                
                                int bcLen = String.valueOf(bc).getBytes().length;
                                if (bcLen > 1) {
                                    buf.insert(bPos+1, '\n');
                                    bufLen = buf.substring(bPos+2).getBytes().length;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
                
            if (isChangeLine) {
                resultBuf.append(buf);
                buf = new StringBuilder();
                bufLen = 0;
            } else {
                buf.append(c);
            }
        }
        buf.append(lineValue.charAt(lastPos));

        resultBuf.append(buf);
        resultBuf.append('\n');
    }
}
