/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.event.ChangeListener;
import javax.xml.parsers.SAXParser;

import mergedoc.MergeDocException;
import mergedoc.xml.ConfigManager;
import mergedoc.xml.ReplaceEntry;
import mergedoc.xml.ReplaceHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

/**
 * マージマネージャです。
 * @author Shinji Kashihara
 */
public class MergeManager {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(MergeManager.class);

    /** マージ設定 */
    private Preference pref;

    /** 処理状態 */
    private WorkingState workingState = new WorkingState();

    /** コピー用のバイトバッファ */
    private byte[] byteBuffer = new byte[4096];

    /** エントリー数取得 Executor */
    private ExecutorService entrySizeGetExecutor = Executors.newSingleThreadExecutor();

    /** エントリー数 Future */
    private Future<Integer> entrySizeFuture;

    /**
     * コンストラクタです。
     */
    public MergeManager() throws MergeDocException {
    }

    /**
     * マージ設定をセットします。
     * @param pref マージ設定
     */
    public void setPreference(final Preference pref) {
        
        this.pref = pref;
        workingState.initialize();
        
        // エントリー数取得は数秒かかるので事前に別スレッドでを開始しておく
        entrySizeFuture = entrySizeGetExecutor.submit(new Callable<Integer>() {
            
            public Integer call() throws Exception {
                ArchiveInputStream is = null;
                try {
                    is = ArchiveInputStream.create(pref.getInputArchive());
                    int size = 0;
                    for (; is.getNextEntry() != null; size++);
                    return size;
                } catch (Exception e) {
                    return 0;
                } finally {
                    if (is != null) is.close();
                }
            }
        });
    }

    /**
     * マージ可能な状態か検証します。
     * @throws MergeDocException マージ不可能な状態の場合
     * @throws IOException 入出力例外が発生した場合
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void validate() throws MergeDocException, IOException, InterruptedException, ExecutionException {

        // API ドキュメントディレクトリのチェック
        File docDir = pref.getDocDirectory();
        if (docDir != null && docDir.getPath().length() > 0) {
            File rootFile = new File(docDir, "allclasses-frame.html");
            if (!rootFile.exists()) {
                throw new MergeDocException(
                "正しい API ドキュメントディレクトリを指定してください。\n" +                "指定するディレクトリには allclasses-frame.html ファイルが\n" +                "含まれている必要があります。");
            }
        }

        // 入力ソースアーカイブファイルのチェック
        File inFile = pref.getInputArchive();
        if (inFile == null || inFile.getPath().equals("")) {
            throw new MergeDocException(
            "入力ソースアーカイブファイルを指定してください。");
        }
        if (entrySize() == 0) {
            throw new MergeDocException(
            "正しい入力ソースアーカイブファイルを指定してください。");
        }

        // 出力ソースアーカイブファイルのチェック
        File outFile = pref.getOutputArchive();
        if (outFile == null || outFile.getPath().equals("")) {
            throw new MergeDocException(
            "出力ソースアーカイブファイルを指定してください。");
        }
        if (outFile.equals(inFile)) {
            throw new MergeDocException(
            "入力、出力ソースアーカイブファイルに同じファイルが指定されています。\n" +
            "正しい出力ソースアーカイブファイルを指定してください。");
        }
        if (pref.getOutputArchive().exists()) {
            if (!outFile.canWrite()) {
                throw new MergeDocException(
                "指定された出力ソースアーカイブファイルは書き込み不可です。\n" +
                "正しい出力ソースアーカイブファイルを指定してください。");
            }
        } else {
            try {
                outFile.createNewFile();
                outFile.delete();
            } catch (IOException e) {
                throw new MergeDocException(
                "正しい出力ソースアーカイブファイルを指定してください。", e);
            }
        }
    }

    /**
     * 処理を実行します。<br>
     * @throws MergeDocException コンフィグ情報の取得に失敗した場合
     * @throws SAXException SAX パース例外が発生した場合
     * @throws IOException 入出力例外が発生した場合
     */
    public void execute() throws MergeDocException, SAXException, IOException {
        
        if (workingState.isCanceled()) return;
        ArchiveInputStream in = null;
        ZipOutputStream out = null;

        try {
            in = ArchiveInputStream.create(pref.getInputArchive());

            out = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(pref.getOutputArchive())));
            out.setLevel(Deflater.BEST_SPEED);

