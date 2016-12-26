/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.io.File;

import mergedoc.xml.ReplaceEntry;


/**
 * マージ設定インタフェースです。
 * @author Shinji Kashihara
 */
public interface Preference {

    /**
     * API ドキュメントディレクトリを取得します。
     * @return API ドキュメントディレクトリ
     */
    File getDocDirectory();

    /**
     * 入力ソースアーカイブファイルを取得します。
     * @return 入力ソースアーカイブファイル
     */
    File getInputArchive();

    /**
     * 出力ソースアーカイブファイルを取得します。
     * @return 出力ソースアーカイブファイル
     */
    File getOutputArchive();

    /**
     * API ドキュメントエンコーディングを取得します。
     * @return API ドキュメントエンコーディング
     */
    String getDocEncoding();

    /**
     * 入力ソースエンコーディングを取得します。
     * @return 入力ソースエンコーディング
     */
    String getInputEncoding();

    /**
     * 出力ソースエンコーディングを取得します。
     * @return 出力ソースエンコーディング
     */
    String getOutputEncoding();

    /**
     * グローバル置換エントリの配列を取得します。
     * @return グローバル置換エントリの配列
     */
    ReplaceEntry[] getGlobalEntries();
}
