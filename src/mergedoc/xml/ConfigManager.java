/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import mergedoc.MergeDocException;
import mergedoc.core.FastStringUtils;

/**
 * コンフィグマネージャです。
 * @author Shinji Kashihara
 */
public class ConfigManager {

    /** このクラスのインスタンス */
    private static ConfigManager configManager;

    /** SAX パーサ */
    private final SAXParser saxParser;

    /** コンフィグルートパス */
    private final File configRoot;
    
    /** global.xml ファイル */
    private final File globalXML;

    /**
     * コンストラクタです。
     * @throws MergeDocException SAX パーサの生成に失敗した場合
     */
    private ConfigManager() throws MergeDocException {

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MergeDocException("SAX パーサの生成に失敗しました。\n" + e);
        }

        String globalXMLName = "/global.xml";
        URL url = getClass().getResource(globalXMLName);
        if (url == null) {
            throw new MergeDocException(globalXMLName + " が見つかりません。");
        } 
        globalXML = new File(url.getPath());
        
        String parent = null;
        try {
        	// 空白やマルチバイト文字ディレクトリの対応
            parent = URLDecoder.decode(globalXML.getParent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        configRoot = new File(parent);
    }
    
    /**
     * このクラスのシングルトンインスタンスを取得します。
     * 生成時の同期化は行われません。
     * @return このクラスのインスタンス
     * @throws MergeDocException SAX パーサの生成に失敗した場合
     */
    public static ConfigManager getInstance() throws MergeDocException {
        if (configManager == null) {
            configManager = new ConfigManager();
        }
        return configManager;
    }

    /**
     * SAX パーサを取得します。
     * @return SAX パーサ
     */
    public SAXParser getSAXPerser() {
        return saxParser;
    }
    
    /**
     * コンフィグルートの子相対パスを指定してファイルを取得します。
     * @param path コンフィグルートの子相対パス
     * @return ファイル
     */
    public File getFile(String path) {
        return new File(configRoot, path);
    }
    
    /**
     * グローバル定義 XML の置換エントリリストを取得します。
     * @return グローバル定義 XML の置換エントリリスト
     * @throws MergeDocException 取得出来なかった場合
     */
    public List<ReplaceEntry> getGlobalEntries() throws MergeDocException {
        ListingHandler handler = new ListingHandler();
        try {
            saxParser.parse(globalXML, handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MergeDocException(
                globalXML.getName() + " のパースに失敗しました。\n" + e);
        }
        return handler.getReplaceEntries();
    }
    
    /**
     * プレビューテンプレートとなる文字列を取得します。
     * @return プレビューテンプレート文字列
     * @throws MergeDocException 取得出来なかった場合
     */
    public String getPrevewTemplate() throws MergeDocException {
        File file = getFile("preview.tpl");
        String template = null;
        try {
            InputStream is = new FileInputStream(file);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            template = new String(buf, "UTF-8");
        } catch (IOException e) {
            throw new MergeDocException(file + " が見つかりません。");
        }
        template = FastStringUtils.optimizeLineSeparator(template);
        return template;
    }
}
