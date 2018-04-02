/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved. This program are made available under
 * the terms of the Common Public License v1.0. Copyright (c) 2017- kuro-neko.
 */
package mergejdoc.core;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import mergejdoc.MergeDocException;
import mergejdoc.xml.Persister;

/**
 * Javadoc API ドキュメントです。
 * 
 * @author Shinji Kashihara
 */
public class APIDocument {

  /** ロガー */
  private static final Logger logger = LogManager.getLogger(APIDocument.class);

  /** シグネチャをキーとしたコメントのテーブル */
  private final Map<Signature, Comment> contextTable = new HashMap<Signature, Comment>();

  /**
   * コンストラクタです。
   * 
   * @param docDir API ドキュメントディレクトリ
   * @param className クラス名
   * @param charsetName 文字セット名
   * @throws IOException 入出力例外が発生した場合
   */
  public APIDocument(File docDir, String className, String charsetName) throws IOException {

    boolean apiD = false;
    try {
      if (Persister.getInstance().getString(Persister.API_DONWLOAD, "").equals("true")) {
        apiD = true;
        if (className.startsWith("com.sun.")) {
          return;
        }
        URL url;
        if (!Persister.getInstance().getString(Persister.API_URL, "").equals("")) {
          url = new URL(Persister.getInstance().getString(Persister.API_URL, "")
              + className.replace('.', '/') + ".html");
        } else {
          url = new URL("http://docs.oracle.com/javase/jp/8/docs/api/" + className.replace('.', '/')
              + ".html");
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setAllowUserInteraction(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod("GET");
        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new Exception();
        }

        // Input Stream
        DataInputStream dataInStream = new DataInputStream(conn.getInputStream());

        // Output Stream
        // API ドキュメント絶対パスを生成
        StringBuilder path = new StringBuilder();
        path.append(docDir.getPath());
        path.append(File.separator);
        path.append(className.replace('.', File.separatorChar));
        path.append(".html");
        File apiDir = new File(new File(path.toString()).getParent());
        apiDir.mkdirs();
        DataOutputStream dataOutStream =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toString())));

        // Read Data
        byte[] b = new byte[4096];
        int readByte = 0;

        while (-1 != (readByte = dataInStream.read(b))) {
          dataOutStream.write(b, 0, readByte);
        }

        // Close Stream
        dataInStream.close();
        dataOutStream.close();
      }
    } catch (MergeDocException e) {
      // 設定ファイルが取得できない場合
      apiD = false;
    } catch (Exception e) {
      // ダウンロードエラーが発生した場合
      apiD = false;
    }

    if (!apiD) {
      return;
    }

    // API ドキュメント絶対パスを生成
    StringBuilder path = new StringBuilder();
    path.append(docDir.getPath());
    path.append(File.separator);
    path.append(className.replace('.', File.separatorChar));
    path.append(".html");

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
        if (cachedFiles == null)
          cachedFiles = EMPTY_FILES;
      }
      return cachedFiles;
    }
  }

  /**
   * API ドキュメント HTML ファイルを読み込みます。
   * 
   * @param docDir API ドキュメントディレクトリ
   * @param docFile API ドキュメントファイル
   * @param charsetName 文字セット名
   * @throws IOException 入出力例外が発生した場合
   */
  private void load(File docDir, File docFile, String charsetName) throws IOException {

    // 存在しない場合は何もしない
    if (!docFile.exists())
      return;

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
    className = className.replace(docDir.getPath() + File.separator, ""); // Patternキャッシュしない
    className = className.replace(File.separatorChar, '.');

    // StringBuffer、StringBuilder だけの特殊処理
    if (className.equals("java.lang.StringBuffer") || className.equals("java.lang.StringBuilder")) {
      docHtml = docHtml.replace("%20", "");
    }

    // API ドキュメントのコメント解析
    Document document = Jsoup.parse(docHtml);
    parseClassComment(className, document);
    parseMethodComment(className, document);
  }

  /**
   * コンテキストが空か判定します。
   * 
   * @return コンテキストが空の場合は true
   */
  public boolean isEmpty() {
    return contextTable.isEmpty();
  }

  /**
   * 指定したシグネチャを持つ Javadoc コメントを取得します。
   * 
   * @param signature シグネチャ
   * @return Javadoc コメント
   */
  public Comment getComment(Signature signature) {
    Comment comment = contextTable.get(signature);
    return comment;
  }

  /**
   * クラスの Javadoc コメント情報を作成します。 author, version タグは Javadoc デフォルトでは存在しないため解析しません。<br>
   * 
   * @param className クラス名
   * @param docHtml API ドキュメントソース
   */
  private void parseClassComment(String className, Document doc) {
    Elements elements = doc.select("body div.contentContainer div.description ul li");

    if (elements.isEmpty() == false) {
      // シグネチャの作成
      String sigStr = elements.select("pre").first().html();
      Signature sig = createSignature(className, sigStr);
      Comment comment = new Comment(sig);

      if (!elements.select("div span.deprecatedLabel").isEmpty()) {
        // deprecated タグ
        parseDeprecatedTag(className, elements.select("div").first(), comment);
      } else if (!elements.select("div").isEmpty()) {
        // 本文
        String body = "";
        body = elements.select("div").last().html();
        body = formatLinkTag(className, body);
        comment.setDocumentBody(body);
      }

      Element el = elements.select("dl dt, dl dd").first();
      while (el != null) {
        if (el.html().contains("simpleTagLabel")) {
          // since タグ
          el = el.nextElementSibling();
          comment.addSince(el.text());
          el = el.nextElementSibling();
        } else if (el.html().contains("seeLabel")) {
          // see タグ
          parseSeetag(className, el, comment);
          el = el.nextElementSibling();
        } else {
          el = el.nextElementSibling();
        }
      }

      // debug parseClassComment メソッドのシグネチャ、コメント確認
      // log.debug(sig);
      contextTable.put(sig, comment);
    }
  }

  /**
   * メソッドやフィールドの Javadoc コメント情報を作成します。
   * 
   * @param className クラス名
   * @param docHtml API ドキュメントソース
   */
  private void parseMethodComment(String className, Document doc) {
    Elements elements =
        doc.select("body div.contentContainer div.details ul li ul li ul li.blockList");
    for (Element element : elements) {
      // シグネチャの作成
      String sigStr = element.select("pre").first().html();
      Signature sig = createSignature(className, sigStr);
      Comment comment = new Comment(sig);

      // debug parseMethodComment メソッドのシグネチャ確認
      // log.debug(sig);
      contextTable.put(sig, comment);
      if (!element.select("div span.deprecatedLabel").isEmpty()) {
        // deprecated タグ
        parseDeprecatedTag(className, element.select("div").first(), comment);
      } else if (!element.select("div").isEmpty()) {
        // 本文
        String body = "";
        body = element.select("div").last().html();
        body = formatLinkTag(className, body);
        comment.setDocumentBody(body);
      }

      Elements dts = element.select("dt, dd");
      Element el = dts.first();
      while (el != null) {
        if (el.html().contains("paramLabel")) {
          // param タグ
          el = el.nextElementSibling();
          if (el == null) {
            break;
          }
          while (el.tagName().equals("dd")) {
            String name = el.select("code").first().text();
            String desc = el.html();
            desc = desc.substring(desc.indexOf(" - ") + 3);
            desc = desc.replace("\n", "");
            desc = formatLinkTag(className, desc);
            String param = name + " " + desc;
            comment.addParam(param);
            el = el.nextElementSibling();
            if (el == null) {
              break;
            }
          }
        } else if (el.html().contains("returnLabel")) {
          // return タグ
          el = el.nextElementSibling();
          if (el == null) {
            break;
          }
          String str = el.html();
          str = formatLinkTag(className, str);
          comment.addReturn(str);
          el = el.nextElementSibling();
        } else if (el.html().contains("throwsLabel")) {
          // throws (exception) タグ
          el = el.nextElementSibling();
          if (el == null) {
            break;
          }
          while (el.tagName().equals("dd")) {
            Elements a = el.select("code a[href]");
            if (a.isEmpty()) {
              el = el.nextElementSibling();
              if (el == null) {
                break;
              }
              continue;
            }
            String name = a.first().attr("href").toString();
            name = formatClassName(className, name);
            String desc = el.html();
            desc = desc.substring(desc.indexOf(" - ") + 3);
            desc = formatLinkTag(className, desc);
            String param = name + " " + desc;
            comment.addThrows(param);
            el = el.nextElementSibling();
            if (el == null) {
              break;
            }
          }
        } else if (el.html().contains("simpleTagLabel")) {
          // since タグ
          el = el.nextElementSibling();
          if (el == null) {
            break;
          }
          comment.addSince(el.text());
          el = el.nextElementSibling();
        } else if (el.html().contains("seeLabel")) {
          // see タグ
          parseSeetag(className, el, comment);
          el = el.nextElementSibling();
        } else {
          el = el.nextElementSibling();
        }
      }
    }
  }

  /**
   * シグネチャを作成します。
   * 
   * @param className クラス名
   * @param sig シグネチャ文字列
   */
  private Signature createSignature(String className, String sig) {
    sig = FastStringUtils.replaceAll(sig, "(?s)<.+?>", " "); // タグ除去
    sig = FastStringUtils.replaceAll(sig, "\\&nbsp;", " "); // 空白置換
    sig = FastStringUtils.replaceAll(sig, "\\&lt;", "<"); // 型引数開始タグ
    sig = FastStringUtils.replaceAll(sig, "\\&gt;", ">"); // 型引数終了タグ
    sig = FastStringUtils.replaceFirst(sig, "(?s)\\sthrows\\s.*", "");
    Signature signature = new Signature(className, sig);
    return signature;
  }

  /**
   * Javadoc の deprecated タグを解析しコメントに追加します。
   * 
   * @param className クラス名
   * @param element select("div") にて取得した Elements の最初の Element
   * @param comment コメント
   */
  private void parseDeprecatedTag(String className, Element element, Comment comment) {
    if (element.select("span.deprecatedLabel").isEmpty()) {
      return;
    }
    Element div = element.select("span.deprecationComment").first();
    String deprecated = "";
    if (div != null) {
      deprecated = div.html();
      deprecated = formatLinkTag(className, deprecated);
    }
    comment.addDeprecated(deprecated);
  }

  /**
   * Javadoc の see タグを解析しコメントに追加します。
   * 
   * @param className クラス名
   * @param element .select("dt, dd") にて取得した Elements の中の .html().contains("seeLabel") に合致する Element
   *        。
   * @param comment コメント
   */
  private void parseSeetag(String className, Element element, Comment comment) {
    // see タグ
    if (!element.html().contains("seeLabel")) {
      return;
    }
    Element el = element.nextElementSibling();
    if (el == null) {
      return;
    }
    for (Element a : el.select("a[href] code")) {
      String url = a.parentNode().attr("href");
      String ref;
      if (a.childNodeSize() != 1) {
        ref = a.outerHtml();
      } else {
        ref = formatClassName(className, url);
      }
      comment.addSee(ref);
    }
  }

  /**
   * HTML の A タグを Javadoc の link タグにフォーマットします。
   * <p>
   * 責務的には Javadoc タグの整形は Comment クラスで行うべきですが、 今のところ、Javadoc の see タグとのからみもあり、このクラスで 処理しています。
   * 
   * @param className クラス名
   * @param html HTML の A タグを含む文字列
   * @return Javadoc link タグ文字列
   */
  private String formatLinkTag(String className, String html) {
    // HTML構文を解析
    Document document = Jsoup.parse(html);
    document.outputSettings().indentAmount(0);
    Elements elements = document.select("a[href]");
    for (Element element : elements) {
      String url = element.attr("href");
      StringBuilder link = new StringBuilder();
      String ref = formatClassName(className, url);

      Elements codes = element.select("code");
      String label = codes.size() > 0 ? codes.first().text() : element.text();
      if (element.text().equals(label)) {

        link.append("{@link ");
        link.append(ref);
        if (label.length() > 0) {
          ref = ref.replace('#', '.');
          label = label.replace(" ", "");
          label = label.replace("java.lang.", "");
          if (!ref.endsWith(label)) {
            link.append(" ");
            link.append(label);
          }
        }
        link.append("}");

      } else {
        link.append("{@linkplain ");
        link.append(ref);
        link.append(" ");
        link.append(element.html());
        link.append("}");
      }

      // <_delete_> タグを目印にしてリンクタグを置換する。
      Element cld = document.createElement("_delete_");
      cld.append(link.toString());
      element.replaceWith(cld);
    }

    // 不必要なタグを削除する。
    String ret = document.select("body").html();
    ret = ret.replace("<_delete_>", "").replace("</_delete_>", "");
    ret = ret.replace("\n", "");
    ret = ret.replace("、", ",");

    return ret;
  }

  /**
   * HTML の CODE タグを Javadoc の code タグにフォーマットします。
   * 
   * @param html HTML の CODE タグを含む文字列
   * @return Javadoc code タグ文字列
   */
  private String formatCodeTag(String html) {
    // HTML構文を解析
    Document document = Jsoup.parse(html);
    document.outputSettings().indentAmount(0);
    Elements elements = document.select("code");
    for (Element element : elements) {
      String label = element.text();

      // <_delete_> タグを目印にしてコードタグを置換する。
      Element cld = document.createElement("_delete_");
      cld.append("{@code " + label + "}");
      element.replaceWith(cld);
    }

    // 不必要なタグを削除する。
    String ret = document.select("body").html();
    ret = ret.replace("<_delete_>", "").replace("</_delete_>", "");
    ret = ret.replace("\n", "");
    return ret;
  }

  /**
   * see タグや link タグの URL を package.class#member 形式にフォーマットします。 同一パッケージの場合は package が省略され、同一クラスの場合は
   * class も省略されます。
   * 
   * @param className クラス名
   * @param path パス
   * @return package.class#member 形式の文字列
   */
  private String formatClassName(String className, String path) {

    String lastClassName = FastStringUtils.replaceFirst(className, ".+\\.", "");
    String packageName = className.replace("." + lastClassName, ""); // Patternキャッシュしない
    String lastClassPrefix = "\\.([A-Z])";

    path = FastStringUtils.replace(path, ".html", "");
    path = path.replace('/', '.');
    path = FastStringUtils.replaceAll(path, "-(.*)-$", "($1)");
    path = FastStringUtils.replaceAll(path, "-", ",");
    path = FastStringUtils.replaceAll(path, ":A", "[]");
    path = FastStringUtils.replaceFirst(path, "^\\.*", "");
    path = FastStringUtils.replaceAll(path, "java.lang" + lastClassPrefix, "$1");
    path = path.replaceAll(packageName + lastClassPrefix, "$1"); // Patternキャッシュしない
    path = path.replaceAll(lastClassName + "#", "#"); // Patternキャッシュしない
    return path;
  }

  /**
   * see タグや link タグの URL を package.class#member 形式にフォーマットします。
   * 
   * @param path パス
   * @return package.class#member 形式の文字列
   */
  private String formatClassName(String path) {
    path = FastStringUtils.replaceAll(path, ".html", "");
    path = FastStringUtils.replaceAll(path, "\\.\\./", "");
    path = path.replace('/', '.');
    path = FastStringUtils.replaceAll(path, "-(.*)-$", "($1)");
    path = FastStringUtils.replaceAll(path, "-", ",");
    path = FastStringUtils.replaceAll(path, ":A", "[]");
    return path;
  }
}