            long start = System.currentTimeMillis();
            merge(in, out);
            long end = System.currentTimeMillis();
            workingState.setWorkTime((end - start) / 1000);

        } finally {

            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
     * アーカイブ入力ストリームから順次エントリを読み込み、Java ソースの場合は
     * API ドキュメントとマージし、それ以外のファイルはそのまま ZIP
     * 出力ストリームに書き込みます。
     * 
     * @param  in  アーカイブ入力ストリーム 
     * @param  out ZIP 出力ストリーム
     * @throws MergeDocException コンフィグ情報の取得に失敗した場合
     * @throws SAXException SAX パース例外が発生した場合
     * @throws IOException 入出力例外が発生した場合
     */
    private void merge(ArchiveInputStream in, ZipOutputStream out)
        throws MergeDocException, SAXException, IOException
    {
        Merger merger = new Merger(pref.getDocDirectory());
        merger.setDocEncoding(pref.getDocEncoding());

        ArchiveInputStream.Entry inEntry = null;
        while ((inEntry = in.getNextEntry()) != null) {

            if (workingState.isCanceled()) return;
            String entryName = inEntry.getName();
            out.putNextEntry(new ZipEntry(entryName));
            workingState.changeWorkingText(entryName);

            //debug 処理対象クラス指定
            //if (!entryName.equals("java/lang/String.java")) continue;
            //if (!entryName.endsWith("/SuppressWarnings.java")) continue;
            //if (!entryName.endsWith("/System.java")) continue;
            
            if (entryName.endsWith(".java") && !entryName.endsWith("/package-info.java")) {

                // Java ソースの場合
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copyStream(in, baos);
                String source = baos.toString(pref.getInputEncoding());
                source = FastStringUtils.optimizeLineSeparator(source);
                source = FastStringUtils.untabify(source);

                // Java ソースを API ドキュメントとマージ
                String result = merger.merge(source);
                String className = merger.getMergedClassName();
                if (className != null) {
                    result = doFilter(className, result);
                }
                byte[] resultBuf = result.getBytes(pref.getOutputEncoding());
                out.write(resultBuf);

            } else {

                // Java ソース以外の場合
                copyStream(in, out);
            }
        }
    }
    
    /**
     * 入力ストリームを出力ストリームにコピーします。
     * @param in 入力ストリーム
     * @param out 出力ストリーム
     * @throws IOException 入出力例外が発生した場合
     */
    private void copyStream(ArchiveInputStream in, OutputStream out) throws IOException {
        for (int size = 0; (size = in.read(byteBuffer)) > 0;) {
            out.write(byteBuffer, 0, size);
        }
    }

    /**
     * XML に定義された置換エントリを元にソース置換処理を行います。
     * @param className クラス名
     * @param source Java ソース文字列
     * @return 処理後のソース文字列
     * @throws MergeDocException コンフィグ情報の取得に失敗した場合
     * @throws SAXException SAX パース例外が発生した場合
     * @throws IOException 入出力例外が発生した場合
     */
    private String doFilter(String className, String source)
        throws MergeDocException, SAXException, IOException
    {
        // クラス別置換定義の処理
        String path = FastStringUtils.replaceAll(className, "\\.", "/") + ".xml";
        ConfigManager config = ConfigManager.getInstance();
        File entryXML = config.getFile(path);
        if (entryXML.exists()) {
            SAXParser saxParser = config.getSAXPerser();
            ReplaceHandler handler = new ReplaceHandler(source);
            saxParser.parse(entryXML, handler);
            source = handler.getResult();
        }

        // グローバル置換定義の処理
        for (ReplaceEntry entry : pref.getGlobalEntries()) {
            source = entry.replace(source);
        }

        return source;
    }

    /**
     * 処理対象となるエントリ数を取得します。
     * @return 処理対象となるエントリ数
     * @throws ExecutionException エントリー数取得に失敗した場合
     * @throws InterruptedException 現在のスレッドで割り込みが発生した場合
     */
    public int entrySize() throws InterruptedException, ExecutionException {
        return entrySizeFuture.get();
    }

    /**
     * 進捗監視用のリスナをセットします。
     * @param changeListener 進捗監視用のリスナ
     */
    public void setChangeListener(ChangeListener changeListener) {
        workingState.setChangeListener(changeListener);
    }

    /**
     * 処理状態を取得します。
     * @return 処理状態
     */
    public WorkingState getWorkingState() {
        return workingState;
    }
}
