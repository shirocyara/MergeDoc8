/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * シグネチャです。
 * 
 * <p>このクラスは JDK のコレクションフレームワークで使用するために必要な
 * メソッド equals() や hashcode() が実装されています。
 * 
 * @author Shinji Kashihara
 */
public class Signature {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(Signature.class);

    /** 短い形式のクラス名（パッケージ部分除く） */
    private String shortClassName;

    /** メソッド名 */
    private String methodName;

    /** 引数 */
    private String arguments;

    /** インナークラス宣言判定 */
    private boolean declareInnerClass;

    /**
     * コンストラクタです。
     * @param className このシグネチャが属するクラス名（短い形式でも良い）
     * @param sig シグネチャ文字列
     */
    public Signature(String className, String sig) {

        // 改行をスペースに置換
        sig = sig.replace('\n', ' ');

        // 型引数を除去。入れ子があるため無くなるまで繰り返す。
        // 例）put(List<List<String>> p1, List p2, List<V> p3)
        //  -> put(List p1, List p2, List p3)
        if (sig.contains("<")) {
            Pattern pat = PatternCache.getPattern("<[\\w\\s\\?,]+>");
            for (Matcher mat = pat.matcher(sig); mat.find(); mat = pat.matcher(sig)) {
                sig = mat.replaceAll(" ");
            }
        }
        
        // クラスの拡張部を除去
        sig = FastStringUtils.replaceFirst(sig, "\\s(extends|implements)\\s.*", " ");

        // 先頭、末尾にスペース付加
        sig = " " + sig + " ";

        // "(" ")" "," の前後にスペース付加
        sig = FastStringUtils.replaceAll(sig, "(\\(|\\)|,)", " $1 ");
        
        // 複数スペースをスペース1個に置換
        sig = FastStringUtils.replaceAll(sig, " +", " ");

        // 型がフルクラス名の場合はパッケージ名部分を除去
        // 例) " java.io.Serializable" -> "Serializable"
        sig = FastStringUtils.replaceAll(sig, " (\\w+?\\.)+", " ");

        // 引数がある場合
        if (sig.contains("(")) {

            // final を除去
            sig = FastStringUtils.replace(sig, " final ", " ");

            // "[]" の前のスペース除去
            sig = FastStringUtils.replace(sig, " []", "[]");

            // 引数名を取り除き、型のみにする。
            // 配列の場合、型ではなく変数名に [] がついている場合があるので
            // 型の後ろに [] をつけるように統一する。
            //   ex) public void get(String str[], int num1, int[] num2)
            //    -> public void get(String[],int,int[])
            sig = FastStringUtils.replaceAll(sig, " \\w+?( ,| \\)|\\[\\] (,|\\)))", "$1");
        }

        // 単純クラス名の取得
        this.shortClassName = FastStringUtils.replaceFirst(className, ".+\\.", "");

        // クラスシグネチャの場合の宣言クラス名を取得
        String classRegex = ".*? (class|interface|@interface|enum) (\\w+) .*";
        if (FastStringUtils.matches(sig, classRegex)) {

            // インナークラス宣言の場合はこのシグネチャの属する
            // クラス名としてメンバに上書きする
            String declaClassName = FastStringUtils.replaceFirst(sig, classRegex, "$2");
            if (!declaClassName.equals(shortClassName)) {
                this.declareInnerClass = true;
                this.shortClassName = declaClassName;
            }
        }

        // name(type,,,) の形式に変換
        sig = FastStringUtils.replaceFirst(sig, ".* (\\w+?(| \\(.*?\\)))(|\\[\\]) $", "$1");
        sig = FastStringUtils.replaceAll(sig, " +", "");

        this.methodName = FastStringUtils.replaceFirst(sig, "(\\w+).*", "$1");
        this.arguments = FastStringUtils.replaceFirst(sig, "\\w+(.*)", "$1");
    }
    
    /**
     * インナークラス宣言シグネチャか判定します．
     * @return インナークラス宣言シグネチャの場合は true
     */
    public boolean isDeclareInnerClass() {
        return declareInnerClass;
    }
    
    /**
     * このシグネチャが属するクラス名を取得します．
     * @return このシグネチャが属するクラス名
     */
    public String getClassName() {
        return shortClassName;
    }
    
    /**
     * 他のシグネチャと等しいか比較します．
     * @param obj 他のシグネチャ
     * @return 等しい場合は true 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) return false; 
        return toString().equals(obj.toString());
    }

    /**
     * このオブジェクトのハッシュコードを取得します．
     * @return ハッシュコード
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * このオブジェクトの文字列表現を取得します．
     * @return 属するクラス名#メソッド名(引数型,,,)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return shortClassName + "#" + methodName + arguments;
    }
}
