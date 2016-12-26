/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import mergedoc.MergeDocException;

/**
 * 設定の永続化を行うクラスです。
 * @author Shinji Kashihara
 */
public class Persister {

    /**
     * 永続化のキーとなるクラスです。
     * 外部から型は使用できますが生成は出来ません。
     */
    public static class Key {
        private final String str;
        private Key(String str)  {this.str = str;}
        public String toString() {return str;}
    }

    /** API ドキュメントディレクトリのキー */
    public static final Key DOC_DIR = new Key("api.document.directory");

    /** 入力ソースアーカイブファイルのキー */
    public static final Key IN_FILE = new Key("input.source.archeve.file");

    /** 出力ソースアーカイブファイルのキー */
    public static final Key OUT_FILE = new Key("output.source.archeve.file");

    /** Javadoc API エンコーディングのキー */
    public static final Key DOC_ENC = new Key("api.document.charset");

    /** 入力エンコーディングのキー */
    public static final Key IN_ENC = new Key("input.source.charset");

    /** 出力エンコーディングのキー */
    public static final Key OUT_ENC = new Key("output.source.charset");

    /** チェック済み置換エントリの説明（配列）のキー */
    public static final Key REPLACE_DESCRIPTION_ARRAY = new Key("replace.entry.descprition");

    /** ウィンドウ幅のキー */
    public static final Key WINDOW_WIDTH = new Key("window.dimention.width");

    /** ウィンドウ高のキー */
    public static final Key WINDOW_HEIGHT = new Key("window.dimention.height");

    /** ウィンドウ左上の座標 X のキー */
    public static final Key WINDOW_X = new Key("window.position.x");

    /** ウィンドウ左上の座標 Y のキー */
    public static final Key WINDOW_Y = new Key("window.position.y");

    /** ウィンドウ状態のキー */
    public static final Key WINDOW_STATE = new Key("window.state");

    /** 詳細設定のチェックリストパネル高のキー */
    public static final Key DETAIL_PANEL_HEIGHT = new Key("window.replace.panel.height");


    /** 永続化ファイル */
    private final File parsistFile;

    /** 永続化ファイルに対応するプロパティ */
    private final Properties prop = new Properties(); 

    /** このクラスのインスタンス */
    private static Persister parsister;


    /**
     * コンストラクタです。
     * @throws MergeDocException 永続化操作が出来ない場合
     */
    private Persister() throws MergeDocException {
        ConfigManager config = ConfigManager.getInstance();
        parsistFile = config.getFile("mergedoc.properties");
        try {
            parsistFile.createNewFile();
            InputStream is = new BufferedInputStream(new FileInputStream(parsistFile));
            prop.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MergeDocException(parsistFile + " の操作でエラーが発生しました。");
        }
    }

    /**
     * このクラスのインスタンスを取得します。同期化は行われません。
     * @return このクラスのインスタンス
     */
    public static Persister getInstance() throws MergeDocException {
        if (parsister == null) {
            parsister = new Persister();
        }
        return parsister;
    }
    
    /**
     * 永続化します。
     * @throws MergeDocException 永続化に失敗した場合
     */
    public void store() throws MergeDocException {
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(parsistFile));
            prop.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MergeDocException(parsistFile + " の書き込みでエラーが発生しました。");
        }
    }
    
    /**
     * エントリ数を取得します。
     * @return エントリ数
     */
    public int size() {
        return prop.size();
    }
    
    /**
     * String 値をセットします。
     * @param key キー
     * @param value 値
     * @throws NullPointerException キーまたは値が null の場合
     */
    public void setString(Key key, String value) {
        prop.setProperty(key.toString(), (value == null) ? "" : value);
    }
    
    /**
     * String 値を取得します。
     * @param key キー
     * @return 値。取得出来なかった場合は null。
     * @throws NullPointerException キーが null の場合
     */
    public String getString(Key key) {
        return getString(key, null);
    }
    
    /**
     * String 値を取得します。
     * @param key キー
     * @param def 値が取得出来なかった場合のデフォルト値
     * @return 値
     * @throws NullPointerException キーが null の場合
     */
    public String getString(Key key, String def) {
        String str = prop.getProperty(key.toString());
        return (str == null) ? def : str;
    }
    
    /**
     * int 値をセットします。
     * @param key キー
     * @param value 値
     * @throws NullPointerException キーが null の場合
     */
    public void setInt(Key key, int value) {
        setString(key, String.valueOf(value));
    }
    
    /**
     * int 値を取得します。
     * @param key キー
     * @return 値
     * @throws NullPointerException キーが null の場合
     * @throws NumberFormatException int 値が取得出来なった場合
     */
    public int getInt(Key key) {
        return Integer.parseInt(getString(key));
    }
    
    /**
     * int 値を取得します。
     * @param key キー
     * @param def 値が取得出来なかった場合のデフォルト値
     * @return 値
     * @throws NullPointerException キーが null の場合
     */
    public int getInt(Key key, int def) {
        try {
            return getInt(key);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    /**
     * String[] 値をセットします。
     * @param key キー
     * @param values 値
     * @throws NullPointerException キーまたは値が null の場合
     */
    public void setStrings(Key key, String[] values) {
        for (int i = 0; ; i++) {
            String str = prop.getProperty(key.toString() + i);
            if (str == null) break;
            prop.remove(key.toString() + i);
        }
        for (int i = 0; i < values.length; i++) {
            prop.setProperty(key.toString() + i, values[i]);
        }
    }
    
    /**
     * String[] 値を取得します。
     * @param key キー
     * @return 値。取得出来なかった場合はサイズ 0 の配列。
     * @throws NullPointerException キーが null の場合
     */
    public String[] getStrings(Key key) {
        List<String> list = new LinkedList<String>();
        for (int i = 0; ; i++) {
            String str = prop.getProperty(key.toString() + i);
            if (str == null) break;
            list.add(str);
        }
        return list.toArray(new String[list.size()]);
    }
}
