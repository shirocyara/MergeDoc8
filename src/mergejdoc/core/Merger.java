/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved. This program are made available under
 * the terms of the Common Public License v1.0. Copyright (c) 2017- kuro-neko.
 */
package mergejdoc.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * マージを行うクラスです。
 * 
 * @author Shinji Kashihara
 */
public class Merger {

  /** ロガー */
  private static final Logger logger = LogManager.getLogger(Merger.class);

  /** クラス種類（class|interface|@interface|enum） */
  private String classKind;

  /** クラス名（パッケージを含む） */
  private String className;

  /** API ドキュメントディレクトリ */
  private File docDirectory;

  /** API ドキュメントエンコーディング */
  private String docEncoding = System.getProperty("file.encoding");

  /** クラス種類定数 （class|interface|@interface|enum） */
  private static final List<String> CLASS_KIND;
  static {
    List<String> list = new ArrayList<String>();
    list.add("class");
    list.add("interface");
    list.add("@interface");
    list.add("enum");
    CLASS_KIND = Collections.unmodifiableList(list);
  }

  /**
   * コンストラクタです。
   * 
   * @param docDirectory
   */
  public Merger(File docDirectory) {
    this.docDirectory = docDirectory;
  }

  /**
   * API ドキュメントエンコーディングを設定します。 設定されなかった場合はデフォルトエンコーディングを使用します。
   * 
   * @param docEncoding API ドキュメントエンコーディング
   */
  public void setDocEncoding(String docEncoding) {
    this.docEncoding = docEncoding;
  }

  /**
   * Java ソースと Javadoc コメントをマージします。 Java ソースに package 宣言が無い場合や、対応する API ドキュメントが見つからない場合はそのまま Java
   * ソースを返します。
   * 
   * @param javaSource Java ソース文字列
   * @return マージ後の Java ソース文字列
   * @throws IOException 入出力例外が発生した場合
   */
  public String merge(String source) throws IOException {

    scanClassName(source);
    if (className == null)
      return source;
    APIDocument apiDoc = new APIDocument(docDirectory, className, docEncoding);
    if (apiDoc.isEmpty())
      return source;

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
   * Java ソース文字列をスキャンし、クラス名（パッケージを含む）を設定します。 package 宣言が無い場合は常に null になります。
   * 
   * @param source Java ソース文字列
   */
  private void scanClassName(String source) {
    // Java ソース内のコメントを削除
    String src = delComment(source).trim();
    // ソース内の改行を削除。
    src = src.replace("\n", " ");

    String packageName = searchPackage(src);
    if (!packageName.equals("")) {
      String tmp[] = searchClass(src).split(" ");
      int cnt = -1;
      for (int i = 0; i < tmp.length; i++) {
        if (CLASS_KIND.contains(tmp[i])) {
          cnt = i;
          break;
        }
      }
      if (cnt != -1) {
        classKind = tmp[cnt];
        className = packageName + "." + tmp[cnt + 1];
        return;
      }
      throw new IllegalArgumentException("Java ソースからクラス名を取得することが出来ませんでした。\n" + source);
    }
  }

  /**
   * 直前のマージした Java ソースのクラス名（パッケージを含む）を取得します。
   * 
   * @return クラス名（パッケージを含む）
   */
  public String getMergedClassName() {
    return className;
  }

  /**
   * 対象文字列からパッケージ名を取得する。
   * 
   * @param src 対象文字列
   * @return パッケージ名
   */
  private String searchPackage(String src) {
    String ret = src;

    int cnt = ret.indexOf("package ");
    if (cnt == -1) {
      return "";
    }
    int tmp = ret.indexOf(";", cnt);
    if (tmp != -1) {
      ret = ret.substring(cnt, tmp);
    } else {
      return "";
    }
    while (ret.contains("  ")) {
      ret = ret.replace("  ", " ");
    }

    // "package " を除く
    return ret.substring(8).trim();
  }

  /**
   * 対象文字列からクラス型を含む文字列を返す。
   * 
   * @param src 対象文字列
   * @return クラス型を含む宣言
   */
  private String searchClass(String src) {
    String ret = src;

    // 先出のクラス型の位置を検出する。その際に単語であることを保障するために空白を追加。
    int iCls = -1;
    for (String strCK : CLASS_KIND) {
      if (ret.contains(strCK + " ")) {
        if (iCls == -1 || iCls > ret.indexOf(strCK + " ")) {
          iCls = ret.indexOf(strCK + " ");
        }
      }
    }
    if (iCls == -1) {
      return "";
    }
    int cnt = ret.indexOf("{", iCls);
    if (cnt == -1) {
      return "";
    }
    for (int i = cnt; i >= 0; i--) {
      if (ret.charAt(i) == ';' || ret.charAt(i) == ')') {
        ret = ret.substring(i + 1, cnt - 1).trim();
        while (ret.contains("  ")) {
          ret = ret.replace("  ", " ");
        }
        return ret;
      }
    }
    return "";
  }

  /**
   * 指定した文字列から Java 言語によるコメント文を削除する。
   * 
   * @param src 対象文字列
   * @return 修正した対象文字列
   */
  private String delComment(String src) {
    StringBuilder sb = new StringBuilder();

    char[] c = src.toCharArray();
    int last = c.length - 1;
    int cnt;
    for (cnt = 0; cnt < last; cnt++) {
      if (c[cnt] == '/' && c[cnt + 1] == '*') {
        cnt++;
        // ブロックコメントを読み飛ばし
        for (cnt++; cnt <= last; cnt++) {
          if (c[cnt - 1] == '*' && c[cnt] == '/') {
            cnt++;
            break;
          }
        }
      } else if (c[cnt] == '/' && c[cnt + 1] == '/') {
        cnt++;
        // 行コメントを読み飛ばし
        for (cnt++; cnt <= last; cnt++) {
          if (c[cnt] == '\n') {
            break;
          }
        }
      }
      if (cnt > last)
        break;
      if (sb.length() == 0 && c[cnt] == '\n') {
      } else if (cnt > 0) {
        if (c[cnt] != '\n' || c[cnt - 1] != '\n') {
          sb.append(c[cnt]);
        }
      } else {
        sb.append(c[cnt]);
      }
    }
    if (cnt == last) {
      if (c[cnt] != '\n') {
        sb.append(c[cnt]);
      }
    }

    return sb.toString();
  }

}
