/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 * アーカイブ入力ストリームです。
 * <p>
 * 異なる形式のアーカイブファイル .zip や .tar.gz を同一視するためのクラスです。
 * 扱いやすくするためにインターフェースは ZipInputStream に合わせています。
 * <p>
 * 汎用的にするために将来 java.io.InputStream インタフェースを実装する可能性が
 * あります。
 * 
 * @author Shinji Kashihara
 */
abstract public class ArchiveInputStream {

    /**
     * アーカイブエントリインターフェースです。
     */
    public static interface Entry {
        public String getName();
    }

    /**
     * コンストラクタです。
     */
    private ArchiveInputStream() {
    }

    /**
     * アーカイブ入力ストリームを作成します。
     * <p>
     * 指定されたファイルの形式を判定し、適切な ArchiveInputStream 実装クラスの
     * インスタンスを返します。今のところ、形式の判定は簡易的にファイル拡張子で
     * 行っており、データ内容での判定は一切行われません。
     * 
     * @param  アーカイブファイル
     * @return アーカイブ入力ストリーム
     * @throws IOException ファイル形式が不正な場合
     */
    public static ArchiveInputStream create(File file) throws IOException {
        String fileName = file.getName();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {

            return new ZipStreamProxy(new ZipInputStream(bis));

        } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {

            return new TarStreamProxy(new TarInputStream(new GZIPInputStream(bis)));

        } else {

            throw new IllegalArgumentException(
                "このファイル形式はサポートしません。\n" + fileName);
        }
    }

    /**
     * アーカイブ入力ストリームを閉じます。
     * @throws  IOException 入出力エラーが発生した場合
     */
    abstract public void close() throws IOException;

    /**
     * 次のアーカイブファイルエントリを読み込み、エントリデータの最初にストリームを
     * 配置します。
     * @return  読み込まれた ZipEntry
     * @throws  ZipException ZIP ファイルエラーが発生した場合
     * @throws  IOException 入出力エラーが発生した場合
     */
    abstract public Entry getNextEntry() throws IOException;

    /**
     * 入力ストリームからバイト配列に最大 byte.length バイトの
     * データを読み込みます。このメソッドは入力データが読み込み可能になる
     * までブロックします。
     * <p>
     * このメソッドは単純に read(b, 0, b.length) の呼び出しを
     * 実行し、その結果を返します。代わりに in.read(b) が実行されな
     * いようにしてください。FilterInputStream の特定のサブクラスは、
     * 実際に使用されている実装方法に依存します。
     * 
     * @param   b   データの読み込み先のバッファ
     * @return  バッファに読み込まれたバイトの合計数。ストリームの終わりに
     *          達してデータがない場合は -1
     * @throws  IOException 入出力エラーが発生した場合
     * @see     InputStream#read(byte[], int, int)
     */
    abstract public int read(byte b[]) throws IOException;
    
    
    /**
     * Zip 入力ストリームのアーカイブ入力ストリーム実装クラスです。
     */
    private static class ZipStreamProxy extends ArchiveInputStream {
        ZipInputStream is;
        ZipStreamProxy(ZipInputStream is) {
            this.is = is;
        }
        public void close() throws IOException {
            is.close();
        }
        public Entry getNextEntry() throws IOException {
            ZipEntry entry = is.getNextEntry();
            return (entry != null) ? new ZipEntryProxy(entry) : null;
        }
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }
    }

    /**
     * Zip エントリのエントリ実装クラスです。
     */
    private static class ZipEntryProxy implements Entry {
        ZipEntry entry;
        ZipEntryProxy(ZipEntry entry) {
            this.entry = entry;
        }
        public String getName() {
            return entry.getName();
        }
    }
    
    /**
     * Tar 入力ストリームのアーカイブ入力ストリーム実装クラスです。
     */
    private static class TarStreamProxy extends ArchiveInputStream {
        TarInputStream is;
        TarStreamProxy(TarInputStream is) {
            this.is = is;
        }
        public void close() throws IOException {
            is.close();
        }
        public Entry getNextEntry() throws IOException {
            TarEntry entry = is.getNextEntry();
            return (entry != null) ? new TarEntryProxy(entry) : null;
        }
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }
    }

    /**
     * Tar エントリのエントリ実装クラスです。
     */
    private static class TarEntryProxy implements Entry {
        TarEntry entry;
        TarEntryProxy(TarEntry entry) {
            this.entry = entry;
        }
        public String getName() {
            return entry.getName();
        }
    }
}
