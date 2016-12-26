/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import mergedoc.MergeDocException;
import mergedoc.core.FastStringUtils;
import mergedoc.core.Preference;
import mergedoc.xml.ConfigManager;
import mergedoc.xml.Persister;
import mergedoc.xml.ReplaceEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 設定パネルです。
 * @author Shinji Kashihara
 */
public class PreferencePanel extends JPanel {

	/** ロガー */
	private static final Log log = LogFactory.getLog(PreferencePanel.class);

	
	/** 基本設定パネル：API ドキュメントディレクトリ ファイル選択フィールド */
    private FileChooserField docField = new FileChooserField();

    /** 基本設定パネル：入力ソースアーカイブファイル ファイル選択フィールド */
    private FileChooserField srcField = new FileChooserField();
    
    /** 基本設定パネル：出力ソースアーカイブファイル ファイル選択フィールド */
    private FileChooserField outField = new FileChooserField();


    /** 詳細設定パネル：スプリットペイン */
    private JSplitPane splitPane = new JSplitPane();

    /** 詳細設定パネル：置換エントリのチェックリスト */
    private List<EntryCheckBox> entryCheckList = new LinkedList<EntryCheckBox>();

    /** 詳細設定パネル：プレビュー スクロールペイン */
    private PreviewScrollPane previewScrollPane;


    /**
     * コンストラクタです。 
     * @throws MergeDocException 設定ファイルが取得できない場合
     */
    public PreferencePanel() throws MergeDocException {

        // レイアウト設定
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setMaximumSize(ComponentFactory.createMaxDimension());

        // 上下パネルをこのコンテナに追加
        add(createUpperPanel());
        add(ComponentFactory.createSpacer(0, 7));
        add(createLowerPanel());
    }

    /**
     * 上部の基本設定パネルを作成します。
     * @return 上部の基本設定パネル
     * @throws MergeDocException 設定ファイルが取得できない場合
     */
    private JComponent createUpperPanel() throws MergeDocException {

        // ラベル設定
        JLabel docLabel = docField.getLabel();
        JLabel srcLabel = srcField.getLabel();
        JLabel outLabel = outField.getLabel();
        docLabel.setText("API ドキュメントディレクトリ");
        srcLabel.setText("入力ソースアーカイブファイル");
        outLabel.setText("出力ソースアーカイブファイル");
        JLabel[] labels = {docLabel, srcLabel, outLabel};
        ComponentFactory.ensureMaxFontWidth(labels);

        // コンボ設定
        JComboBox docCombo = docField.getComboBox();
        JComboBox srcCombo = srcField.getComboBox();
        JComboBox outCombo = outField.getComboBox();
        docCombo.addItem(FileChooserField.ENCODING_AUTO);
        srcCombo.addItem(FileChooserField.ENCODING_AUTO);
        docCombo.setSelectedItem("EUC-JP");
        srcCombo.setSelectedItem(FileChooserField.ENCODING_DEFAULT);
        outCombo.setSelectedItem(FileChooserField.ENCODING_DEFAULT);
        JComboBox[] combos = {docCombo, srcCombo, outCombo};
        ComponentFactory.ensureMaxFontWidth(combos);
        
        // ファイルチューザの設定
        docField.setSelectionMode(FileChooserField.DIRECTORIES);
        srcField.setSelectionMode(FileChooserField.ZIP_TGZ_FILES);
        outField.setSelectionMode(FileChooserField.ZIP_FILES);
        docField.setChooseListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resolveArchivePath(docField.getFile());
            }
        });
        
        // 上部パネル作成
        JPanel panel = new TitledPanel("基本設定");
        panel.add(docField);
        panel.add(srcField);
        panel.add(outField);

        // 起動オプションのターゲットディレクトリ取得
        final String OPTION_KEY = "target.directory";
        String targetStr = System.getProperty(OPTION_KEY);
        if (targetStr != null) {
            File targetDir = new File(targetStr);
            if (!targetDir.exists() || targetDir.isFile()) {
                throw new MergeDocException(
                    "オプション " + OPTION_KEY + " に指定された値 " + targetStr +
                    " は\n存在しないかディレクトリではありません。");
            }
            File docDir = searchDocDirectory(targetDir);
            if (docDir != null) {
                docField.setFile(docDir);
            }
            srcField.setFile(new File(""));
            outField.setFile(new File(""));
            resolveArchivePath(targetDir);
        }

        // 設定ファイル読み込み
        loadPersister(docField, Persister.DOC_DIR, Persister.DOC_ENC);
        loadPersister(srcField, Persister.IN_FILE, Persister.IN_ENC);
        loadPersister(outField, Persister.OUT_FILE, Persister.OUT_ENC);

        // 未設定時のデフォルト設定
        String docPath = docField.getFile().getPath();
        if (docPath.equals("")) {
            
            File home = new File(System.getProperty("java.home"));
            if (home.getName().equals("jre") ) {
                home = home.getParentFile();
            }
            File docDir = new File(home, "docs/ja/api");
            if (docDir.exists()) {
                docField.setFile(docDir);
                resolveArchivePath(home);
            }
        }
        
        return panel;
    }

    /**
     * 下部の詳細設定パネルを作成します。
     * @return 下部の詳細設定パネル
     * @throws MergeDocException 設定ファイルが取得できない場合
     */
    private JComponent createLowerPanel() throws MergeDocException {
        
        // チェックボックス配置ペインを作成
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        List<ReplaceEntry> list = ConfigManager.getInstance().getGlobalEntries();
        for (ReplaceEntry entry : list) {
            EntryCheckBox cb = new EntryCheckBox(entry);
            cb.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    previewScrollPane.updatePreview(entryCheckList);
                }
            });
            checkPanel.add(cb);
            entryCheckList.add(cb);
        }
        JScrollPane checkScrollPane = ComponentFactory.createScrollPane(checkPanel);
        checkScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        
        // プレビューペインを作成
        previewScrollPane = new PreviewScrollPane();
        
        // スプリットペインに配置
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setTopComponent(checkScrollPane);
        splitPane.setBottomComponent(previewScrollPane);
        JPanel splitPanel = new JPanel();
        splitPanel.setLayout(new BoxLayout(splitPanel, BoxLayout.X_AXIS));
        splitPanel.add(splitPane);

        // 下部パネル作成
        JPanel panel = new TitledPanel("詳細設定");
        panel.setMaximumSize(ComponentFactory.createMaxDimension());
        panel.add(splitPanel);

        // 設定ファイルからスプリットペイン分割位置を取得
        Persister psst = Persister.getInstance();
        int loc = psst.getInt(Persister.DETAIL_PANEL_HEIGHT, 88);
        splitPane.setDividerLocation(loc);

        // 設定ファイルからチェック有無を取得
        String[] pDescs = psst.getStrings(Persister.REPLACE_DESCRIPTION_ARRAY);
        for (EntryCheckBox ecb : entryCheckList) {
            ReplaceEntry entry = ecb.getReplaceEntry();
            String desc = entry.getDescription();
            for (String pDesc : pDescs) {
                if (desc.equals(pDesc)) {
                    ecb.setSelected(true);
                    break;
                }
            }
        }
        previewScrollPane.updatePreview(entryCheckList);

        return panel;
    }

    /**
     * 指定したベースディレクトリを遡り、予想される入出力ソースアーカイブファイルを
     * 設定します。入出力ソースアーカイブファイルのどちらか 1 つでも値が設定
     * されている場合は何も行いません。
     * @param baseDir ベースディレクトリ
     */
    private void resolveArchivePath(File baseDir) {

        String src = srcField.getFile().getPath();
        String out = outField.getFile().getPath();
        if (!src.equals("") || !out.equals("")) {
            return;
        }
        for (File dir = baseDir; dir != null; dir = dir.getParentFile()) {
            File zip = new File(dir, "src.zip");
            if (zip.exists()) {
                srcField.setFile(zip);
                break;
            }
            File jar = new File(dir, "src.jar");
            if (jar.exists()) {
                srcField.setFile(jar);
                break;
            }
        }
        src = srcField.getFile().getPath();
        if (!src.equals("")) {
            String outName = FastStringUtils.replaceFirst(src, "\\.(zip|jar)$", "ja.zip");
            outField.setFile(new File(outName));
        }
    }

    /**
     * 指定したベースディレクトリを下り、API ドキュメントディレクトリを探します。
     * @param baseDir ベースディレクトリ
     * @return API ドキュメントディレクトリ
     */
    private File searchDocDirectory(File baseDir) {
        
        File dir = null;
        
        for (File file : baseDir.listFiles()) {
            if (file.isFile()) {
                if (file.getName().equals("allclasses-frame.html")) {
                    dir = baseDir;
                }
            } else {
                dir = searchDocDirectory(file); //再帰
            }
            if (dir != null) {
                break;
            }
        }
        return dir;
    }
    
    /**
     * ファイル選択フィールドに設定ファイルから取得した値をセットします。
     * @param field ファイル選択フィールド
     * @param pathKey パスを示す設定ファイルのキー
     * @param charKey 文字セットを示す設定ファイルのキー
     * @throws MergeDocException 設定ファイルが取得できない場合
     */
    private void loadPersister(FileChooserField field, Persister.Key pathKey,
        Persister.Key charKey) throws MergeDocException
    {
        Persister psst = Persister.getInstance();
        if (field.getFile().getPath().equals("")) {
            String path = psst.getString(pathKey, "");
            if (path.length() > 0) {
                field.setFile(new File(path));
            }
        }
        try {
            String enc = psst.getString(charKey);
            field.getComboBox().setSelectedItem(enc);
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * 選択された置換エントリの配列を取得します。
     * @return 選択された置換エントリの配列
     */
    public ReplaceEntry[] getSelectedEntries() {
        
        List<ReplaceEntry> enables = new LinkedList<ReplaceEntry>();
        for (EntryCheckBox cb : entryCheckList) {
            if (cb.isSelected()) {
                ReplaceEntry entry = cb.getReplaceEntry();
                enables.add(entry);
            }
        }
        ReplaceEntry[] entries = enables.toArray(new ReplaceEntry[enables.size()]);
        return entries;
    }

    /**
     * マージ設定を取得します。
     * @return マージ設定
     */
    public Preference getPreference() {

        return new Preference() {

            File docDir  = docField.getFile();
            File srcFile = srcField.getFile();
            File outFile = outField.getFile();
            String docEnc = docField.getComboBox().getSelectedItem().toString();
            String srcEnc = srcField.getComboBox().getSelectedItem().toString();
            String outEnc = outField.getComboBox().getSelectedItem().toString();
            ReplaceEntry[] entries = getSelectedEntries();

            public File getDocDirectory()  {return docDir;}
            public File getInputArchive()  {return srcFile;}
            public File getOutputArchive() {return outFile;}
            public String getDocEncoding()    {return docEnc;}
            public String getInputEncoding()  {return srcEnc;}
            public String getOutputEncoding() {return outEnc;}
            public ReplaceEntry[] getGlobalEntries() {return entries;}
        };
    }
    
    /**
     * このパネルの設定内容を Persister にセットします。
     * @throws Persister にセット出来なかった場合
     */
    public void persistent() throws MergeDocException {

        Preference pref = getPreference();
        Persister psst = Persister.getInstance();

        psst.setString(Persister.DOC_DIR, pref.getDocDirectory().getPath());
        psst.setString(Persister.IN_FILE, pref.getInputArchive().getPath());
        psst.setString(Persister.OUT_FILE, pref.getOutputArchive().getPath());
        psst.setString(Persister.DOC_ENC, pref.getDocEncoding());
        psst.setString(Persister.IN_ENC, pref.getInputEncoding());
        psst.setString(Persister.OUT_ENC, pref.getOutputEncoding());
        psst.setInt(Persister.DETAIL_PANEL_HEIGHT, splitPane.getDividerLocation());

        List<String> descList = new LinkedList<String>();
        for (ReplaceEntry entry : pref.getGlobalEntries()) {
            descList.add(entry.getDescription());
        }
        String[] descs = descList.toArray(new String[descList.size()]);
        psst.setStrings(Persister.REPLACE_DESCRIPTION_ARRAY, descs);
    }
}
